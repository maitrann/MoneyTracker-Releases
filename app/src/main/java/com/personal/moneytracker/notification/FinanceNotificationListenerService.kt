package com.personal.moneytracker.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.personal.moneytracker.data.local.entity.RawEventStatus
import com.personal.moneytracker.data.local.entity.RawNotificationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import com.personal.moneytracker.data.repository.RawEventRepository
import com.personal.moneytracker.data.repository.TrackedPackageRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FinanceNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Inject lateinit var rawEventRepository: RawEventRepository
    @Inject lateinit var trackedPackageRepository: TrackedPackageRepository
    @Inject lateinit var notificationProcessor: NotificationProcessor

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        scope.launch {
            if (sbn.packageName !in trackedPackageRepository.current()) return@launch

            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            val sensitive = SensitiveContentFilter.isSensitive(title, text, bigText, subText)
            val hash = if (sensitive) {
                NotificationFingerprint.create(sbn.packageName, sbn.key, null, null, null, null)
            } else {
                NotificationFingerprint.create(sbn.packageName, sbn.key, title, text, bigText, subText)
            }

            val event = RawNotificationEvent(
                    id = UUID.randomUUID().toString(),
                    packageName = sbn.packageName,
                    notificationKey = sbn.key,
                    notificationId = sbn.id,
                    postedAtEpochMs = sbn.postTime,
                    title = if (sensitive) null else title,
                    text = if (sensitive) null else text,
                    bigText = if (sensitive) null else bigText,
                    subText = if (sensitive) null else subText,
                    category = if (sensitive) null else sbn.notification.category,
                    channelId = if (sensitive) null else sbn.notification.channelId,
                    contentHash = hash,
                    captureStatus = if (sensitive) RawEventStatus.SENSITIVE_IGNORED else RawEventStatus.CAPTURED,
                    createdAtEpochMs = System.currentTimeMillis(),
                )
            if (rawEventRepository.save(event)) notificationProcessor.process(event)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
