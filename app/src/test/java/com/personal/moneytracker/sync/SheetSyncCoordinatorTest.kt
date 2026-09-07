package com.personal.moneytracker.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.moneytracker.data.local.AppDatabase
import com.personal.moneytracker.data.local.entity.CanonicalTransaction
import com.personal.moneytracker.domain.model.SyncState
import com.personal.moneytracker.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SheetSyncCoordinatorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: AppDatabase; private lateinit var settings: SheetSyncSettingsRepository; private lateinit var remote: FakeRemote
    @Before fun setUp() { database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build(); settings = SheetSyncSettingsRepository(context); remote = FakeRemote() }
    @After fun tearDown() { database.close() }
    @Test fun appendAndEditedTransactionUpdateSameRemoteRow() = runTest {
        settings.save("abcdefghijklmnopqrst", "Transactions"); insert("one"); val coordinator = coordinator()
        assertEquals(SyncRunResult.SUCCESS, coordinator.syncReady(10)); assertEquals(1, remote.appendCalls)
        val t = database.transactionDao().findById("one")!!; database.transactionDao().update(t.copy(note = "edited", updatedAtEpochMs = 20, syncState = SyncState.QUEUED))
        assertEquals(SyncRunResult.SUCCESS, coordinator.syncReady(30)); assertEquals(1, remote.appendCalls); assertEquals(1, remote.updateCalls)
    }
    @Test fun timeoutAfterRemoteAppendIsRecoveredWithoutDuplicateAppend() = runTest {
        settings.save("abcdefghijklmnopqrst", "Transactions"); insert("timeout"); remote.timeoutOnceAfterAppend = true; val coordinator = coordinator()
        assertEquals(SyncRunResult.RETRY, coordinator.syncReady(10)); assertEquals(SyncRunResult.SUCCESS, coordinator.syncReady(20)); assertEquals(1, remote.appendCalls); assertEquals(1, remote.updateCalls)
    }
    @Test fun authorizationFailureBecomesAuthRequiredWithoutRetry() = runTest {
        settings.save("abcdefghijklmnopqrst", "Transactions"); insert("auth")
        val c = SheetSyncCoordinator(database, settings, object : GoogleAuthorizationGateway { override suspend fun accessTokenOrNull() = AccessTokenResult.AuthorizationRequired }, remote)
        assertEquals(SyncRunResult.AUTH_REQUIRED, c.syncReady(10)); assertEquals(SyncState.AUTH_REQUIRED, database.transactionDao().findById("auth")!!.syncState)
    }
    private fun coordinator() = SheetSyncCoordinator(database, settings, object : GoogleAuthorizationGateway { override suspend fun accessTokenOrNull() = AccessTokenResult.Granted("test") }, remote)
    private suspend fun insert(id: String) { database.transactionDao().insert(CanonicalTransaction(id, 1, 125_000, type = TransactionType.EXPENSE, merchant = "Test", service = null, subcategory = null, fundingSource = null, paymentChannel = null, description = null, note = null, classificationConfidence = 0.0, matchConfidence = 1.0, reconciliationReadyAtEpochMs = 0, createdAtEpochMs = 1, updatedAtEpochMs = 1)) }
    private class FakeRemote : GoogleSheetsRemote {
        val rows = linkedMapOf<String, Int>(); var appendCalls = 0; var updateCalls = 0; var timeoutOnceAfterAppend = false
        override suspend fun findTransactionRow(config: SheetConfig, accessToken: String, transactionId: String) = rows[transactionId]?.let { RemoteWriteResult.Found(it) } ?: RemoteWriteResult.Missing
        override suspend fun append(config: SheetConfig, accessToken: String, row: List<String>): RemoteWriteResult { appendCalls++; rows[row.first()] = rows.size + 1; return if (timeoutOnceAfterAppend.also { timeoutOnceAfterAppend = false }) RemoteWriteResult.TransientFailure else RemoteWriteResult.Found(rows[row.first()]!!) }
        override suspend fun update(config: SheetConfig, accessToken: String, rowNumber: Int, row: List<String>) = RemoteWriteResult.Found(rowNumber).also { updateCalls++ }
    }
}
