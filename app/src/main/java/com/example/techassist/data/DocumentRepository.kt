package com.example.techassist.data

import android.content.Context
import android.net.Uri
import com.example.techassist.data.chunk.TextChunker
import com.example.techassist.data.embedding.EmbeddingEngine
import com.example.techassist.data.search.VectorSearch
import com.example.techassist.data.local.AppDatabase
import com.example.techassist.data.local.ChunkEntity
import com.example.techassist.data.local.EmbeddingEntity
import com.example.techassist.data.local.FloatArrayConverters
import com.example.techassist.data.local.DocumentEntity
import com.example.techassist.data.local.DocumentStatus
import com.example.techassist.data.local.ExtractedPageEntity
import com.example.techassist.data.pdf.PdfExtractionResult
import com.example.techassist.data.pdf.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed class ImportProgress {
    object CopyingFile : ImportProgress()
    data class ExtractingText(val page: Int, val totalPages: Int) : ImportProgress()
    data class GeneratingEmbeddings(val chunk: Int, val totalChunks: Int) : ImportProgress()
    data class Done(val documentId: String, val status: DocumentStatus) : ImportProgress()
    data class Failed(val reason: String) : ImportProgress()
}

class DocumentRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val documentDao = db.documentDao()
    private val pageDao = db.extractedPageDao()
    private val chunkDao = db.chunkDao()
    private val embeddingDao = db.embeddingDao()
    // Loaded lazily and reused across imports; loading the ONNX session is the expensive part.
    private var embeddingEngine: EmbeddingEngine? = null

    private suspend fun getOrLoadEmbeddingEngine(): EmbeddingEngine =
        embeddingEngine ?: EmbeddingEngine.load(context).also { embeddingEngine = it }

    fun observeDocuments(): Flow<List<DocumentEntity>> = documentDao.observeAll()

    /** Copies the selected PDF into app-private storage, then extracts text page by page. */
    fun importDocument(sourceUri: Uri, displayName: String, sizeBytes: Long): Flow<ImportProgress> = flow {
        emit(ImportProgress.CopyingFile)

        val documentId = UUID.randomUUID().toString()
        val documentsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val destFile = File(documentsDir, "$documentId.pdf")

        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: run {
                emit(ImportProgress.Failed("Could not open the selected file."))
                return@flow
            }
        } catch (e: Exception) {
            emit(ImportProgress.Failed("Could not copy the file: ${e.message}"))
            return@flow
        }

        // Placeholder row so the UI can show the document immediately, before extraction finishes.
        documentDao.upsert(
            DocumentEntity(
                id = documentId,
                displayName = displayName,
                filePath = "documents/$documentId.pdf",
                sizeBytes = sizeBytes,
                pageCount = 0,
                chunkCount = 0,
                status = DocumentStatus.EXTRACTING,
                errorMessage = null,
                importedAtEpochMillis = System.currentTimeMillis(),
            )
        )

        when (val result = PdfTextExtractor.extract(destFile) { page, total ->
            // Progress callback runs synchronously inside extract(); we can't emit() from here
            // directly since we're not inside the flow builder's coroutine at that point in
            // some dispatchers, so we track and emit before/after instead (see below).
        }) {
            is PdfExtractionResult.Success -> {
                pageDao.insertAll(result.pages.map { ExtractedPageEntity(documentId, it.pageNumber, it.text) })

                val chunkEntities = result.pages.flatMap { page ->
                    TextChunker.chunkPageText(page.text).mapIndexed { index, chunk ->
                        ChunkEntity(
                            id = UUID.randomUUID().toString(),
                            documentId = documentId,
                            pageNumber = page.pageNumber,
                            chunkIndexOnPage = index,
                            text = chunk.text,
                            wordCount = chunk.wordCount,
                        )
                    }
                }
                chunkDao.insertAll(chunkEntities)

                val engine = getOrLoadEmbeddingEngine()
                val embeddingEntities = mutableListOf<EmbeddingEntity>()
                chunkEntities.forEachIndexed { index, chunk ->
                    val vector = engine.embed(chunk.text)
                    embeddingEntities.add(
                        EmbeddingEntity(
                            chunkId = chunk.id,
                            documentId = documentId,
                            vector = FloatArrayConverters.toBytes(vector),
                        )
                    )
                    emit(ImportProgress.GeneratingEmbeddings(index + 1, chunkEntities.size))
                }
                embeddingDao.insertAll(embeddingEntities)

                val updated = DocumentEntity(
                    id = documentId, displayName = displayName, filePath = "documents/$documentId.pdf",
                    sizeBytes = sizeBytes, pageCount = result.pages.size, chunkCount = chunkEntities.size,
                    status = DocumentStatus.READY,
                    errorMessage = null, importedAtEpochMillis = System.currentTimeMillis(),
                )
                documentDao.update(updated)
                emit(ImportProgress.Done(documentId, DocumentStatus.READY))
            }
            is PdfExtractionResult.Encrypted -> failAndRecord(documentId, displayName, sizeBytes, DocumentStatus.ENCRYPTED, "Password-protected PDF.")
            is PdfExtractionResult.InvalidPdf -> failAndRecord(documentId, displayName, sizeBytes, DocumentStatus.INVALID, "File is not a readable PDF.")
            is PdfExtractionResult.EmptyOrScanned -> failAndRecord(documentId, displayName, sizeBytes, DocumentStatus.EMPTY_OR_SCANNED, "No extractable text found (likely a scanned document).")
            is PdfExtractionResult.UnknownError -> failAndRecord(documentId, displayName, sizeBytes, DocumentStatus.FAILED, result.message)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<ImportProgress>.failAndRecord(
        documentId: String, displayName: String, sizeBytes: Long, status: DocumentStatus, message: String,
    ) {
        documentDao.update(
            DocumentEntity(
                id = documentId, displayName = displayName, filePath = "documents/$documentId.pdf",
                sizeBytes = sizeBytes, pageCount = 0, chunkCount = 0, status = status,
                errorMessage = message, importedAtEpochMillis = System.currentTimeMillis(),
            )
        )
        emit(ImportProgress.Done(documentId, status))
    }

    suspend fun deleteDocument(document: DocumentEntity) = withContext(Dispatchers.IO) {
        pageDao.deleteForDocument(document.id)
        chunkDao.deleteForDocument(document.id)
        embeddingDao.deleteForDocument(document.id)
        documentDao.delete(document)
        File(context.filesDir, document.filePath).delete()
    }

    suspend fun chunkCountForDocument(documentId: String): Int = chunkDao.countForDocument(documentId)

    suspend fun isDuplicateName(name: String): Boolean = documentDao.countByName(name) > 0

    data class RetrievedChunk(
        val chunkId: String,
        val documentId: String,
        val documentName: String,
        val pageNumber: Int,
        val text: String,
        val score: Float,
    )

    /**
     * Embeds [query], scores it against stored chunk vectors (optionally restricted to one
     * document), and returns up to [topK] hits scoring at or above [minSimilarity].
     * An empty result means either no documents are indexed yet, or none were relevant enough —
     * callers should distinguish those two cases for the user rather than treating both as "no results".
     */
    suspend fun searchChunks(
        query: String,
        topK: Int = 5,
        minSimilarity: Float = 0.35f,
        documentIdFilter: String? = null,
    ): List<RetrievedChunk> = withContext(Dispatchers.Default) {
        val engine = getOrLoadEmbeddingEngine()
        val queryVector = engine.embed(query)

        val candidateEmbeddings = if (documentIdFilter != null) {
            embeddingDao.getForDocument(documentIdFilter)
        } else {
            embeddingDao.getAll()
        }
        if (candidateEmbeddings.isEmpty()) return@withContext emptyList()

        val vectors = candidateEmbeddings.map { FloatArrayConverters.toFloatArray(it.vector) }
        val scored = VectorSearch.topK(queryVector, vectors, topK)
            .filter { it.score >= minSimilarity }
        if (scored.isEmpty()) return@withContext emptyList()

        val hitChunkIds = scored.map { candidateEmbeddings[it.index].chunkId }
        val chunksById = chunkDao.getByIds(hitChunkIds).associateBy { it.id }

        val distinctDocumentIds = scored.map { candidateEmbeddings[it.index].documentId }.distinct()
        val documentNamesById = distinctDocumentIds
            .mapNotNull { id -> documentDao.getById(id)?.let { id to it.displayName } }
            .toMap()

        scored.mapNotNull { hit ->
            val embeddingRow = candidateEmbeddings[hit.index]
            val chunk = chunksById[embeddingRow.chunkId] ?: return@mapNotNull null
            val documentName = documentNamesById[embeddingRow.documentId] ?: return@mapNotNull null
            RetrievedChunk(
                chunkId = chunk.id,
                documentId = embeddingRow.documentId,
                documentName = documentName,
                pageNumber = chunk.pageNumber,
                text = chunk.text,
                score = hit.score,
            )
        }
    }
}