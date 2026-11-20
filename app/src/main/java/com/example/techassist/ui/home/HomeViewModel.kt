package com.example.techassist.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.techassist.data.llm.ModelStore
import com.example.techassist.data.local.AppDatabase
import com.example.techassist.data.local.DocumentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ModelStore(application)

    val readyDocuments: StateFlow<Int> = AppDatabase.getInstance(application).documentDao().observeAll()
        .map { documents -> documents.count { it.status == DocumentStatus.READY } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _modelText = MutableStateFlow(describeModel())
    val modelText: StateFlow<String> = _modelText.asStateFlow()

    fun refreshModel() { _modelText.value = describeModel() }

    private fun describeModel(): String =
        if (store.exists()) "Installed (${store.sizeMb()} MB)" else "Not installed (import in Test local LLM)"
}