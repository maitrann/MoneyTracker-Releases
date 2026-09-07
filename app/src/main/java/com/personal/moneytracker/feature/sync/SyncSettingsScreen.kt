@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.personal.moneytracker.feature.sync

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.identity.Identity
import com.personal.moneytracker.data.local.AppDatabase
import com.personal.moneytracker.sync.GoogleIdentityAuthorizationGateway
import com.personal.moneytracker.sync.SheetSyncScheduler
import com.personal.moneytracker.sync.SheetSyncSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncSettingsViewModel @Inject constructor(
    private val settings: SheetSyncSettingsRepository,
    private val scheduler: SheetSyncScheduler,
    private val database: AppDatabase,
) : ViewModel() {
    val config = settings.config
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    fun save(value: String, tab: String) = viewModelScope.launch {
        _message.value = if (settings.save(value, tab)) { scheduler.schedule(); "Saved. Sync will run when authorized and online." } else "Enter a valid Google Sheets URL or spreadsheet ID."
    }
    fun authorizationSucceeded() = viewModelScope.launch { database.transactionDao().requeueAuthorizationRequired(); scheduler.schedule(); _message.value = "Google Sheets access granted. Sync scheduled." }
    fun authorizationFailed() { _message.value = "Authorization was not granted; financial data remains local." }
}

@Composable
fun SyncSettingsScreen(onBack: () -> Unit, viewModel: SyncSettingsViewModel = hiltViewModel()) {
    val config by viewModel.config.collectAsStateWithLifecycle(initialValue = null)
    val message by viewModel.message.collectAsStateWithLifecycle()
    var sheet by remember(config) { mutableStateOf(config?.spreadsheetId.orEmpty()) }
    var tab by remember(config) { mutableStateOf(config?.sheetName ?: "Transactions") }
    val context = LocalContext.current
    val activity = context as? Activity
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        try { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data); viewModel.authorizationSucceeded() }
        catch (_: Exception) { viewModel.authorizationFailed() }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Google Sheets sync") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Room remains the source of truth. Use a dedicated test sheet first.")
            OutlinedTextField(sheet, { sheet = it }, label = { Text("Spreadsheet URL or ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(tab, { tab = it }, label = { Text("Target tab") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { viewModel.save(sheet, tab) }) { Text("Save sync destination") }
            Button(onClick = {
                if (activity == null) { viewModel.authorizationFailed(); return@Button }
                Identity.getAuthorizationClient(activity).authorize(GoogleIdentityAuthorizationGateway.request())
                    .addOnSuccessListener { result -> if (result.hasResolution()) launcher.launch(IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()) else viewModel.authorizationSucceeded() }
                    .addOnFailureListener { viewModel.authorizationFailed() }
            }) { Text("Connect Google Sheets") }
            Text("Google handles consent. This app never asks for or stores your Google password or a service-account key.")
            message?.let { Text(it) }
        }
    }
}
