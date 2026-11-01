package com.example.techassist

import com.example.techassist.data.eval.CaseResult
import com.example.techassist.data.eval.EvalCase
import com.example.techassist.data.eval.EvalScorer
import com.example.techassist.data.eval.ScoredHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EvalScorerTest {

    private fun answerable(q: String, doc: String, vararg pages: Int) =
        EvalCase(1, q, true, doc, pages.toSet())
    private fun unanswerable(q: String) = EvalCase(1, q, false, "", emptySet())
    private fun hit(doc: String, page: Int, score: Float) = ScoredHit(doc, page, score)

    // q1: right page is rank 1. q2: right page is rank 2. q3: unanswerable, weak top score.
    private val results = listOf(
        CaseResult(answerable("q1", "a.pdf", 3), listOf(hit("a.pdf", 3, 0.8f), hit("a.pdf", 9, 0.5f))),
        CaseResult(answerable("q2", "a.pdf", 7), listOf(hit("a.pdf", 2, 0.6f), hit("a.pdf", 7, 0.42f))),
        CaseResult(unanswerable("q3"), listOf(hit("a.pdf", 1, 0.22f))),
    )

    @Test fun `hit rates and MRR are computed over answerable questions only`() {
        val report = EvalScorer.score(results, emptyList())
        assertEquals(0.5f, report.hitAt1, 0.001f)
        assertEquals(1.0f, report.hitAt3, 0.001f)
        assertEquals(0.75f, report.mrr, 0.001f)
        assertEquals(2, report.answerableCount)
        assertEquals(1, report.unanswerableCount)
    }

    @Test fun `a hit from the wrong document is not correct even on the right page number`() {
        val r = listOf(CaseResult(answerable("q", "a.pdf", 3), listOf(hit("other.pdf", 3, 0.9f))))
        assertEquals(0f, EvalScorer.score(r, emptyList()).hitAt1, 0.001f)
    }

    @Test fun `low thresholds let an unanswerable question through`() {
        val row = EvalScorer.score(results, emptyList()).sweep.first { abs(it.threshold - 0.10f) < 0.001f }
        assertEquals(1, row.falseAnswers)
        assertEquals(0, row.correctRefusals)
    }

    @Test fun `a high threshold refuses answerable questions`() {
        val row = EvalScorer.score(results, emptyList()).sweep.first { abs(it.threshold - 0.70f) < 0.001f }
        assertEquals(1, row.falseRefusals)   // q2 never reaches 0.70
        assertEquals(1, row.answeredCorrectly)
        assertEquals(1, row.correctRefusals)
    }

    @Test fun `mid thresholds can pick the wrong page`() {
        val row = EvalScorer.score(results, emptyList()).sweep.first { abs(it.threshold - 0.45f) < 0.001f }
        assertEquals(1, row.wrongAnswers)    // q2: only the 0.6 chunk (page 2) passes
    }

    @Test fun `suggested threshold is the most accurate and ties go to the higher value`() {
        val report = EvalScorer.score(results, emptyList())
        val suggested = report.suggested
        assertNotNull(suggested)
        assertEquals(1.0f, suggested!!.accuracy, 0.001f)
        assertEquals(0.40f, suggested.threshold, 0.001f)
    }

    @Test fun `case lines reflect the current app threshold`() {
        val report = EvalScorer.score(results, emptyList())
        assertEquals(3, report.caseLines.size)
        assertTrue(report.caseLines.all { it.ok })   // at the default 0.35 all three behave correctly here
    }

    @Test fun `empty results do not crash`() {
        val report = EvalScorer.score(emptyList(), emptyList())
        assertEquals(0, report.answerableCount)
        assertEquals(null, report.suggested)
    }
}