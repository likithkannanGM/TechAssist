package com.example.techassist.data.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.IOException

sealed class PdfExtractionResult {
    data class Success(val pages: List<PageText>) : PdfExtractionResult()
    object Encrypted : PdfExtractionResult()
    object InvalidPdf : PdfExtractionResult()
    object EmptyOrScanned : PdfExtractionResult()
    data class UnknownError(val message: String) : PdfExtractionResult()
}

data class PageText(val pageNumber: Int, val text: String)

/**
 * Extracts per-page text from a PDF file already stored on local disk.
 * Must run off the main thread; a large PDF can take several seconds.
 */
object PdfTextExtractor {

    fun extract(file: File, onPageDone: (page: Int, totalPages: Int) -> Unit): PdfExtractionResult {
        return try {
            PDDocument.load(file).use { document ->
                val totalPages = document.numberOfPages
                if (totalPages == 0) return PdfExtractionResult.EmptyOrScanned

                val stripper = PDFTextStripper()
                val pages = mutableListOf<PageText>()

                for (pageIndex in 1..totalPages) {
                    stripper.startPage = pageIndex
                    stripper.endPage = pageIndex
                    val rawText = stripper.getText(document)
                    val cleaned = cleanText(rawText)
                    pages.add(PageText(pageIndex, cleaned))
                    onPageDone(pageIndex, totalPages)
                }

                val hasAnyText = pages.any { it.text.isNotBlank() }
                if (!hasAnyText) PdfExtractionResult.EmptyOrScanned
                else PdfExtractionResult.Success(pages)
            }
        } catch (e: InvalidPasswordException) {
            PdfExtractionResult.Encrypted
        } catch (e: IOException) {
            // PDFBox throws IOException both for malformed files and some read failures.
            PdfExtractionResult.InvalidPdf
        } catch (e: Exception) {
            PdfExtractionResult.UnknownError(e.message ?: "Unknown extraction error")
        }
    }

    /** Collapses excess whitespace left by PDF text layout without altering wording. */
    private fun cleanText(raw: String): String =
        raw.replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
}