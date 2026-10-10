package com.example.techassist.ui.retrieval

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.techassist.data.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class RetrievalUiState {
    object Idle : RetrievalUiState()
    object Searching : RetrievalUiState()
    object NoDocumentsIndexed : RetrievalUiState()
    data class Results(val hits: List<DocumentRepository.RetrievedChunk>) : RetrievalUiState()
    object NoRelevantMatch : RetrievalUiState()
}

class RetrievalDebugViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DocumentRepository(application)

    private val _state = MutableStateFlow<RetrievalUiState>(RetrievalUiState.Idle)
    val state: StateFlow<RetrievalUiState> = _state

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.value = RetrievalUiState.Searching

            val anyDocumentsReady = repository.observeDocuments().first()
                .any { it.status.name == "READY" }
            if (!anyDocumentsReady) {
                _state.value = RetrievalUiState.NoDocumentsIndexed
                return@launch
            }

            val hits = repository.searchChunks(query, topK = 5, minSimilarity = 0.35f)
            _state.value = if (hits.isEmpty()) RetrievalUiState.NoRelevantMatch
            else RetrievalUiState.Results(hits)
        }
    }
}