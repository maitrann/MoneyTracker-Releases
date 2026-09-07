package com.personal.moneytracker.data.repository

import androidx.room.withTransaction
import com.personal.moneytracker.data.local.AppDatabase
import com.personal.moneytracker.data.local.entity.ParsedObservation
import com.personal.moneytracker.data.local.entity.RawEventStatus
import com.personal.moneytracker.domain.parser.ParseResult
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class ObservationRepository @Inject constructor(private val database: AppDatabase) {
    fun observeAll(): Flow<List<ParsedObservation>> = database.parsedObservationDao().observeAll()
    suspend fun findByRawEventId(rawEventId: String): ParsedObservation? = database.parsedObservationDao().findByRawEventId(rawEventId)

    suspend fun record(rawEventId: String, result: ParseResult, now: Long = System.currentTimeMillis()) {
        database.withTransaction {
            when (result) {
                is ParseResult.Parsed -> {
                    val draft = result.observation
                    database.parsedObservationDao().insert(
                        ParsedObservation(
                            id = UUID.randomUUID().toString(), rawEventId = rawEventId,
                            sourceApp = draft.sourceApp, amountVnd = draft.amountVnd,
                            directionHint = draft.directionHint, eventAtEpochMs = draft.eventAtEpochMs,
                            merchant = draft.merchant, service = draft.service,
                            fundingSourceHint = draft.fundingSourceHint,
                            paymentChannelHint = draft.paymentChannelHint,
                            referenceHint = draft.referenceHint,
                            descriptionNormalized = draft.descriptionNormalized,
                            parserName = draft.parserName, parserVersion = draft.parserVersion,
                            confidence = draft.confidence, createdAtEpochMs = now,
                        ),
                    )
                    database.rawEventDao().updateProcessing(rawEventId, RawEventStatus.PARSED, draft.parserName, null)
                }
                is ParseResult.Ignored -> database.rawEventDao().updateProcessing(rawEventId, RawEventStatus.IGNORED, result.parserName, result.reason)
                is ParseResult.Unparsed -> database.rawEventDao().updateProcessing(rawEventId, RawEventStatus.UNPARSED, result.parserName, result.reason)
                is ParseResult.SensitiveIgnored -> database.rawEventDao().updateProcessing(rawEventId, RawEventStatus.SENSITIVE_IGNORED, null, result.reason)
            }
        }
    }
}
