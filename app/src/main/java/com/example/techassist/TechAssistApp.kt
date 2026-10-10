package com.example.techassist

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class TechAssistApp : Application() {
    val llmEngine: com.example.techassist.data.llm.LlmEngine by lazy {
        com.example.techassist.data.llm.LlamaCppEngine()
    }
    override fun onCreate() {
        super.onCreate()
        // Required once, before any PDFBox call. Loads PDFBox's font/resource cache.
        PDFBoxResourceLoader.init(applicationContext)
    }
}