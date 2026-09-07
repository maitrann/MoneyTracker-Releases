package com.personal.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.personal.moneytracker.domain.parser.DirectionHint
import com.personal.moneytracker.domain.parser.SourceApp

@Entity(
    tableName = "parsed_observations",
    foreignKeys = [ForeignKey(
        entity = RawNotificationEvent::class,
        parentColumns = ["id"],
        childColumns = ["rawEventId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["rawEventId"], unique = true)],
)
data class ParsedObservation(
    @PrimaryKey val id: String,
    val rawEventId: String,
    val sourceApp: SourceApp,
    val amountVnd: Long,
    val directionHint: DirectionHint,
    val eventAtEpochMs: Long,
    val merchant: String?,
    val service: String?,
    val fundingSourceHint: String?,
    val paymentChannelHint: String?,
    val referenceHint: String?,
    val descriptionNormalized: String?,
    val parserName: String,
    val parserVersion: Int,
    val confidence: Double,
    val createdAtEpochMs: Long,
)
