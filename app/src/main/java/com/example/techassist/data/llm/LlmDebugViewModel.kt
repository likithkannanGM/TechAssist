package com.example.techassist.ui.llm

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.techassist.TechAssistApp
import com.example.techassist.data.llm.ModelStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LlmDebugState(
    val modelPresent: Boolean = false,
    val modelSizeMb: Long = 0,
    val isCopying: Boolean = false,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val isGenerating: Boolean = false,
    val output: String = "",
    val metrics: String? = null,
    val error: String? = null,
)

class LlmDebugViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ModelStore(application)
    private val engine = (application as TechAssistApp).llmEngine

    private val _state = MutableStateFlow(
        LlmDebugState(
            modelPresent = store.exists(),
            modelSizeMb = store.sizeMb(),
            isLoaded = engine.isLoaded,
        )
    )
    val state: StateFlow<LlmDebugState> = _state.asStateFlow()

    private var generationJob: Job? = null

    fun onModelPicked(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isCopying = true, error = null) }
            engine.close() // a new file replaces whatever was loaded
            store.importFrom(uri)
                .onSuccess {
                    _state.update {
                        it.copy(isCopying = false, modelPresent = true, modelSizeMb = store.sizeMb(), isLoaded = false)
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isCopying = false, modelPresent = store.exists(), isLoaded = false, error = e.message)
                    }
                }
        }
    }

    fun loadModel() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, metrics = null) }
            val start = SystemClock.elapsedRealtime()
            // runCatching also catches Errors such as UnsatisfiedLinkError, which is what we want here
            runCatching { engine.load(store.modelFile().absolutePath) }
                .onSuccess {
                    val ms = SystemClock.elapsedRealtime() - start
                    _state.update { it.copy(isLoading = false, isLoaded = true, metrics = "Model loaded in $ms ms") }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, isLoaded = false, error = "Could not load model: ${e.message}") }
                }
        }
    }

    fun generate(prompt: String) {
        if (prompt.isBlank() || generationJob?.isActive == true) return
        generationJob = viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, output = "", metrics = null, error = null) }
            val start = SystemClock.elapsedRealtime()
            var firstPieceAt = 0L
            var pieces = 0
            try {
                engine.generate(SYSTEM_PROMPT, prompt).collect { piece ->
                    if (pieces == 0) firstPieceAt = SystemClock.elapsedRealtime()
                    pieces++
                    _state.update { it.copy(output = it.output + piece) }
                }
                val end = SystemClock.elapsedRealtime()
                val seconds = (end - firstPieceAt) / 1000f
                val speed = if (pieces > 1 && seconds > 0f) "%.1f".format((pieces - 1) / seconds) else "n/a"
                _state.update {
                    it.copy(metrics = "First piece after ${firstPieceAt - start} ms · ~$speed pieces/s · $pieces pieces total")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(error = "Generation failed: ${e.message}") }
            } finally {
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun stop() { generationJob?.cancel() }

    private companion object {
        const val SYSTEM_PROMPT = "You are a concise technical assistant."
    }
}