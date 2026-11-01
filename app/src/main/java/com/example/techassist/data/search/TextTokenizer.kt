package com.example.techassist.data.search

object TextTokenizer {
    private val STOP_WORDS = setOf(
        "a", "an", "the", "of", "is", "are", "was", "were", "be", "to", "in", "on", "at", "for", "and", "or",
        "do", "does", "did", "how", "what", "which", "who", "when", "where", "why", "can", "could", "should",
        "would", "i", "it", "its", "this", "that", "these", "those", "with", "as", "by", "from", "if", "my",
        "me", "there", "has", "have", "had", "much", "many", "you", "your", "we", "our", "into", "than", "then",
    )
    private val SPLIT = Regex("[^\\p{L}\\p{N}]+")

    /** Lowercase terms; punctuation separates terms, so "V10.x" gives "v10". Codes like "74659z4" stay whole. */
    fun tokenize(text: String): List<String> =
        text.lowercase()
            .split(SPLIT)
            .filter { it.length >= 2 && it !in STOP_WORDS }
            .map { stem(it) }

    /** Only strips a plural "s" from longer words, so "zbus" and "class" stay as they are. */
    internal fun stem(token: String): String =
        if (token.length > 4 && token.endsWith("s") && !token.endsWith("ss")) token.dropLast(1) else token
}