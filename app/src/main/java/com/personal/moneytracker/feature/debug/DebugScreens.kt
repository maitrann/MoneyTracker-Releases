@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.personal.moneytracker.feature.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DebugEventListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, viewModel: DebugViewModel = hiltViewModel()) {
    val events by viewModel.events.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Captured events") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (events.isEmpty()) item { Text("No captured events yet. Add an allowed package and post a test notification.") }
            items(events, key = { it.raw.id }) { event ->
                Card(onClick = { onOpen(event.raw.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(event.raw.packageName, style = MaterialTheme.typography.titleSmall)
                        Text("Result: ${event.raw.captureStatus}")
                        Text("Parser: ${event.observation?.parserName ?: event.raw.parserName ?: "—"}")
                        Text(event.raw.title ?: if (event.raw.captureStatus.name == "SENSITIVE_IGNORED") "Sensitive content removed" else "No title")
                    }
                }
            }
        }
    }
}

@Composable
fun DebugEventDetailScreen(id: String, onBack: () -> Unit, viewModel: DebugViewModel = hiltViewModel()) {
    val events by viewModel.events.collectAsState()
    val event = events.firstOrNull { it.raw.id == id }
    Scaffold(topBar = { TopAppBar(title = { Text("Parser result") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (event == null) Text("Event not found") else with(event) {
                Text("Package: ${raw.packageName}")
                Text("Result: ${raw.captureStatus}")
                Text("Parser: ${observation?.parserName ?: raw.parserName ?: "—"}")
                Text("Reason: ${raw.processingReason ?: "—"}")
                Text("Amount VND: ${observation?.amountVnd ?: "—"}")
                Text("Direction: ${observation?.directionHint ?: "—"}")
                Text("Merchant: ${observation?.merchant ?: "—"}")
                Text("Service: ${observation?.service ?: "—"}")
                Text("Confidence: ${observation?.confidence ?: "—"}")
                Text("Title: ${raw.title.orEmpty()}")
                Text("Text: ${raw.text.orEmpty()}")
                Text("Big text: ${raw.bigText.orEmpty()}")
                Text("Sub text: ${raw.subText.orEmpty()}")
                Text("Fingerprint: ${raw.contentHash}")
            }
        }
    }
}
