package com.example.techassist.ui.checklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.techassist.data.checklist.ChecklistRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistListScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: ChecklistListViewModel = viewModel(),
) {
    val rows by viewModel.rows.collectAsState()
    val working by viewModel.isWorking.collectAsState()
    val message by viewModel.message.collectAsState()
    var topic by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<ChecklistRow?>(null) }

    LaunchedEffect(Unit) {
        viewModel.created.collect { id ->
            topic = ""
            onOpen(id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commissioning checklists") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("What are you installing or commissioning?") },
                placeholder = { Text("e.g. commissioning procedure for Drive X1") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Button(onClick = { viewModel.create(topic) }, enabled = topic.isNotBlank() && !working) {
                Text("Create from manuals")
            }
            if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }

            if (rows.isEmpty()) {
                Text(
                    "No checklists yet. Describe a task above and the app will collect the matching steps from your manuals.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rows, key = { it.checklist.id }) { row ->
                        ElevatedCard(onClick = { onOpen(row.checklist.id) }, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(row.checklist.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${row.done} of ${row.total} done",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    LinearProgressIndicator(
                                        progress = { if (row.total == 0) 0f else row.done.toFloat() / row.total },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                IconButton(onClick = { pendingDelete = row }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete ${row.checklist.title}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { row ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete checklist?") },
            text = { Text("\"${row.checklist.title}\" and its progress will be removed.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(row.checklist.id); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}