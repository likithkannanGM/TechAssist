package com.example.techassist.data.embedding

/**
 * Minimal BERT-style WordPiece tokenizer: basic whitespace/punctuation splitting,
 * then greedy longest-match subword lookup against the supplied vocabulary.
 * Good enough for English technical text; not a full implementation of every
 * Unicode edge case the original BERT tokenizer handles (e.g. CJK characters).
 */
class BertTokenizer(private val vocab: Map<String, Int>) {

    data class Encoding(val inputIds: LongArray, val attentionMask: LongArray, val tokenTypeIds: LongArray)

    private val clsId = vocab["[CLS]"] ?: error("Vocab missing [CLS]")
    private val sepId = vocab["[SEP]"] ?: error("Vocab missing [SEP]")
    private val padId = vocab["[PAD]"] ?: error("Vocab missing [PAD]")
    private val unkId = vocab["[UNK]"] ?: error("Vocab missing [UNK]")

    fun encode(text: String, maxSeqLen: Int): Encoding {
        val wordPieces = wordpieceTokenize(basicTokenize(text))
        val truncated = wordPieces.take(maxSeqLen - 2) // room for [CLS] and [SEP]

        val ids = mutableListOf(clsId)
        ids.addAll(truncated.map { vocab[it] ?: unkId })
        ids.add(sepId)

        val realLength = ids.size
        while (ids.size < maxSeqLen) ids.add(padId)

        val inputIds = ids.map { it.toLong() }.toLongArray()
        val attentionMask = LongArray(maxSeqLen) { if (it < realLength) 1L else 0L }
        val tokenTypeIds = LongArray(maxSeqLen) { 0L }


        return Encoding(inputIds, attentionMask, tokenTypeIds)
    }

    private fun basicTokenize(text: String): List<String> {
        val lower = text.lowercase()
        val spaced = StringBuilder()
        for (ch in lower) {
            if (isPunctuation(ch)) {
                spaced.append(' ').append(ch).append(' ')
            } else {
                spaced.append(ch)
            }
        }
        return spaced.toString().split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    private fun isPunctuation(ch: Char): Boolean {
        val code = ch.code
        return (code in 33..47) || (code in 58..64) || (code in 91..96) || (code in 123..126)
    }

    private fun wordpieceTokenize(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        for (token in tokens) {
            if (token.length > 100) { output.add("[UNK]"); continue }

            var start = 0
            var found = true
            val subTokens = mutableListOf<String>()
            while (start < token.length) {
                var end = token.length
                var matched: String? = null

                while (start < end) {
                    var candidate = token.substring(start, end)
                    if (start > 0) candidate = "##$candidate"
                    if (vocab.containsKey(candidate)) { matched = candidate; break }
                    end--
                }
                if (matched == null) { found = false; break }
                subTokens.add(matched)
                start = end
            }
            if (found) output.addAll(subTokens) else output.add("[UNK]")
        }
        return output
    }
}