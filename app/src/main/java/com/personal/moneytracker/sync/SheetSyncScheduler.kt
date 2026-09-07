package com.personal.moneytracker.sync

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SheetSyncScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val workManager get() = WorkManager.getInstance(context)
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun schedule() {
        val immediate = OneTimeWorkRequestBuilder<SheetSyncWorker>().setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        workManager.enqueueUniqueWork(IMMEDIATE, ExistingWorkPolicy.KEEP, immediate)
        val periodic = PeriodicWorkRequestBuilder<SheetSyncWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build()
        workManager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, periodic)
    }
    companion object { const val IMMEDIATE = "google-sheets-sync-now"; const val PERIODIC = "google-sheets-sync-periodic" }
}
