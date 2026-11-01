package com.example.techassist.data.embedding

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.LongBuffer
import kotlin.math.sqrt

/**
 * Loads the bundled MiniLM ONNX model once, then generates a 384-dim, L2-normalized
 * sentence embedding for arbitrary text. All work runs on Dispatchers.Default.
 */
class EmbeddingEngine private constructor(
    private val session: OrtSession,
    private val tokenizer: BertTokenizer,
) {
    // Becomes false if the model turns out to need fixed-length input.
    @Volatile private var trimInputs = true

    companion object {
        const val MAX_SEQ_LEN = 256   // chunks are kept short enough (PassageChunker) to fit
        const val EMBEDDING_DIM = 384
        private const val INTRA_OP_THREADS = 4   // untested on this phone; try 2, 4 and 6 and compare query times
        private const val TAG = "EmbeddingEngine"

        suspend fun load(context: Context): EmbeddingEngine = withContext(Dispatchers.Default) {
            val env = OrtEnvironment.getEnvironment()

            val modelBytes = context.assets.open("embedding/model.onnx").use { it.readBytes() }
            val options = OrtSession.SessionOptions().apply { setIntraOpNumThreads(INTRA_OP_THREADS) }
            val session = env.createSession(modelBytes, options)
            val vocab = VocabLoader.loadFromAssets(context)
            EmbeddingEngine(session, BertTokenizer(vocab))
        }
    }

    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        val encoding = tokenizer.encode(text, MAX_SEQ_LEN)
        if (trimInputs) {
            val length = (encoding.attentionMask.indexOfLast { it == 1L } + 1).coerceIn(1, MAX_SEQ_LEN)
            try {
                return@withContext infer(encoding.inputIds, encoding.attentionMask, encoding.tokenTypeIds, length)
            } catch (e: OrtException) {
                trimInputs = false
                Log.w(TAG, "Model rejected shorter inputs, using fixed length: ${e.message}")
            }
        }
        infer(encoding.inputIds, encoding.attentionMask, encoding.tokenTypeIds, MAX_SEQ_LEN)
    }

    private fun infer(inputIds: LongArray, attentionMask: LongArray, tokenTypeIds: LongArray, length: Int): FloatArray {
        val env = OrtEnvironment.getEnvironment()
        val shape = longArrayOf(1, length.toLong())
        val ids = inputIds.copyOf(length)
        val mask = attentionMask.copyOf(length)
        val types = tokenTypeIds.copyOf(length)

        return OnnxTensor.createTensor(env, LongBuffer.wrap(ids), shape).use { idsTensor ->
            OnnxTensor.createTensor(env, LongBuffer.wrap(mask), shape).use { maskTensor ->
                OnnxTensor.createTensor(env, LongBuffer.wrap(types), shape).use { typesTensor ->

                    val inputs = mapOf(
                        "input_ids" to idsTensor,
                        "attention_mask" to maskTensor,
                        "token_type_ids" to typesTensor,
                    )
                    session.run(inputs).use { result ->
                        meanPoolAndNormalize(extractHiddenStates(result), mask)
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractHiddenStates(result: OrtSession.Result): Array<FloatArray> {
        val preferred = runCatching { result.get("last_hidden_state").orElse(null) }.getOrNull()
        val tensorValue = preferred?.value ?: result.first().value
        val batched = tensorValue as Array<Array<FloatArray>>
        return batched[0]
    }

    private fun meanPoolAndNormalize(hiddenStates: Array<FloatArray>, attentionMask: LongArray): FloatArray {
        val pooled = FloatArray(EMBEDDING_DIM)
        var validTokenCount = 0
        for (i in hiddenStates.indices) {
            if (attentionMask[i] == 1L) {
                validTokenCount++
                for (d in 0 until EMBEDDING_DIM) pooled[d] += hiddenStates[i][d]
            }
        }
        if (validTokenCount > 0) for (d in 0 until EMBEDDING_DIM) pooled[d] /= validTokenCount


        var norm = 0f
        for (v in pooled) norm += v * v
        norm = sqrt(norm).coerceAtLeast(1e-9f)
        for (d in 0 until EMBEDDING_DIM) pooled[d] /= norm
        return pooled
    }

    fun close() = session.close()
}