package com.example.techassist.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.techassist.TechAssistApp
import com.example.techassist.data.DocumentRepository
import com.example.techassist.data.llm.ModelStore
import com.example.techassist.data.local.DocumentStatus
import com.example.techassist.data.rag.RagPromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Role { USER, ASSISTANT }

data class ChatMessage(
    val id: Long,
    val role: Role,
    val text: String,
    val sources: List<String> = emptyList(),
    val isStreaming: Boolean = false,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DocumentRepository(application)
    private val store = ModelStore(application)
    private val engine = (application as TechAssistApp).llmEngine

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private var job: Job? = null
    private var nextId = 0L

    fun ask(question: String) {
        val q = question.trim()
        if (q.isEmpty() || _isBusy.value) return
        job = viewModelScope.launch {
            _isBusy.value = true
            addMessage(ChatMessage(nextId++, Role.USER, q))
            try {
                answer(q)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) { // Throwable on purpose: native load failures are Errors
                addMessage(ChatMessage(nextId++, Role.ASSISTANT, "Something went wrong: ${e.message}"))
            } finally {
                _messages.update { list -> list.map { it.copy(isStreaming = false) } }
                _status.value = null
                _isBusy.value = false
            }
        }
    }

    fun stop() { job?.cancel() }

    private suspend fun answer(question: String) {
        val anyReady = repository.observeDocuments().first().any { it.status == DocumentStatus.READY }
        if (!anyReady) {
            addAssistant("No documents are ready yet. Import a PDF in the Document Library first.")
            return
        }

        _status.value = "Searching documents…"
        val hits = repository.searchChunks(question, topK = TOP_K, minSimilarity = MIN_SIMILARITY)
        if (hits.isEmpty()) {
            // Weak evidence: refuse without calling the model, so it cannot make something up.
            addAssistant(RagPromptBuilder.NOT_ENOUGH_INFO)
            return
        }

        if (!engine.isLoaded) {
            if (!store.exists()) {
                addAssistant("No language model is installed. Open \"Test local LLM (debug)\" to import a .gguf file.")
                return
            }
            _status.value = "Loading model…"
            engine.load(store.modelFile().absolutePath)
        }

        val prompt = RagPromptBuilder.build(question, hits)
        val id = nextId++
        addMessage(
            ChatMessage(id, Role.ASSISTANT, "", sources = RagPromptBuilder.sourceLabels(hits), isStreaming = true)
        )
        _status.value = "Generating…"
        engine.generate(prompt.system, prompt.user).collect { piece ->
            _messages.update { list -> list.map { if (it.id == id) it.copy(text = it.text + piece) else it } }
        }
    }

    private fun addAssistant(text: String) = addMessage(ChatMessage(nextId++, Role.ASSISTANT, text))

    private fun addMessage(message: ChatMessage) = _messages.update { it + message }

    private companion object {
        const val TOP_K = 3
        const val MIN_SIMILARITY = 0.35f // placeholder; tuned in Phase 10
    }
}