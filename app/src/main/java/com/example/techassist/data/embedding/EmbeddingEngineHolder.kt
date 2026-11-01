package com.example.techassist.data.embedding

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One ONNX session for the whole app instead of one per screen. */
object EmbeddingEngineHolder {
    private val mutex = Mutex()
    @Volatile private var engine: EmbeddingEngine? = null

    suspend fun get(context: Context): EmbeddingEngine {
        engine?.let { return it }
        return mutex.withLock {
            engine ?: EmbeddingEngine.load(context.applicationContext).also { engine = it }
        }
    }
}