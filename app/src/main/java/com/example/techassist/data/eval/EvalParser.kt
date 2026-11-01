package com.example.techassist.data.eval

/**
 * Test file format, one case per line, fields separated by "|":
 *   ANSWERABLE   | question | document name | pages
 *   UNANSWERABLE | question
 * Pages: 12   or   12,14   or   12-14   or   12,14-16. Lines starting with # and blank lines are ignored.
 */
object EvalParser {
    data class ParseResult(val cases: List<EvalCase>, val errors: List<String>)

    private const val MAX_RANGE = 200

    fun parse(text: String): ParseResult {
        val cases = mutableListOf<EvalCase>()
        val errors = mutableListOf<String>()

        text.lines().forEachIndexed { index, raw ->
            val lineNo = index + 1
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed

            val parts = line.split("|").map { it.trim() }
            when (parts[0].uppercase()) {
                "ANSWERABLE" -> {
                    if (parts.size < 4 || parts[1].isEmpty()) {
                        errors += "Line $lineNo: expected ANSWERABLE | question | document | pages"
                        return@forEachIndexed
                    }
                    val pages = parsePages(parts[3])
                    if (pages == null) {
                        errors += "Line $lineNo: pages must look like 12, 12,14 or 12-14"

                        return@forEachIndexed
                    }
                    cases += EvalCase(lineNo, parts[1], true, parts[2], pages)
                }
                "UNANSWERABLE" -> {
                    if (parts.size < 2 || parts[1].isEmpty()) {
                        errors += "Line $lineNo: expected UNANSWERABLE | question"
                        return@forEachIndexed
                    }
                    cases += EvalCase(lineNo, parts[1], false, "", emptySet())
                }
                else -> errors += "Line $lineNo: a line must start with ANSWERABLE or UNANSWERABLE"
            }
        }
        return ParseResult(cases, errors)
    }

    internal fun parsePages(spec: String): Set<Int>? {
        val result = mutableSetOf<Int>()
        for (token in spec.split(",").map { it.trim() }) {
            if (token.isEmpty()) return null
            val bounds = token.split("-").map { it.trim() }
            when (bounds.size) {
                1 -> {
                    val page = bounds[0].toIntOrNull()
                    if (page == null || page < 1) return null
                    result += page
                }
                2 -> {
                    val from = bounds[0].toIntOrNull()
                    val to = bounds[1].toIntOrNull()
                    if (from == null || to == null || from < 1 || to < from || to - from > MAX_RANGE) return null

                    result += from..to
                }
                else -> return null
            }
        }
        return result.takeIf { it.isNotEmpty() }
    }
}