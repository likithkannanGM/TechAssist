package com.example.techassist.data.eval

import java.util.Locale
import kotlin.math.roundToInt

data class EvalCase(
    val line: Int,
    val question: String,
    val answerable: Boolean,
    val expectedDocument: String,   // matched as a case-insensitive substring of the file name; blank = any
    val expectedPages: Set<Int>,    // PDF page numbers (1-based), same numbering the app shows
)

data class ScoredHit(
    val documentName: String,
    val pageNumber: Int,
    val score: Float,
    val dense: Float = 0f,
    val lexical: Float = 0f,
)
data class CaseResult(val case: EvalCase, val hits: List<ScoredHit>)

data class Stats(val min: Float, val median: Float, val max: Float)

data class SweepRow(
    val threshold: Float,
    val answeredCorrectly: Int,   // answerable, a correct page was among the kept chunks
    val falseRefusals: Int,       // answerable, nothing passed the threshold
    val wrongAnswers: Int,        // answerable, chunks passed but none was the right page
    val correctRefusals: Int,     // unanswerable, nothing passed the threshold
    val falseAnswers: Int,        // unanswerable, something passed (the app would answer from noise)
    val accuracy: Float,
)

data class CaseLine(val ok: Boolean, val text: String)

data class EvalReport(
    val answerableCount: Int,
    val unanswerableCount: Int,
    val hitAt1: Float,
    val hitAt3: Float,
    val hitAt5: Float,
    val mrr: Float,
    val relevantBestScore: Stats?,
    val irrelevantTopScore: Stats?,
    val retrievalMs: Stats?,
    val sweep: List<SweepRow>,
    val suggested: SweepRow?,
    val caseLines: List<CaseLine>,
) {
    private fun f2(v: Float) = String.format(Locale.US, "%.2f", v)
    private fun f0(v: Float) = String.format(Locale.US, "%.0f", v)
    private fun pct(v: Float) = "${(v * 100).roundToInt()}%"

    fun summaryText(): String = buildString {
        val total = answerableCount + unanswerableCount
        appendLine("Cases: $total ($answerableCount answerable, $unanswerableCount unanswerable)")
        if (total < 30) appendLine("Note: fewer than 30 cases, so treat these numbers as indicative only.")
        if (answerableCount > 0) {
            appendLine("Hit@1 ${pct(hitAt1)} | Hit@3 ${pct(hitAt3)} | Hit@5 ${pct(hitAt5)} | MRR ${f2(mrr)}")
            relevantBestScore?.let {
                appendLine("Best correct-hit score: min ${f2(it.min)}, median ${f2(it.median)}, max ${f2(it.max)}")
            } ?: appendLine("No correct page was retrieved for any answerable question.")
        }
        irrelevantTopScore?.let {
            appendLine("Top score on unanswerable questions: min ${f2(it.min)}, median ${f2(it.median)}, max ${f2(it.max)}")
        }
        retrievalMs?.let { appendLine("Retrieval time per query: median ${f0(it.median)} ms, max ${f0(it.max)} ms") }
        appendLine()
        appendLine("Threshold sweep (top ${EvalScorer.APP_TOP_K} chunks, as in the app)")
        appendLine("thr   ok falseRef wrong refOk falseAns acc")
        sweep.forEach { r ->
            appendLine(
                String.format(
                    Locale.US, "%.2f %3d %8d %5d %5d %8d %3d%%",
                    r.threshold, r.answeredCorrectly, r.falseRefusals, r.wrongAnswers,
                    r.correctRefusals, r.falseAnswers, (r.accuracy * 100).roundToInt(),
                )
            )
        }
        suggested?.let {
            appendLine()
            append("Suggested threshold: ${f2(it.threshold)} (accuracy ${pct(it.accuracy)})")
        }
    }.trimEnd()

    fun casesText(): String =
        caseLines.joinToString("\n") { (if (it.ok) "OK   " else "FAIL ") + it.text }

    fun fullText(): String = summaryText() + "\n\nPer-question results (threshold ${f2(EvalScorer.APP_THRESHOLD)})\n" + casesText()
}