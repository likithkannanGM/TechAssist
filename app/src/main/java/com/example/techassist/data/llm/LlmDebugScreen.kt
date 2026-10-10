package com.example.techassist.ui.llm

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmDebugScreen(
    onBack: () -> Unit,
    viewModel: LlmDebugViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var prompt by remember { mutableStateOf("In one sentence, what is a commissioning checklist?") }

    val pickModel = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onModelPicked(uri)
    }
    val busy = state.isCopying || state.isLoading || state.isGenerating

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Local LLM (debug)") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                when {
                    state.isCopying -> "Copying model into app storage…"
                    state.modelPresent -> "Model file: ${state.modelSizeMb} MB · " + if (state.isLoaded) "loaded" else "not loaded"
                    else -> "No model file yet. Pick a .gguf file."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickModel.launch(arrayOf("*/*")) }, enabled = !busy) {
                    Text("Choose model")
                }
                Button(onClick = { viewModel.loadModel() }, enabled = state.modelPresent && !busy && !state.isLoaded) {
                    Text("Load model")
                }
            }
            if (state.isCopying || state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.generate(prompt) }, enabled = state.isLoaded && !busy && prompt.isNotBlank()) {
                    Text("Generate")
                }
                OutlinedButton(onClick = { viewModel.stop() }, enabled = state.isGenerating) { Text("Stop") }
            }

            state.metrics?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            state.error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            if (state.output.isNotEmpty()) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Text(state.output, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}