@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.personal.moneytracker.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.personal.moneytracker.data.local.entity.CanonicalTransaction
import com.personal.moneytracker.domain.dashboard.CategoryTotal
import com.personal.moneytracker.domain.dashboard.DashboardSummary
import com.personal.moneytracker.domain.model.TransactionType
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(onBack: () -> Unit, viewModel: DashboardViewModel = hiltViewModel()) {
    val summary by viewModel.summary.collectAsState()
    val month by viewModel.selectedMonth.collectAsState()
    val query by viewModel.searchQuery.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Dashboard") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MonthSelector(
                    month = month,
                    onPrevious = viewModel::selectPreviousMonth,
                    onNext = viewModel::selectNextMonth,
                )
            }
            item { SummaryCards(summary) }
            item { PendingSyncBanner(summary.pendingSyncCount) }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::updateSearchQuery,
                    label = { Text("Search merchant, category, note…") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            if (summary.categoryTotals.isNotEmpty()) {
                item {
                    Text("Spending by category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(summary.categoryTotals, key = { it.categoryId }) { category -> CategoryTotalRow(category) }
            }
            item {
                Text("Recent transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (summary.recentTransactions.isEmpty()) {
                item { Text("No transactions for this month yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(summary.recentTransactions, key = { it.id }) { transaction -> RecentTransactionRow(transaction) }
            }
        }
    }
}

@Composable
private fun MonthSelector(month: java.time.YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) { Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous month") }
        val label = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}"
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = onNext) { Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next month") }
    }
}

@Composable
private fun SummaryCards(summary: DashboardSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Income", summary.incomeVnd, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
            MetricCard("Expense", summary.expenseVnd, MaterialTheme.colorScheme.errorContainer, Modifier.weight(1f))
        }
        MetricCard(
            label = "Net cash flow",
            amountVnd = summary.netCashFlowVnd,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MetricCard(label: String, amountVnd: Long, containerColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(formatVnd(amountVnd), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PendingSyncBanner(pendingCount: Int) {
    if (pendingCount <= 0) return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Text(
            text = "$pendingCount transaction${if (pendingCount == 1) "" else "s"} waiting to sync to Google Sheets.",
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun CategoryTotalRow(category: CategoryTotal) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(category.categoryName, fontWeight = FontWeight.Bold)
                Text("${category.count} transaction${if (category.count == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatVnd(category.totalVnd), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecentTransactionRow(transaction: CanonicalTransaction) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(transaction.merchant ?: transaction.service ?: "Unknown", fontWeight = FontWeight.Bold)
                val sign = when (transaction.type) {
                    TransactionType.INCOME -> "+"
                    TransactionType.EXPENSE -> "-"
                    else -> ""
                }
                Text("$sign${formatVnd(transaction.amountVnd)}", fontWeight = FontWeight.Bold)
            }
            Text(
                "${transaction.type} • ${transaction.categoryId} • ${formatDate(transaction.occurredAtEpochMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (transaction.syncState.name != "SYNCED") {
                Text(transaction.syncState.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatVnd(amountVnd: Long): String {
    val formatted = String.format(Locale.US, "%,d", amountVnd)
    return "$formatted ₫"
}

private fun formatDate(epochMs: Long): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
    return java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault()).format(formatter)
}
