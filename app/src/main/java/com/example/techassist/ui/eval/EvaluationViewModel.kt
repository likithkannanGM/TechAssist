package com.example.techassist.ui.eval

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.techassist.TechAssistApp
import com.example.techassist.data.DocumentRepository
import com.example.techassist.data.DocumentRepository.RetrievedChunk
import com.example.techassist.data.eval.CaseResult
import com.example.techassist.data.eval.EvalCase
import com.example.techassist.data.eval.EvalParser
import com.example.techassist.data.eval.EvalReport
import com.example.techassist.data.eval.EvalScorer
import com.example.techassist.data.eval.ScoredHit
import com.example.techassist.data.llm.ModelStore
import com.example.techassist.data.local.DocumentStatus
import com.example.techassist.data.rag.RagPromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class EvalUiState(

    val cases: List<EvalCase> = emptyList(),
    val errors: List<String> = emptyList(),
    val isRunning: Boolean = false,
    val progress: String? = null,
    val report: EvalReport? = null,
    val timing: String? = null,
    val message: String? = null,
)

class EvaluationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DocumentRepository(application)
    private val store = ModelStore(application)
    private val engine = (application as TechAssistApp).llmEngine

    private val _state = MutableStateFlow(EvalUiState())
    val state: StateFlow<EvalUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun onFilePicked(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver
                        .openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text == null) {
                _state.update { it.copy(message = "Could not read that file.") }
                return@launch
            }
            val parsed = EvalParser.parse(text)

            _state.update {
                it.copy(
                    cases = parsed.cases,
                    errors = parsed.errors,
                    report = null,
                    timing = null,
                    message = if (parsed.cases.isEmpty()) "No valid test cases found in that file." else null,
                )
            }
        }
    }

    fun run(includeTiming: Boolean) {
        val cases = _state.value.cases
        if (cases.isEmpty() || _state.value.isRunning) return
        job = viewModelScope.launch {
            _state.update { it.copy(isRunning = true, report = null, timing = null, message = null, progress = "Starting…") }
            try {
                val anyReady = repository.observeDocuments().first().any { it.status == DocumentStatus.READY }
                if (!anyReady) {
                    _state.update { it.copy(message = "No documents are ready. Import the PDFs your test file refers to first.") }
                    return@launch
                }

                // Warm-up so the one-time embedding-model load does not distort the timings.
                _state.update { it.copy(progress = "Warming up the embedding model…") }
                repository.searchChunks("warm up", topK = 1, minSimilarity = 0f)

                val results = mutableListOf<CaseResult>()
                val latencies = mutableListOf<Long>()
                val timingSamples = mutableListOf<Pair<EvalCase, List<RetrievedChunk>>>()


                cases.forEachIndexed { i, case ->
                    _state.update { it.copy(progress = "Question ${i + 1} of ${cases.size}") }
                    val start = SystemClock.elapsedRealtime()
                    // minSimilarity = 0 returns raw scores, so every threshold can be evaluated offline.
                    val hits = repository.searchChunks(case.question, topK = EVAL_TOP_K, minSimilarity = 0f)
                    latencies += SystemClock.elapsedRealtime() - start
                    results += CaseResult(
                        case,
                        hits.map { ScoredHit(it.documentName, it.pageNumber, it.score, it.dense, it.lexical) },
                    )
                    if (case.answerable && timingSamples.size < TIMING_SAMPLES) timingSamples += case to hits
                }

                _state.update { it.copy(report = EvalScorer.score(results, latencies)) }
                if (includeTiming) measureGeneration(timingSamples)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(message = "Evaluation failed: ${e.message}") }
            } finally {
                _state.update { it.copy(isRunning = false, progress = null) }
            }
        }
    }

    fun stop() { job?.cancel() }

    private suspend fun measureGeneration(samples: List<Pair<EvalCase, List<RetrievedChunk>>>) {
        if (!store.exists()) {
            _state.update { it.copy(timing = "Answer timing skipped: no language model installed.") }
            return
        }
        val usable = samples
            .map { (case, hits) -> case to hits.filter { it.score >= EvalScorer.APP_THRESHOLD }.take(EvalScorer.APP_TOP_K) }
            .filter { it.second.isNotEmpty() }

        if (usable.isEmpty()) {
            _state.update { it.copy(timing = "Answer timing skipped: no question had chunks above the threshold.") }
            return
        }
        if (!engine.isLoaded) {
            _state.update { it.copy(progress = "Loading model…") }
            engine.load(store.modelFile().absolutePath)
        }

        val lines = mutableListOf<String>()
        usable.forEachIndexed { i, (case, hits) ->
            _state.update { it.copy(progress = "Timing answer ${i + 1} of ${usable.size}") }
            val prompt = RagPromptBuilder.build(case.question, hits)
            val start = SystemClock.elapsedRealtime()
            var first = 0L
            var pieces = 0
            engine.generate(prompt.system, prompt.user).collect {
                if (pieces == 0) first = SystemClock.elapsedRealtime()
                pieces++
            }
            val end = SystemClock.elapsedRealtime()
            val speed = if (pieces > 1 && end > first) (pieces - 1) * 1000f / (end - first) else 0f
            lines += String.format(
                Locale.US, "Q%d: first piece after %d ms, %.1f pieces/s, %d pieces",
                i + 1, first - start, speed, pieces,
            )
        }
        _state.update { it.copy(timing = "Answer timing (real retrieved context)\n" + lines.joinToString("\n")) }
    }

    private companion object {
        const val EVAL_TOP_K = 5

        const val TIMING_SAMPLES = 3
    }
}