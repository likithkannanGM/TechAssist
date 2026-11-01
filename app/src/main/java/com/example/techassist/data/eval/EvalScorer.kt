package com.example.techassist.data.eval

import com.example.techassist.data.rag.RagConfig
import java.util.Locale

object EvalScorer {
    const val APP_TOP_K = 3                              // matches TOP_K in ChatViewModel
    const val APP_THRESHOLD = RagConfig.MIN_SIMILARITY

    /** 0.10, 0.15, ... 0.70 */
    val THRESHOLDS: List<Float> = (2..14).map { it * 0.05f }

    private enum class Outcome { CORRECT, WRONG, REFUSED }

    fun score(results: List<CaseResult>, latenciesMs: List<Long>): EvalReport {
        val answerable = results.filter { it.case.answerable }
        val unanswerable = results.filterNot { it.case.answerable }

        val ranks = answerable.map { firstCorrectRank(it) }
        fun hitAt(k: Int): Float =
            if (answerable.isEmpty()) 0f else ranks.count { it != null && it <= k }.toFloat() / answerable.size
        val mrr = if (answerable.isEmpty()) 0f else ranks.sumOf { r -> if (r == null) 0.0 else 1.0 / r }.toFloat() / answerable.size

        val relevantBest = answerable.mapNotNull { r ->
            r.hits.filter { isCorrect(it, r.case) }.maxOfOrNull { it.score }
        }
        val irrelevantTop = unanswerable.map { r -> r.hits.maxOfOrNull { it.score } ?: 0f }

        val sweep = THRESHOLDS.map { t -> sweepRow(t, answerable, unanswerable, results.size) }
        val suggested = if (results.isEmpty()) null
        else sweep.maxWithOrNull(compareBy<SweepRow>({ it.accuracy }, { it.threshold })) // ties go to the higher threshold


        return EvalReport(
            answerableCount = answerable.size,
            unanswerableCount = unanswerable.size,
            hitAt1 = hitAt(1), hitAt3 = hitAt(3), hitAt5 = hitAt(5),
            mrr = mrr,
            relevantBestScore = stats(relevantBest),
            irrelevantTopScore = stats(irrelevantTop),
            retrievalMs = stats(latenciesMs.map { it.toFloat() }),
            sweep = sweep,
            suggested = suggested,
            caseLines = results.map { r ->
                caseLine(r).let { it.copy(text = it.text + " | top: " + topHitsText(r)) }
            },
        )
    }

    internal fun isCorrect(hit: ScoredHit, case: EvalCase): Boolean {
        val docOk = case.expectedDocument.isBlank() || hit.documentName.contains(case.expectedDocument, ignoreCase = true)
        return docOk && hit.pageNumber in case.expectedPages
    }

    private fun sorted(result: CaseResult) = result.hits.sortedByDescending { it.score }

    private fun firstCorrectRank(result: CaseResult): Int? {
        val index = sorted(result).indexOfFirst { isCorrect(it, result.case) }
        return if (index < 0) null else index + 1
    }

    private fun outcomeAt(result: CaseResult, threshold: Float): Outcome {
        val eligible = sorted(result).take(APP_TOP_K).filter { it.score >= threshold }
        if (eligible.isEmpty()) return Outcome.REFUSED
        return if (result.case.answerable && eligible.any { isCorrect(it, result.case) }) Outcome.CORRECT
        else Outcome.WRONG
    }


    private fun sweepRow(t: Float, answerable: List<CaseResult>, unanswerable: List<CaseResult>, total: Int): SweepRow {
        val a = answerable.map { outcomeAt(it, t) }
        val u = unanswerable.map { outcomeAt(it, t) }
        val correct = a.count { it == Outcome.CORRECT }
        val correctRefusals = u.count { it == Outcome.REFUSED }
        return SweepRow(
            threshold = t,
            answeredCorrectly = correct,
            falseRefusals = a.count { it == Outcome.REFUSED },
            wrongAnswers = a.count { it == Outcome.WRONG },
            correctRefusals = correctRefusals,
            falseAnswers = u.count { it != Outcome.REFUSED },
            accuracy = if (total == 0) 0f else (correct + correctRefusals).toFloat() / total,
        )
    }

    private fun caseLine(result: CaseResult): CaseLine {
        val top1 = result.hits.maxOfOrNull { it.score } ?: 0f
        val q = result.case.question.let { if (it.length > 70) it.take(70) + "…" else it }
        val outcome = outcomeAt(result, APP_THRESHOLD)
        val t = String.format(Locale.US, "%.2f", top1)
        return if (result.case.answerable) {
            val rank = firstCorrectRank(result)?.toString() ?: "none"
            when (outcome) {
                Outcome.CORRECT -> CaseLine(true, "$q: right page found (rank $rank, top score $t)")
                Outcome.REFUSED -> CaseLine(false, "$q: refused although answerable (correct rank $rank, top score $t)")
                Outcome.WRONG -> CaseLine(false, "$q: answered from the wrong page (correct rank $rank, top score $t)")
            }
        } else {
            if (outcome == Outcome.REFUSED) CaseLine(true, "$q: refused correctly (top score $t)")
            else CaseLine(false, "$q: would answer although unanswerable (top score $t)")

        }
    }

    private fun topHitsText(result: CaseResult): String =
        sorted(result).take(3)
            .joinToString(", ") {
                String.format(Locale.US, "p.%d %.2f (meaning %.2f, keywords %.2f)", it.pageNumber, it.score, it.dense, it.lexical)
            }
            .ifEmpty { "no chunks returned" }

    private fun stats(values: List<Float>): Stats? {
        if (values.isEmpty()) return null
        val s = values.sorted()
        val median = if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2f
        return Stats(s.first(), median, s.last())
    }
}