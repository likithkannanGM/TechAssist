package com.example.techassist.ui.checklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.techassist.data.checklist.ChecklistItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistDetailScreen(
    onBack: () -> Unit,
    viewModel: ChecklistDetailViewModel = viewModel(),
) {
    val checklist by viewModel.checklist.collectAsState()
    val items by viewModel.items.collectAsState()
    val done = items.count { it.isDone }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(checklist?.title ?: "Checklist", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = { viewModel.reset() }, enabled = done > 0) { Text("Reset") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$done of ${items.size} done", style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(
                    progress = { if (items.isEmpty()) 0f else done.toFloat() / items.size },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    ItemRow(
                        item = item,
                        onToggle = { viewModel.toggle(item) },
                        onDelete = { viewModel.deleteItem(item) },
                    )
                }
                item {
                    Text(
                        "Steps are copied from the manuals' own lists. Check each one against the source page and follow site safety procedures.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: ChecklistItemEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    val container = if (item.isSafety) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.surfaceVariant
    Surface(color = container, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.isDone, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(item.source, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove this item")
            }
        }
    }
}