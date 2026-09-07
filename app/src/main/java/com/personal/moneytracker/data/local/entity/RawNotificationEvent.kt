package com.personal.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RawEventStatus { CAPTURED, PARSED, IGNORED, UNPARSED, SENSITIVE_IGNORED, ERROR }

@Entity(
    tableName = "raw_notification_events",
    indices = [Index(value = ["contentHash"], unique = true)],
)
data class RawNotificationEvent(
    @PrimaryKey val id: String,
    val packageName: String,
    val notificationKey: String?,
    val notificationId: Int?,
    val postedAtEpochMs: Long,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val category: String?,
    val channelId: String?,
    val contentHash: String,
    val captureStatus: RawEventStatus,
    val createdAtEpochMs: Long,
    val parserName: String? = null,
    val processingReason: String? = null,
)
