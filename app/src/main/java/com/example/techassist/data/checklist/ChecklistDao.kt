package com.example.techassist.data.checklist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Insert suspend fun insertChecklist(checklist: ChecklistEntity)
    @Insert suspend fun insertItems(items: List<ChecklistItemEntity>)

    @Query("SELECT * FROM checklists ORDER BY createdAtEpochMillis DESC")
    fun observeChecklists(): Flow<List<ChecklistEntity>>

    @Query(
        "SELECT checklistId, COUNT(*) AS totalCount, SUM(isDone) AS doneCount " +
                "FROM checklist_items GROUP BY checklistId"
    )
    fun observeProgress(): Flow<List<ChecklistProgress>>

    @Query("SELECT * FROM checklists WHERE id = :id")
    fun observeChecklist(id: String): Flow<ChecklistEntity?>

    @Query("SELECT * FROM checklist_items WHERE checklistId = :id ORDER BY position ASC")
    fun observeItems(id: String): Flow<List<ChecklistItemEntity>>

    @Query("UPDATE checklist_items SET isDone = :done WHERE id = :itemId")
    suspend fun setDone(itemId: String, done: Boolean)

    @Query("UPDATE checklist_items SET isDone = 0 WHERE checklistId = :id")
    suspend fun resetItems(id: String)

    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("DELETE FROM checklist_items WHERE checklistId = :id")
    suspend fun deleteItemsOf(id: String)

    @Query("DELETE FROM checklists WHERE id = :id")
    suspend fun deleteChecklist(id: String)
}