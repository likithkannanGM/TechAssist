package com.example.techassist.data.checklist

import android.content.Context
import androidx.room.withTransaction
import com.example.techassist.data.rag.ChecklistDraftBuilder.DraftItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

data class ChecklistRow(val checklist: ChecklistEntity, val total: Int, val done: Int)

class ChecklistRepository(context: Context) {
    private val database = ChecklistDatabase.getInstance(context)
    private val dao = database.checklistDao()

    fun observeRows(): Flow<List<ChecklistRow>> =
        combine(dao.observeChecklists(), dao.observeProgress()) { lists, progress ->
            val byId = progress.associateBy { it.checklistId }
            lists.map { c ->
                ChecklistRow(c, byId[c.id]?.totalCount ?: 0, byId[c.id]?.doneCount ?: 0)
            }
        }

    fun observeChecklist(id: String): Flow<ChecklistEntity?> = dao.observeChecklist(id)
    fun observeItems(id: String): Flow<List<ChecklistItemEntity>> = dao.observeItems(id)

    suspend fun create(title: String, topic: String, draft: List<DraftItem>): String {
        val id = UUID.randomUUID().toString()
        database.withTransaction {
            dao.insertChecklist(ChecklistEntity(id, title, topic, System.currentTimeMillis()))
            dao.insertItems(
                draft.mapIndexed { index, item ->

                    ChecklistItemEntity(
                        id = UUID.randomUUID().toString(),
                        checklistId = id,
                        position = index,
                        text = item.text,
                        source = item.source,
                        isSafety = item.isSafety,
                        isDone = false,
                    )
                }
            )
        }
        return id
    }

    suspend fun setDone(itemId: String, done: Boolean) = dao.setDone(itemId, done)
    suspend fun reset(checklistId: String) = dao.resetItems(checklistId)
    suspend fun deleteItem(itemId: String) = dao.deleteItem(itemId)

    suspend fun deleteChecklist(id: String) {
        database.withTransaction {
            dao.deleteItemsOf(id)
            dao.deleteChecklist(id)
        }
    }
}