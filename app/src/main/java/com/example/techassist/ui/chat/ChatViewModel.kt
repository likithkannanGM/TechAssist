package com.example.techassist.ui.chat

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.techassist.TechAssistApp
import com.example.techassist.data.DocumentRepository
import com.example.techassist.data.embedding.EmbeddingEngineHolder
import com.example.techassist.data.llm.ModelStore
import com.example.techassist.data.local.DocumentStatus
import com.example.techassist.data.rag.PassageSelector
import com.example.techassist.data.rag.RagConfig
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
    val quote: String? = null,          // closest passage from the manual
    val quoteSource: String? = null,
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

    fun ask(question: String, quickOnly: Boolean = false) {
        val q = question.trim()
        if (q.isEmpty() || _isBusy.value) return
        job = viewModelScope.launch {
            _isBusy.value = true
            addMessage(ChatMessage(nextId++, Role.USER, q))
            try {
                answer(q, quickOnly)
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

    private suspend fun answer(question: String, quickOnly: Boolean) {
        val anyReady = repository.observeDocuments().first().any { it.status == DocumentStatus.READY }
        if (!anyReady) {
            addAssistant("No documents are ready yet. Import a PDF in the Document Library first.")
            return
        }

        _status.value = "Searching documents…"
        val hits = repository.searchChunks(question, topK = TOP_K, minSimilarity = RagConfig.MIN_SIMILARITY)
        if (hits.isEmpty()) {
            // Weak evidence: refuse without calling the model, so it cannot make something up.
            addAssistant(RagPromptBuilder.NOT_ENOUGH_INFO)
            return
        }

        // Show the closest passage straight away; the language model, if used, can take a while.
        val started = SystemClock.elapsedRealtime()
        val embedder = EmbeddingEngineHolder.get(getApplication<Application>())
        val passage = PassageSelector.select(question, hits, RagConfig.DENSE_WEIGHT) { embedder.embed(it) }
        Log.i(TAG, "Closest passage took ${SystemClock.elapsedRealtime() - started} ms")

        val id = nextId++
        addMessage(
            ChatMessage(
                id = id,
                role = Role.ASSISTANT,
                text = "",
                sources = RagPromptBuilder.sourceLabels(hits),
                quote = passage?.text,
                quoteSource = passage?.source,
                isStreaming = !quickOnly,
            )
        )
        if (quickOnly) return

        if (!engine.isLoaded) {
            if (!store.exists()) {
                setText(id, "No language model is installed, so only the closest passage is shown. " +
                        "Import a .gguf in \"Test local LLM (debug)\" for a written answer.")
                return
            }
            _status.value = "Loading model…"
            engine.load(store.modelFile().absolutePath)
        }

        val prompt = RagPromptBuilder.build(question, hits)
        _status.value = "Generating…"
        engine.generate(prompt.system, prompt.user).collect { piece ->
            _messages.update { list -> list.map { if (it.id == id) it.copy(text = it.text + piece) else it } }
        }
    }

    private fun setText(id: Long, text: String) =
        _messages.update { list -> list.map { if (it.id == id) it.copy(text = text) else it } }

    private fun addAssistant(text: String) = addMessage(ChatMessage(nextId++, Role.ASSISTANT, text))

    private fun addMessage(message: ChatMessage) = _messages.update { it + message }

    private companion object {
        const val TAG = "TechAssist"
        const val TOP_K = 3
    }
}