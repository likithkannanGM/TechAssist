package com.example.techassist.ui.eval

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluationScreen(
    onBack: () -> Unit,
    viewModel: EvaluationViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var includeTiming by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onFilePicked(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evaluation (debug)") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Pick a test file (lines like ANSWERABLE | question | document | pages, or UNANSWERABLE | question), then run it against your imported documents.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickFile.launch(arrayOf("*/*")) }, enabled = !state.isRunning) {
                        Text("Choose test file")
                    }
                    if (state.isRunning) {
                        OutlinedButton(onClick = { viewModel.stop() }) { Text("Stop") }
                    } else {
                        Button(onClick = { viewModel.run(includeTiming) }, enabled = state.cases.isNotEmpty()) {
                            Text("Run evaluation")
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeTiming, onCheckedChange = { includeTiming = it }, enabled = !state.isRunning)
                    Text("Also time answer generation (first 3 questions, slow)", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (state.cases.isNotEmpty()) {
                item {
                    val a = state.cases.count { it.answerable }
                    Text(
                        "${state.cases.size} test cases loaded ($a answerable, ${state.cases.size - a} unanswerable)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            if (state.errors.isNotEmpty()) {
                item {
                    Text(
                        (state.errors.take(5) + listOfNotNull(
                            if (state.errors.size > 5) "…and ${state.errors.size - 5} more" else null
                        )).joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            state.message?.let {
                item { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            }
            state.progress?.let {
                item {
                    Column {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }

            state.report?.let { report ->
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Text(
                            report.summaryText(),
                            modifier = Modifier.padding(16.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                }
                state.timing?.let { timing ->
                    item {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Text(timing, modifier = Modifier.padding(16.dp), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
                item {
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(report.fullText() + (state.timing?.let { "\n\n$it" } ?: "")))
                    }) { Text("Copy full report") }
                }
                item { Text("Per-question results", style = MaterialTheme.typography.titleMedium) }
                items(report.caseLines) { line ->
                    Text(
                        (if (line.ok) "OK   " else "FAIL ") + line.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (line.ok) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}