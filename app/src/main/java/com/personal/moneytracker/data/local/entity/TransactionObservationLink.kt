package com.personal.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.personal.moneytracker.domain.model.ObservationRole

@Entity(
    tableName = "transaction_observation_links",
    primaryKeys = ["transactionId", "observationId"],
    foreignKeys = [
        ForeignKey(entity = CanonicalTransaction::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ParsedObservation::class, parentColumns = ["id"], childColumns = ["observationId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("observationId", unique = true)],
)
data class TransactionObservationLink(
    val transactionId: String,
    val observationId: String,
    val linkRole: ObservationRole,
    val matchScore: Double,
    val explanation: String,
)
