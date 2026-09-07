package com.personal.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** Local bookkeeping only. The remote row is advisory; transactionId is always re-checked remotely. */
@Entity(
    tableName = "sheet_sync_records",
    foreignKeys = [ForeignKey(
        entity = CanonicalTransaction::class,
        parentColumns = ["id"],
        childColumns = ["transactionId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class SheetSyncRecord(
    @PrimaryKey
    val transactionId: String,
    val spreadsheetId: String,
    val sheetName: String,
    val remoteRow: Int?,
    val lastSyncedUpdatedAtEpochMs: Long,
    val lastAttemptAtEpochMs: Long? = null,
    val lastErrorCode: String? = null,
)
