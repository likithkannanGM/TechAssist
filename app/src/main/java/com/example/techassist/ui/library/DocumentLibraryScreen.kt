package com.example.techassist.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.techassist.data.local.DocumentEntity
import com.example.techassist.data.local.DocumentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentLibraryScreen(
    onBack: () -> Unit,
    viewModel: DocumentLibraryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val name = queryDisplayName(context, uri) ?: "document.pdf"
            val size = querySize(context, uri)
            viewModel.onDocumentPicked(uri, name, size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Library") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                text = { Text("Import PDF") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val progressText by viewModel.progressText.collectAsState()
            if (isImporting) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    progressText ?: "Working…",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            errorMessage?.let { msg ->
                AssistChip(
                    onClick = { viewModel.dismissError() },
                    label = { Text(msg) },
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (documents.isEmpty() && !isImporting) {
                EmptyState()
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentRow(doc, onDelete = { viewModel.deleteDocument(doc) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "No documents yet. Tap \"Import PDF\" to add a manual.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DocumentRow(document: DocumentEntity, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(document.displayName, style = MaterialTheme.typography.titleMedium)
                val chunkInfo = if (document.status == DocumentStatus.READY) " · ${document.chunkCount} chunks" else ""
                Text(
                    "${formatSize(document.sizeBytes)} · ${statusLabel(document)}$chunkInfo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                document.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove ${document.displayName}")
            }
        }
    }
}

private fun statusLabel(document: DocumentEntity): String = when (document.status) {
    DocumentStatus.IMPORTING -> "Importing…"
    DocumentStatus.EXTRACTING -> "Extracting text…"
    DocumentStatus.READY -> "${document.pageCount} pages, ready"
    DocumentStatus.EMPTY_OR_SCANNED -> "No extractable text (scanned?)"
    DocumentStatus.ENCRYPTED -> "Password-protected, could not import"
    DocumentStatus.INVALID -> "Not a readable PDF"
    DocumentStatus.FAILED -> "Import failed"
}

private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    return if (kb < 1024) "%.0f KB".format(kb) else "%.1f MB".format(kb / 1024.0)
}

private fun queryDisplayName(context: android.content.Context, uri: android.net.Uri): String? {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) return cursor.getString(nameIndex)
    }
    return null
}

private fun querySize(context: android.content.Context, uri: android.net.Uri): Long {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
        if (cursor.moveToFirst() && sizeIndex >= 0) return cursor.getLong(sizeIndex)
    }
    return 0L
}