package com.personal.moneytracker.sync

import androidx.room.withTransaction
import com.personal.moneytracker.data.local.AppDatabase
import com.personal.moneytracker.data.local.entity.SheetSyncRecord
import com.personal.moneytracker.domain.model.SyncState
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncRunResult { SUCCESS, RETRY, AUTH_REQUIRED, NOT_CONFIGURED }

@Singleton
class SheetSyncCoordinator @Inject constructor(
    private val database: AppDatabase,
    private val settings: SheetSyncSettingsRepository,
    private val authorization: GoogleAuthorizationGateway,
    private val remote: GoogleSheetsRemote,
) {
    suspend fun syncReady(now: Long = System.currentTimeMillis()): SyncRunResult {
        val config = settings.current() ?: return SyncRunResult.NOT_CONFIGURED
        val token = when (val result = authorization.accessTokenOrNull()) {
            is AccessTokenResult.Granted -> result.token
            AccessTokenResult.AuthorizationRequired -> {
                database.transactionDao().readyToSync(now).forEach { database.transactionDao().update(it.copy(syncState = SyncState.AUTH_REQUIRED)) }
                return SyncRunResult.AUTH_REQUIRED
            }
        }
        for (transaction in database.transactionDao().readyToSync(now)) {
            val result = syncOne(transaction.id, config, token, now)
            if (result == SyncRunResult.RETRY) return result
            if (result == SyncRunResult.AUTH_REQUIRED) return result
        }
        return SyncRunResult.SUCCESS
    }

    private suspend fun syncOne(id: String, config: SheetConfig, token: String, now: Long): SyncRunResult = database.withTransaction {
        val transaction = database.transactionDao().findById(id) ?: return@withTransaction SyncRunResult.SUCCESS
        database.transactionDao().update(transaction.copy(syncState = SyncState.SYNCING))
        val sourceApps = database.transactionDao().observationsForTransaction(id).map { it.sourceApp.name }.distinct().sorted().joinToString(",")
        val row = transaction.toSheetRow(sourceApps)
        // Always find by column A first. This protects retry after an ambiguous timeout and avoids trusting a stale row.
        when (val located = remote.findTransactionRow(config, token, transaction.id)) {
            is RemoteWriteResult.Found -> finishWrite(transaction, config, located.row, row, token, now, update = true)
            RemoteWriteResult.Missing -> finishWrite(transaction, config, null, row, token, now, update = false)
            RemoteWriteResult.AuthorizationFailure -> markAuthRequired(transaction, now)
            RemoteWriteResult.TransientFailure -> markRetry(transaction, now)
            RemoteWriteResult.PermanentFailure -> markFailed(transaction, now)
        }
    }

    private suspend fun finishWrite(transaction: com.personal.moneytracker.data.local.entity.CanonicalTransaction, config: SheetConfig, rowNumber: Int?, row: List<String>, token: String, now: Long, update: Boolean): SyncRunResult {
        val result = if (update) remote.update(config, token, rowNumber!!, row) else remote.append(config, token, row)
        return when (result) {
            is RemoteWriteResult.Found -> {
                database.sheetSyncDao().upsert(SheetSyncRecord(transaction.id, config.spreadsheetId, config.sheetName, result.row.takeIf { it > 0 } ?: rowNumber, transaction.updatedAtEpochMs, now))
                database.transactionDao().update(transaction.copy(syncState = SyncState.SYNCED)); SyncRunResult.SUCCESS
            }
            RemoteWriteResult.AuthorizationFailure -> markAuthRequired(transaction, now)
            RemoteWriteResult.TransientFailure -> markRetry(transaction, now)
            else -> markFailed(transaction, now)
        }
    }
    private suspend fun markAuthRequired(t: com.personal.moneytracker.data.local.entity.CanonicalTransaction, now: Long): SyncRunResult { database.transactionDao().update(t.copy(syncState = SyncState.AUTH_REQUIRED)); return SyncRunResult.AUTH_REQUIRED }
    private suspend fun markRetry(t: com.personal.moneytracker.data.local.entity.CanonicalTransaction, now: Long): SyncRunResult { database.transactionDao().update(t.copy(syncState = SyncState.FAILED)); return SyncRunResult.RETRY }
    private suspend fun markFailed(t: com.personal.moneytracker.data.local.entity.CanonicalTransaction, now: Long): SyncRunResult { database.transactionDao().update(t.copy(syncState = SyncState.FAILED)); return SyncRunResult.SUCCESS }
}
