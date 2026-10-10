package com.example.techassist.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Describes one action tile on the home screen. */
private data class HomeAction(
    val title: String,
    val description: String,
    val availableInPhase: Int, // Phase in which this feature becomes functional
)

private val homeActions = listOf(
    HomeAction("New conversation", "Ask a technical question about your manuals", 0),
    HomeAction("Document library", "Import and manage PDF documentation", 0), // 0 = available now
    HomeAction("Test retrieval (debug)", "See which chunks match a question, with scores", 0),
    HomeAction("Test local LLM (debug)", "Load a GGUF model and generate text on-device", 0),
    HomeAction("Troubleshooting mode", "Guided diagnosis from documented procedures", 0 ),
    HomeAction("Commissioning checklist", "Step-by-step checklist from manuals", 9),
)

@Composable
fun HomeScreen(
    isDeviceOnline: Boolean,
    onOpenLibrary: () -> Unit,
    onOpenRetrievalDebug: () -> Unit,
    onOpenLlmDebug: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenTroubleshoot: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header()
            OfflineStatusCard(isDeviceOnline)
            homeActions.forEach { action ->
                ActionCard(
                    action = action,
                    onClick = when (action.title) {
                        "Document library" -> onOpenLibrary
                        "Test retrieval (debug)" -> onOpenRetrievalDebug
                        "Test local LLM (debug)" -> onOpenLlmDebug
                        "New conversation" -> onOpenChat
                        "Troubleshooting mode" -> onOpenTroubleshoot
                        else -> null
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Prototype build: Phase 1 (UI shell). Features marked \"Planned\" are not implemented yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("TechAssist", style = MaterialTheme.typography.headlineLarge)
        Text(
            "AI-Powered Virtual Assistant for Real-Time Installation and Commissioning Support",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OfflineStatusCard(isDeviceOnline: Boolean) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Status", style = MaterialTheme.typography.titleMedium)
            StatusRow(
                label = "Device network",
                value = if (isDeviceOnline) "Connected" else "Offline (airplane mode or no network)",
            )
            StatusRow(label = "Language model", value = "Not installed (planned, Phase 6)")
            StatusRow(label = "Embedding model", value = "Not installed (planned, Phase 4)")
            StatusRow(label = "Documents", value = "0 imported")
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionCard(action: HomeAction, onClick: (() -> Unit)?) {
    // Disabled until the corresponding phase is built, so nothing appears to work when it doesn't.
    ElevatedCard(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(action.title, style = MaterialTheme.typography.titleMedium)
            Text(action.description, style = MaterialTheme.typography.bodyMedium)
            if (onClick == null) {
                Text(
                    "Planned: Phase ${action.availableInPhase}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}