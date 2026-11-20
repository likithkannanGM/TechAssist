package com.example.techassist.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private data class HomeAction(val title: String, val description: String)

private val mainActions = listOf(
    HomeAction("New conversation", "Ask a technical question about your manuals"),
    HomeAction("Document library", "Import and manage PDF documentation"),
    HomeAction("Troubleshooting mode", "Guided diagnosis from documented procedures"),
    HomeAction("Commissioning checklist", "Step-by-step checklist from manuals"),
)

private val toolActions = listOf(

    HomeAction("Test retrieval (debug)", "See which chunks match a question, with scores"),
    HomeAction("Test local LLM (debug)", "Import a GGUF model and generate text on-device"),
    HomeAction("Evaluation (debug)", "Measure retrieval quality and tune the relevance threshold"),
)

@Composable
fun HomeScreen(
    isDeviceOnline: Boolean,
    onOpenLibrary: () -> Unit,
    onOpenRetrievalDebug: () -> Unit,
    onOpenLlmDebug: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenTroubleshoot: () -> Unit,
    onOpenChecklists: () -> Unit,
    onOpenEvaluation: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val readyDocuments by viewModel.readyDocuments.collectAsState()
    val modelText by viewModel.modelText.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshModel() }

    val open: (String) -> Unit = { title ->
        when (title) {
            "New conversation" -> onOpenChat()
            "Document library" -> onOpenLibrary()
            "Troubleshooting mode" -> onOpenTroubleshoot()
            "Commissioning checklist" -> onOpenChecklists()
            "Test retrieval (debug)" -> onOpenRetrievalDebug()
            "Test local LLM (debug)" -> onOpenLlmDebug()
            "Evaluation (debug)" -> onOpenEvaluation()
        }
    }


    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("TechAssist", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "AI-Powered Virtual Assistant for Real-Time Installation and Commissioning Support",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Status", style = MaterialTheme.typography.titleMedium)
                    StatusRow("Device network", if (isDeviceOnline) "Connected" else "Offline (the app works without it)")
                    StatusRow("Language model", modelText)
                    StatusRow("Embedding model", "Built in")
                    StatusRow("Documents", "$readyDocuments ready")
                }
            }

            mainActions.forEach { ActionCard(it) { open(it.title) } }

            Text(

                "Developer tools",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            toolActions.forEach { ActionCard(it) { open(it.title) } }

            Text(
                "All processing runs on this device. Answers show their source; always check the manual.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionCard(action: HomeAction, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(action.title, style = MaterialTheme.typography.titleMedium)
            Text(action.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}