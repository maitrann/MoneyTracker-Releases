package com.personal.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.personal.moneytracker.domain.model.SyncState
import com.personal.moneytracker.domain.model.TransactionType

@Entity(tableName = "canonical_transactions")
data class CanonicalTransaction(
    @PrimaryKey val id: String,
    val occurredAtEpochMs: Long,
    val amountVnd: Long,
    val currency: String = "VND",
    val type: TransactionType,
    val merchant: String?,
    val service: String?,
    val categoryId: String = "unclassified",
    val subcategory: String?,
    val fundingSource: String?,
    val paymentChannel: String?,
    val description: String?,
    val note: String?,
    val classificationConfidence: Double,
    val matchConfidence: Double,
    val userEdited: Boolean = false,
    val userCategoryLocked: Boolean = false,
    val syncState: SyncState = SyncState.NOT_SYNCED,
    val reconciliationReadyAtEpochMs: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
