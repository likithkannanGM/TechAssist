package com.example.techassist.ui.checklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.techassist.data.checklist.ChecklistEntity
import com.example.techassist.data.checklist.ChecklistItemEntity
import com.example.techassist.data.checklist.ChecklistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChecklistDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val repository = ChecklistRepository(application)
    private val checklistId: String = checkNotNull(savedStateHandle.get<String>("id"))

    val checklist: StateFlow<ChecklistEntity?> = repository.observeChecklist(checklistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val items: StateFlow<List<ChecklistItemEntity>> = repository.observeItems(checklistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggle(item: ChecklistItemEntity) {
        viewModelScope.launch { repository.setDone(item.id, !item.isDone) }
    }

    fun deleteItem(item: ChecklistItemEntity) {
        viewModelScope.launch { repository.deleteItem(item.id) }
    }

    fun reset() {
        viewModelScope.launch { repository.reset(checklistId) }
    }
}