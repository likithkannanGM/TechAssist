package com.example.techassist.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.techassist.data.DocumentRepository
import com.example.techassist.data.local.DocumentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class DocumentLibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DocumentRepository(application)

    val documents: StateFlow<List<DocumentEntity>> = repository.observeDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _progressText = MutableStateFlow<String?>(null)
    val progressText: StateFlow<String?> = _progressText

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun onDocumentPicked(uri: Uri, displayName: String, sizeBytes: Long) {
        viewModelScope.launch {
            if (repository.isDuplicateName(displayName)) {
                _errorMessage.value = "\"$displayName\" is already imported."
                return@launch
            }
            _isImporting.value = true
            repository.importDocument(uri, displayName, sizeBytes).collect { progress ->
                _progressText.value = when (progress) {
                    is com.example.techassist.data.ImportProgress.CopyingFile -> "Reading the PDF…"
                    is com.example.techassist.data.ImportProgress.ExtractingText -> "Extracting text…"
                    is com.example.techassist.data.ImportProgress.GeneratingEmbeddings ->
                        "Generating embeddings (${progress.chunk}/${progress.totalChunks})…"
                    is com.example.techassist.data.ImportProgress.Done -> null
                    is com.example.techassist.data.ImportProgress.Failed -> progress.reason
                }
            }
            _isImporting.value = false
            _progressText.value = null
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch { repository.deleteDocument(document) }
    }

    fun dismissError() { _errorMessage.value = null }
}