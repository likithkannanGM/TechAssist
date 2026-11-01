package com.example.techassist.data.search

import android.content.Context
import com.example.techassist.data.local.AppDatabase
import com.example.techassist.data.local.FloatArrayConverters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/** One shared index for the whole app. Rebuilt lazily after an import or a delete. */
object RetrievalIndexHolder {
    private val mutex = Mutex()
    private val generation = AtomicInteger(0)
    @Volatile private var cached: RetrievalIndex? = null

    fun invalidate() {
        generation.incrementAndGet()
        cached = null
    }

    suspend fun get(context: Context): RetrievalIndex {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: run {
                val startedAt = generation.get()
                val built = build(context.applicationContext)
                // If an import or delete happened while building, do not keep a stale index.
                if (startedAt == generation.get()) cached = built
                built
            }
        }
    }

    private suspend fun build(context: Context): RetrievalIndex = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val documents = db.documentDao().observeAll().first().associateBy { it.id }
        val chunks = db.chunkDao().getAll().associateBy { it.id }
        val items = db.embeddingDao().getAll().mapNotNull { e ->
            // Rows whose chunk or document no longer exists are ignored here.
            val chunk = chunks[e.chunkId] ?: return@mapNotNull null
            val document = documents[e.documentId] ?: return@mapNotNull null
            IndexedChunk(
                id = chunk.id,
                documentId = e.documentId,
                documentName = document.displayName,
                pageNumber = chunk.pageNumber,
                text = chunk.text,
                vector = FloatArrayConverters.toFloatArray(e.vector),
            )
        }
        RetrievalIndex(items)
    }
}