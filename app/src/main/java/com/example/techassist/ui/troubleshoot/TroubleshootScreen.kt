package com.example.techassist.ui.troubleshoot

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TroubleshootScreen(
    onBack: () -> Unit,
    viewModel: TroubleshootViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var fault by rememberSaveable { mutableStateOf("") }
    var equipment by rememberSaveable { mutableStateOf("") }
    var checked by rememberSaveable { mutableStateOf("") }
    val busy = state.phase == TroubleshootPhase.WORKING

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Troubleshooting") },

                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = fault, onValueChange = { fault = it },
                label = { Text("Fault or symptom (required)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )
            OutlinedTextField(
                value = equipment, onValueChange = { equipment = it },
                label = { Text("Equipment / model (optional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            OutlinedTextField(
                value = checked, onValueChange = { checked = it },
                label = { Text("Already checked or observed (optional)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )

            if (busy) {
                OutlinedButton(onClick = { viewModel.stop() }) { Text("Stop") }
            } else {
                Button(
                    onClick = { viewModel.diagnose(fault, equipment, checked) },

                    enabled = fault.isNotBlank(),
                ) { Text(if (state.phase == TroubleshootPhase.DONE) "Diagnose again" else "Diagnose") }
            }

            state.status?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            state.message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            if (state.safetyNotes.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Safety notes from the manual", style = MaterialTheme.typography.titleMedium)
                        state.safetyNotes.forEach { note ->
                            Column {
                                Text("${note.level}: ${note.text}", style = MaterialTheme.typography.bodyMedium)
                                Text(note.source, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            if (state.answer.isNotEmpty()) {
                ElevatedCard(Modifier.fillMaxWidth()) {

                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Suggested diagnosis", style = MaterialTheme.typography.titleMedium)
                        Text(state.answer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (state.excerpts.isNotEmpty()) {
                Text("Matching manual pages", style = MaterialTheme.typography.titleMedium)
                state.excerpts.forEach { ExcerptCard(it) }
                Text(
                    "Assistance only. Check every step against the manual and follow site safety procedures.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExcerptCard(excerpt: Excerpt) {
    var expanded by remember(excerpt) { mutableStateOf(false) }
    ElevatedCard(Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(excerpt.source, style = MaterialTheme.typography.titleSmall)
            Text(
                excerpt.text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (expanded) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                if (expanded) "Tap to collapse" else "Tap to read the full excerpt",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}