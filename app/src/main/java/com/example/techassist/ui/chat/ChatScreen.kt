package com.example.techassist.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(),
) {
    val messages by viewModel.messages.collectAsState()
    val busy by viewModel.isBusy.collectAsState()
    val status by viewModel.status.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Keep the newest text in view while the answer streams in.
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ask your manuals") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            "Ask a question about an imported manual. Answers use only your documents and list their sources.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(messages, key = { it.id }) { MessageBubble(it) }
            }

            status?.let {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Your question") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                )
                if (busy) {
                    OutlinedButton(onClick = { viewModel.stop() }) { Text("Stop") }
                } else {
                    Button(
                        onClick = { viewModel.ask(input); input = "" },
                        enabled = input.isNotBlank(),
                    ) { Text("Send") }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == Role.USER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = message.text.ifEmpty { if (message.isStreaming) "…" else "" },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (message.sources.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Sources", style = MaterialTheme.typography.labelMedium)
                        message.sources.forEach {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}