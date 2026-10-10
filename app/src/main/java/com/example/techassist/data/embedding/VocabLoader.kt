package com.example.techassist.data.embedding

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object VocabLoader {
    fun loadFromAssets(context: Context, assetPath: String = "embedding/vocab.txt"): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        context.assets.open(assetPath).use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).useLines { lines ->
                lines.forEachIndexed { index, line -> map[line] = index }
            }
        }
        return map
    }
}