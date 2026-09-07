@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.personal.moneytracker.feature.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.moneytracker.data.repository.CategorizationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(private val repository: CategorizationRepository) : ViewModel() {
    val rules = repository.observeRules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    init { viewModelScope.launch { repository.initialize() } }
    fun toggle(rule: com.personal.moneytracker.data.local.entity.CategorizationRule, enabled: Boolean) { viewModelScope.launch { repository.setRuleEnabled(rule, enabled) } }
}

@Composable
fun RulesScreen(onBack: () -> Unit, viewModel: RulesViewModel = hiltViewModel()) {
    val rules by viewModel.rules.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Categorization rules") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rules, key = { it.id }) { rule ->
                Column {
                    Text("${rule.source}: ${rule.field} ${rule.operator} ${rule.pattern}")
                    Text("→ ${rule.targetCategoryId}")
                    Switch(checked = rule.enabled, onCheckedChange = { viewModel.toggle(rule, it) })
                }
            }
        }
    }
}
