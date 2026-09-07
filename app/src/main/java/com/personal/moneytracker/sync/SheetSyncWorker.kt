package com.personal.moneytracker.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SheetSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val coordinator: SheetSyncCoordinator,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = when (coordinator.syncReady()) {
        SyncRunResult.RETRY -> Result.retry()
        SyncRunResult.SUCCESS, SyncRunResult.AUTH_REQUIRED, SyncRunResult.NOT_CONFIGURED -> Result.success()
    }
}
