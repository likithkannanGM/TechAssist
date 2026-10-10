package com.example.techassist.ui.retrieval
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.techassist.data.DocumentRepository
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetrievalDebugScreen(
    onBack: () -> Unit,
    viewModel: RetrievalDebugViewModel = viewModel(),
) {
    var query by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Retrieval (debug)") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Ask a question about an imported document") },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { viewModel.search(query) }, enabled = query.isNotBlank()) {
                    Text("Search")
                }
            }
            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is RetrievalUiState.Idle -> HintText("Results and similarity scores will appear here.")
                is RetrievalUiState.Searching -> LinearProgressIndicator(Modifier.fillMaxWidth())
                is RetrievalUiState.NoDocumentsIndexed -> HintText("No documents are ready yet — import one in the Document Library first.")
                is RetrievalUiState.NoRelevantMatch -> HintText("No chunk scored above the relevance threshold for this question. This is the same signal the real assistant will use to say \"not enough information\".")
                is RetrievalUiState.Results -> ResultsList(s.hits)
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ResultsList(hits: List<DocumentRepository.RetrievedChunk>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(hits) { hit ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${hit.documentName}, p. ${hit.pageNumber}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("score ${(hit.score * 100).roundToInt()}%") },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        hit.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}