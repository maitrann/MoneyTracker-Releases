package com.personal.moneytracker.data.repository

import androidx.room.withTransaction
import com.personal.moneytracker.data.local.AppDatabase
import com.personal.moneytracker.data.local.dao.TransactionWithObservations
import com.personal.moneytracker.data.local.entity.CanonicalTransaction
import com.personal.moneytracker.data.local.entity.ParsedObservation
import com.personal.moneytracker.data.local.entity.TransactionObservationLink
import com.personal.moneytracker.domain.matching.MatchScorer
import com.personal.moneytracker.domain.model.ObservationRole
import com.personal.moneytracker.domain.model.TransactionType
import com.personal.moneytracker.domain.model.SyncState
import com.personal.moneytracker.domain.parser.DirectionHint
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val database: AppDatabase,
    private val scorer: MatchScorer,
    private val categorizationRepository: CategorizationRepository,
) {
    fun observeAll(): Flow<List<CanonicalTransaction>> = database.transactionDao().observeAll()
    suspend fun detail(id: String): TransactionWithObservations? = database.transactionDao().detail(id)

    suspend fun correctCategory(id: String, categoryId: String, locked: Boolean, now: Long = System.currentTimeMillis()) = database.withTransaction {
        val transaction = database.transactionDao().findById(id) ?: return@withTransaction
        database.transactionDao().update(transaction.copy(categoryId = categoryId, userEdited = true, userCategoryLocked = locked, syncState = if (transaction.syncState == SyncState.SYNCED) SyncState.QUEUED else transaction.syncState, updatedAtEpochMs = now))
    }

    suspend fun createRuleFromCorrection(id: String, categoryId: String) = database.withTransaction {
        val transaction = database.transactionDao().findById(id) ?: return@withTransaction
        val fieldAndPattern = transaction.service?.takeIf { it.isNotBlank() }?.let { com.personal.moneytracker.domain.model.RuleField.SERVICE to it }
            ?: transaction.merchant?.takeIf { it.isNotBlank() }?.let { com.personal.moneytracker.domain.model.RuleField.MERCHANT to it }
            ?: return@withTransaction
        categorizationRepository.createUserRule(fieldAndPattern.first, com.personal.moneytracker.domain.model.RuleOperator.EQUALS, fieldAndPattern.second, categoryId)
    }

    suspend fun reconcileObservation(observationId: String, now: Long = System.currentTimeMillis()): String? = database.withTransaction {
        val observation = database.parsedObservationDao().findById(observationId) ?: return@withTransaction null
        database.transactionDao().linkForObservation(observationId)?.transactionId ?: run {
            val candidates = database.transactionDao().all().map { transaction ->
                transaction to scorer.score(observation, transaction, database.transactionDao().observationsForTransaction(transaction.id))
            }
            val match = candidates.maxByOrNull { it.second.score }?.takeIf { it.second.isConservativeMatch }
            if (match != null) {
                database.transactionDao().insertLink(
                    TransactionObservationLink(match.first.id, observationId, ObservationRole.SUPPORTING, match.second.score, match.second.reasons.joinToString("; ")),
                )
                enrich(match.first, database.transactionDao().observationsForTransaction(match.first.id) + observation, match.second, now)
                match.first.id
            } else createTransaction(observation, now)
        }
    }

    private suspend fun createTransaction(observation: ParsedObservation, now: Long): String {
        val id = UUID.randomUUID().toString()
        val initial = CanonicalTransaction(
                id = id, occurredAtEpochMs = observation.eventAtEpochMs, amountVnd = observation.amountVnd,
                type = typeFor(observation), merchant = observation.merchant, service = observation.service,
                subcategory = null, fundingSource = observation.fundingSourceHint,
                paymentChannel = observation.paymentChannelHint, description = observation.descriptionNormalized,
                note = null, classificationConfidence = 0.0, matchConfidence = observation.confidence,
                reconciliationReadyAtEpochMs = now + RECONCILIATION_DELAY_MS,
                createdAtEpochMs = now, updatedAtEpochMs = now,
            )
        database.transactionDao().insert(categorizationRepository.categorize(initial))
        database.transactionDao().insertLink(TransactionObservationLink(id, observation.id, ObservationRole.PRIMARY, 1.0, "Created from parsed observation"))
        return id
    }

    private fun typeFor(observation: ParsedObservation): TransactionType = when (observation.directionHint) {
        DirectionHint.DEBIT -> TransactionType.EXPENSE
        DirectionHint.CREDIT -> TransactionType.INCOME
        DirectionHint.TRANSFER -> if (observation.descriptionNormalized?.contains("internal transfer", ignoreCase = true) == true) TransactionType.INTERNAL_TRANSFER else TransactionType.UNKNOWN
        DirectionHint.UNKNOWN -> TransactionType.UNKNOWN
    }

    private suspend fun enrich(
        existing: CanonicalTransaction,
        observations: List<ParsedObservation>,
        score: com.personal.moneytracker.domain.matching.MatchScore,
        now: Long,
    ) {
        if (existing.userEdited) return
        val bank = observations.firstOrNull { it.sourceApp.name in setOf("TECHCOMBANK", "TIMO") }
        val momo = observations.firstOrNull { it.sourceApp.name == "MOMO" }
        val detail = observations.firstOrNull { !it.service.isNullOrBlank() }
        val merchant = detail?.merchant ?: momo?.merchant ?: existing.merchant
        val service = detail?.service ?: existing.service
        val enriched = existing.copy(
                type = if (existing.type == TransactionType.UNKNOWN && observations.any { it.directionHint == DirectionHint.DEBIT }) TransactionType.EXPENSE else existing.type,
                fundingSource = bank?.fundingSourceHint ?: existing.fundingSource,
                paymentChannel = momo?.paymentChannelHint ?: if (momo != null) "MoMo" else existing.paymentChannel,
                merchant = merchant,
                service = service,
                matchConfidence = maxOf(existing.matchConfidence, (score.score / 100.0).coerceAtMost(1.0)),
                reconciliationReadyAtEpochMs = now + RECONCILIATION_DELAY_MS,
                syncState = if (existing.syncState == SyncState.SYNCED) SyncState.QUEUED else existing.syncState,
                updatedAtEpochMs = now,
            )
        database.transactionDao().update(categorizationRepository.categorize(enriched))
    }

    private companion object { const val RECONCILIATION_DELAY_MS = 120_000L }
}
