package com.example.techassist.ui.checklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.techassist.data.DocumentRepository
import com.example.techassist.data.checklist.ChecklistRepository
import com.example.techassist.data.checklist.ChecklistRow
import com.example.techassist.data.local.DocumentStatus
import com.example.techassist.data.rag.ChecklistDraftBuilder
import com.example.techassist.data.rag.RagPromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChecklistListViewModel(application: Application) : AndroidViewModel(application) {
    private val checklists = ChecklistRepository(application)
    private val documents = DocumentRepository(application)

    val rows: StateFlow<List<ChecklistRow>> = checklists.observeRows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _created = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val created: SharedFlow<String> = _created.asSharedFlow()

    fun create(topic: String) {
        val t = topic.trim()
        if (t.isEmpty() || _isWorking.value) return
        viewModelScope.launch {
            _isWorking.value = true
            _message.value = null
            try {
                val anyReady = documents.observeDocuments().first().any { it.status == DocumentStatus.READY }
                if (!anyReady) {
                    _message.value = "No documents are ready yet. Import a PDF in the Document Library first."
                    return@launch
                }
                val hits = documents.searchChunks(t, topK = TOP_K, minSimilarity = MIN_SIMILARITY)
                if (hits.isEmpty()) {
                    _message.value = RagPromptBuilder.NOT_ENOUGH_INFO
                    return@launch
                }
                val draft = ChecklistDraftBuilder.build(hits)
                if (draft.none { !it.isSafety }) {
                    _message.value = "The matching pages contain no step-by-step lists. " +
                            "Try rewording, for example \"commissioning procedure for <equipment>\"."
                    return@launch
                }
                val id = checklists.create(
                    title = t.replaceFirstChar { it.uppercase() },
                    topic = t,
                    draft = draft,
                )
                _created.emit(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _message.value = "Something went wrong: ${e.message}"
            } finally {
                _isWorking.value = false
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { checklists.deleteChecklist(id) }
    }

    private companion object {
        const val TOP_K = 6              // more pages than chat: a procedure spans several
        const val MIN_SIMILARITY = com.example.techassist.data.rag.RagConfig.MIN_SIMILARITY
    }
}