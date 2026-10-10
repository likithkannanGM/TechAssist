package com.example.techassist.data.embedding

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
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
    companion object {
        const val MAX_SEQ_LEN = 256   // revisit against Phase 3's chunk size once real usage data exists
        const val EMBEDDING_DIM = 384

        suspend fun load(context: Context): EmbeddingEngine = withContext(Dispatchers.Default) {
            val env = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open("embedding/model.onnx").use { it.readBytes() }
            val session = env.createSession(modelBytes, OrtSession.SessionOptions())
            val vocab = VocabLoader.loadFromAssets(context)
            EmbeddingEngine(session, BertTokenizer(vocab))
        }
    }

    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        val env = OrtEnvironment.getEnvironment()
        val encoding = tokenizer.encode(text, MAX_SEQ_LEN)

        val shape = longArrayOf(1, MAX_SEQ_LEN.toLong())
        OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.inputIds), shape).use { inputIdsTensor ->
            OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.attentionMask), shape).use { attnMaskTensor ->
                OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.tokenTypeIds), shape).use { tokenTypeTensor ->
                    val inputs = mapOf(
                        "input_ids" to inputIdsTensor,
                        "attention_mask" to attnMaskTensor,
                        "token_type_ids" to tokenTypeTensor,
                    )
                    session.run(inputs).use { result ->
                        val hiddenStates = extractHiddenStates(result)
                        val embedding = meanPoolAndNormalize(
                            hiddenStates,
                            encoding.attentionMask
                        )

                        android.util.Log.i(
                            "EmbeddingEngine",
                            "Embedding generated successfully: ${embedding.size} dimensions"
                        )

                        embedding
                    }
                }
            }
        }
    }


    @Suppress("UNCHECKED_CAST")
    private fun extractHiddenStates(result: OrtSession.Result): Array<FloatArray> {
        android.util.Log.i(
            "EmbeddingEngine",
            "Available ONNX output names: ${result.map { it.key }}"
        )

        val preferred = runCatching {
            result.get("last_hidden_state").orElse(null)
        }.getOrNull()

        val selectedName = if (preferred != null) {
            "last_hidden_state"
        } else {
            result.first().key
        }

        val tensorValue = preferred?.value ?: result.first().value

        android.util.Log.i(
            "EmbeddingEngine",
            "Selected output tensor: $selectedName"
        )

        val batched = tensorValue as Array<Array<FloatArray>>

        android.util.Log.i(
            "EmbeddingEngine",
            "Tensor shape interpreted as [${batched.size}, " +
                    "${batched[0].size}, ${batched[0][0].size}]"
        )

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