package com.example.techassist.data.rag

object FocusedContext {
    /** Consecutive lines around [centre], growing alternately forwards and backwards while they fit in [maxChars]. */
    fun window(lines: List<String>, centre: Int, maxChars: Int): String {
        if (lines.isEmpty()) return ""
        val c = centre.coerceIn(0, lines.lastIndex)
        if (lines[c].length >= maxChars) return lines[c].take(maxChars)

        var start = c
        var end = c
        var length = lines[c].length
        var forward = true
        while (true) {
            val canForward = end < lines.lastIndex && length + 1 + lines[end + 1].length <= maxChars
            val canBackward = start > 0 && length + 1 + lines[start - 1].length <= maxChars
            if (!canForward && !canBackward) break
            if ((forward && canForward) || !canBackward) {
                end++
                length += 1 + lines[end].length
            } else {
                start--
                length += 1 + lines[start].length
            }
            forward = !forward
        }
        return lines.subList(start, end + 1).joinToString("\n")
    }
}