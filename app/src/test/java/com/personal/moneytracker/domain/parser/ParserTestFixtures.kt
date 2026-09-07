package com.personal.moneytracker.domain.parser

import com.personal.moneytracker.data.local.entity.RawEventStatus
import com.personal.moneytracker.data.local.entity.RawNotificationEvent

fun rawEvent(
    packageName: String = "synthetic.finance",
    title: String? = "Synthetic notification",
    text: String? = "Payment -125.000đ",
    status: RawEventStatus = RawEventStatus.CAPTURED,
) = RawNotificationEvent(
    id = "raw-1", packageName = packageName, notificationKey = "key", notificationId = 1,
    postedAtEpochMs = 1000, title = title, text = text, bigText = null, subText = null,
    category = null, channelId = null, contentHash = "hash", captureStatus = status, createdAtEpochMs = 1000,
)
