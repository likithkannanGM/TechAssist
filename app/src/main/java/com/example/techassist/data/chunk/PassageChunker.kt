package com.example.techassist.data.chunk

/**
 * Packs the lines of one page into chunks small enough to be embedded in full
 * (the embedding model reads at most 256 tokens), with a little overlap between neighbours.
 */
object PassageChunker {
    const val MAX_CHARS = 600
    const val OVERLAP_CHARS = 100
    private const val MIN_ALNUM_CHARS = 3        // drops stray page numbers
    private const val MAX_DOT_RATIO = 0.3        // drops table-of-contents dot leaders

    // A line plus the carried-over overlap plus a newline always fits in MAX_CHARS.
    private const val LINE_MAX = MAX_CHARS - OVERLAP_CHARS - 1

    data class Chunk(val text: String, val wordCount: Int)

    fun chunkPageText(pageText: String): List<Chunk> {
        val lines = pageText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { splitLongLine(it) }
        if (lines.isEmpty()) return emptyList()

        val groups = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        for (line in lines) {
            if (current.isNotEmpty() && joinedLength(current) + 1 + line.length > MAX_CHARS) {
                groups += current
                current = overlapTail(current).toMutableList()
            }
            current.add(line)
        }
        if (current.isNotEmpty()) groups += current

        return groups
            .map { it.joinToString("\n") }
            .filter { isUsable(it) }
            .map { Chunk(it, wordCount(it)) }
    }

    private fun splitLongLine(line: String): List<String> {
        if (line.length <= LINE_MAX) return listOf(line)
        val parts = mutableListOf<String>()
        var rest = line
        while (rest.length > LINE_MAX) {
            var cut = rest.lastIndexOf(' ', LINE_MAX)
            if (cut <= 0) cut = LINE_MAX          // no space to break on, e.g. a run of dots
            parts += rest.substring(0, cut).trim()
            rest = rest.substring(cut).trim()
        }
        if (rest.isNotEmpty()) parts += rest
        return parts
    }

    /** The last lines of a finished chunk that fit in OVERLAP_CHARS, repeated at the start of the next one. */
    private fun overlapTail(lines: List<String>): List<String> {
        val tail = ArrayDeque<String>()
        var length = 0
        for (line in lines.asReversed()) {
            val added = if (tail.isEmpty()) line.length else line.length + 1
            if (length + added > OVERLAP_CHARS) break
            tail.addFirst(line)
            length += added
        }
        return tail.toList()
    }

    private fun joinedLength(lines: List<String>): Int =
        lines.sumOf { it.length } + (lines.size - 1).coerceAtLeast(0)

    private fun isUsable(text: String): Boolean {
        if (text.count { it.isLetterOrDigit() } < MIN_ALNUM_CHARS) return false
        val dotRatio = text.count { it == '.' }.toDouble() / text.length
        return dotRatio <= MAX_DOT_RATIO
    }

    private fun wordCount(text: String): Int = text.split(Regex("\\s+")).count { it.isNotEmpty() }
}