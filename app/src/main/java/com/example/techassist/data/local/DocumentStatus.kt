package com.example.techassist.data.local

/** Lifecycle of an imported document, shown in the Document Library UI. */
enum class DocumentStatus {
    IMPORTING,       // file is being copied into app storage
    EXTRACTING,      // text extraction in progress
    READY,           // extraction succeeded, text available for later chunking
    EMPTY_OR_SCANNED,// no extractable text found (likely a scanned PDF; OCR is a later phase)
    ENCRYPTED,       // PDF is password-protected, could not be opened
    INVALID,         // file could not be parsed as a PDF
    FAILED,          // unexpected error; see errorMessage
}