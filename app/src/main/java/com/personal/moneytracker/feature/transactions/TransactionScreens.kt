@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.personal.moneytracker.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.moneytracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun TransactionListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, viewModel: TransactionsViewModel = hiltViewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Transactions") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (transactions.isEmpty()) item { Text("No canonical transactions yet.") }
            items(transactions, key = { it.id }) { transaction ->
                Card(onClick = { onOpen(transaction.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${transaction.type}  ${transaction.amountVnd} ₫")
                        Text(transaction.merchant ?: "Unknown merchant")
                        Text("${transaction.categoryId} • match ${transaction.matchConfidence}")
                    }
                }
            }
        }
    }
}

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(private val repository: TransactionRepository) : ViewModel() {
    private val _detail = MutableStateFlow<com.personal.moneytracker.data.local.dao.TransactionWithObservations?>(null)
    val detail: StateFlow<com.personal.moneytracker.data.local.dao.TransactionWithObservations?> = _detail
    fun load(id: String) { viewModelScope.launch { _detail.value = repository.detail(id) } }
    fun correctCategory(id: String, category: String, locked: Boolean) { viewModelScope.launch { repository.correctCategory(id, category, locked); load(id) } }
    fun createRule(id: String, category: String) { viewModelScope.launch { repository.createRuleFromCorrection(id, category) } }
}

@Composable
fun TransactionDetailScreen(id: String, onBack: () -> Unit, viewModel: TransactionDetailViewModel = hiltViewModel()) {
    androidx.compose.runtime.LaunchedEffect(id) { viewModel.load(id) }
    val detail by viewModel.detail.collectAsState()
    var categoryInput by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("Transaction detail") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val current = detail
            if (current == null) Text("Loading transaction…") else {
                val transaction = current.transaction
                Text("${transaction.type}: ${transaction.amountVnd} ₫")
                Text("Merchant: ${transaction.merchant ?: "—"}")
                Text("Service: ${transaction.service ?: "—"}")
                Text("Category: ${transaction.categoryId}")
                Text("Match confidence: ${transaction.matchConfidence}")
                Text("Reconciliation stable after: ${transaction.reconciliationReadyAtEpochMs}")
                Text("Linked observations")
                current.links.forEach { link ->
                    val observation = current.observations.firstOrNull { it.id == link.observationId }
                    Text("${link.linkRole}: ${observation?.sourceApp ?: "—"} • ${link.explanation}")
                }
                OutlinedTextField(categoryInput, { categoryInput = it }, label = { Text("Category ID") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.correctCategory(id, categoryInput, locked = false) }, enabled = categoryInput.isNotBlank()) { Text("Save category correction") }
                Button(onClick = { viewModel.correctCategory(id, categoryInput, locked = true) }, enabled = categoryInput.isNotBlank()) { Text("Save and lock category") }
                Button(onClick = { viewModel.createRule(id, categoryInput) }, enabled = categoryInput.isNotBlank()) { Text("Create rule from correction") }
            }
        }
    }
}
