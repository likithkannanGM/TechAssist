package com.example.techassist.ui.troubleshoot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.techassist.TechAssistApp
import com.example.techassist.data.DocumentRepository
import com.example.techassist.data.llm.ModelStore
import com.example.techassist.data.local.DocumentStatus
import com.example.techassist.data.rag.RagPromptBuilder
import com.example.techassist.data.rag.SafetyNoteExtractor
import com.example.techassist.data.rag.SafetyNoteExtractor.SafetyNote
import com.example.techassist.data.rag.TroubleshootingPromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TroubleshootPhase { IDLE, WORKING, DONE }

data class Excerpt(val source: String, val text: String)

data class TroubleshootUiState(
    val phase: TroubleshootPhase = TroubleshootPhase.IDLE,
    val status: String? = null,
    val answer: String = "",
    val safetyNotes: List<SafetyNote> = emptyList(),
    val excerpts: List<Excerpt> = emptyList(),
    val message: String? = null,
)

class TroubleshootViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DocumentRepository(application)
    private val store = ModelStore(application)
    private val engine = (application as TechAssistApp).llmEngine

    private val _state = MutableStateFlow(TroubleshootUiState())
    val state: StateFlow<TroubleshootUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun diagnose(fault: String, equipment: String, alreadyChecked: String) {
        val f = fault.trim()
        if (f.isEmpty() || _state.value.phase == TroubleshootPhase.WORKING) return
        job = viewModelScope.launch {
            _state.value = TroubleshootUiState(phase = TroubleshootPhase.WORKING, status = "Searching documents…")
            try {
                runDiagnosis(f, equipment.trim(), alreadyChecked.trim())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) { // Throwable on purpose: native load failures are Errors
                notice("Something went wrong: ${e.message}")
            } finally {
                _state.update { it.copy(phase = TroubleshootPhase.DONE, status = null) }
            }
        }
    }

    fun stop() { job?.cancel() }

    private suspend fun runDiagnosis(fault: String, equipment: String, alreadyChecked: String) {
        val anyReady = repository.observeDocuments().first().any { it.status == DocumentStatus.READY }
        if (!anyReady) {
            notice("No documents are ready yet. Import a PDF in the Document Library first.")
            return
        }

        // Search on equipment + fault only. "Already checked" would pull in pages about things
        // the technician has ruled out, so it goes to the model prompt but not the search.
        val query = listOf(equipment, fault).filter { it.isNotBlank() }.joinToString(". ")
        val hits = repository.searchChunks(query, topK = TOP_K, minSimilarity = MIN_SIMILARITY)
        if (hits.isEmpty()) {
            notice(RagPromptBuilder.NOT_ENOUGH_INFO)
            return
        }

        // Show evidence immediately; the model is slow and these need no model.
        val labels = RagPromptBuilder.sourceLabels(hits)
        _state.update {
            it.copy(
                safetyNotes = SafetyNoteExtractor.extract(hits),
                excerpts = hits.mapIndexed { i, hit -> Excerpt(labels[i], hit.text.trim()) },
                status = "Found ${hits.size} matching pages.",
            )
        }

        if (!engine.isLoaded) {
            if (!store.exists()) {
                notice(
                    "No language model is installed, so only the matching manual text is shown. " +
                            "Import a .gguf in \"Test local LLM (debug)\" for a written diagnosis."
                )
                return
            }
            _state.update { it.copy(status = "Loading model…") }
            engine.load(store.modelFile().absolutePath)
        }

        _state.update { it.copy(status = "Writing diagnosis…") }
        val prompt = TroubleshootingPromptBuilder.build(fault, equipment, alreadyChecked, hits)
        engine.generate(prompt.system, prompt.user).collect { piece ->
            _state.update { it.copy(answer = it.answer + piece) }
        }
    }

    private fun notice(text: String) = _state.update { it.copy(message = text) }

    private companion object {
        const val TOP_K = 3
        const val MIN_SIMILARITY = com.example.techassist.data.rag.RagConfig.MIN_SIMILARITY
    }
}