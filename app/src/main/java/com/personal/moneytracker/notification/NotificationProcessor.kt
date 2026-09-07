package com.personal.moneytracker.notification

import com.personal.moneytracker.data.local.entity.RawNotificationEvent
import com.personal.moneytracker.data.repository.ObservationRepository
import com.personal.moneytracker.data.repository.TransactionRepository
import com.personal.moneytracker.domain.parser.ParserRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationProcessor @Inject constructor(
    private val parserRegistry: ParserRegistry,
    private val observationRepository: ObservationRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend fun process(event: RawNotificationEvent) {
        observationRepository.record(event.id, parserRegistry.parse(event))
        observationRepository.findByRawEventId(event.id)?.let { transactionRepository.reconcileObservation(it.id) }
    }
}
