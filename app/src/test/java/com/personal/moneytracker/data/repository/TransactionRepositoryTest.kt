package com.personal.moneytracker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.personal.moneytracker.data.local.AppDatabase
import com.personal.moneytracker.data.local.entity.ParsedObservation
import com.personal.moneytracker.data.local.entity.RawEventStatus
import com.personal.moneytracker.data.local.entity.RawNotificationEvent
import com.personal.moneytracker.domain.matching.MatchScorer
import com.personal.moneytracker.domain.categorization.CategorizationEngine
import com.personal.moneytracker.domain.model.TransactionType
import com.personal.moneytracker.domain.parser.DirectionHint
import com.personal.moneytracker.domain.parser.SourceApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransactionRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var categorizationRepository: CategorizationRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        categorizationRepository = CategorizationRepository(database, CategorizationEngine())
        repository = TransactionRepository(database, MatchScorer(), categorizationRepository)
    }

    @After fun tearDown() = database.close()

    @Test fun oneObservationCreatesOneCanonicalTransaction() = runTest {
        insertObservation("one")
        val id = repository.reconcileObservation("one", 2_000)!!
        val detail = repository.detail(id)!!
        assertEquals(1, repository.observeAll().first().size)
        assertEquals(1, detail.links.size)
        assertEquals(TransactionType.EXPENSE, detail.transaction.type)
    }

    @Test fun reconcileRetryDoesNotCreateDuplicateTransaction() = runTest {
        insertObservation("one")
        val first = repository.reconcileObservation("one", 2_000)!!
        val second = repository.reconcileObservation("one", 3_000)!!
        assertEquals(first, second)
        assertEquals(1, repository.observeAll().first().size)
    }

    @Test fun unrelatedSameAmountObservationsRemainSeparate() = runTest {
        insertObservation("coffee", merchant = "Coffee", occurredAt = 1_000)
        insertObservation("store", merchant = "Store", occurredAt = 1_500)
        val first = repository.reconcileObservation("coffee")!!
        val second = repository.reconcileObservation("store")!!
        assertNotEquals(first, second)
        assertEquals(2, repository.observeAll().first().size)
    }

    @Test fun ambiguousObservationsAreNotForceMerged() = runTest {
        insertObservation("first", merchant = null, occurredAt = 1_000)
        insertObservation("second", merchant = null, occurredAt = 1_100)
        repository.reconcileObservation("first")
        repository.reconcileObservation("second")
        assertEquals(2, repository.observeAll().first().size)
    }

    @Test fun explicitInternalTransferIsDistinctType() = runTest {
        insertObservation("transfer", direction = DirectionHint.TRANSFER, description = "Internal transfer between own accounts")
        val id = repository.reconcileObservation("transfer")!!
        assertEquals(TransactionType.INTERNAL_TRANSFER, repository.detail(id)?.transaction?.type)
    }

    @Test fun matchingReferenceCanLinkAdditionalObservationWithExplanation() = runTest {
        insertObservation("first", reference = "fixture-reference", occurredAt = 1_000)
        insertObservation("second", reference = "fixture-reference", occurredAt = 1_100)
        val id = repository.reconcileObservation("first")!!
        assertEquals(id, repository.reconcileObservation("second"))
        val detail = repository.detail(id)!!
        assertEquals(2, detail.links.size)
        assertEquals(1, repository.observeAll().first().size)
        assertEquals(true, detail.links.any { it.explanation.contains("reference hint matches") })
    }

    @Test fun techcombankMomoGrabFoodChainEnrichesOneFoodExpense() = runTest {
        insertObservation("bank", sourceApp = SourceApp.TECHCOMBANK, merchant = null, occurredAt = 1_000, funding = "Techcombank")
        insertObservation("momo", sourceApp = SourceApp.MOMO, merchant = "Grab", occurredAt = 1_030, channel = "MoMo")
        insertObservation("grab", sourceApp = SourceApp.GRAB, merchant = "Grab", service = "GrabFood", occurredAt = 1_060)
        val id = repository.reconcileObservation("bank")!!
        assertEquals(id, repository.reconcileObservation("momo"))
        assertEquals(id, repository.reconcileObservation("grab"))
        val transaction = repository.detail(id)!!.transaction
        assertEquals(1, repository.observeAll().first().size)
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals("Techcombank", transaction.fundingSource)
        assertEquals("MoMo", transaction.paymentChannel)
        assertEquals("GrabFood", transaction.service)
        assertEquals("food", transaction.categoryId)
    }

    @Test fun momoGrabBikeChainEnrichesOneTransportationExpense() = runTest {
        insertObservation("momo", sourceApp = SourceApp.MOMO, merchant = "Grab", occurredAt = 1_000, channel = "MoMo")
        insertObservation("grab", sourceApp = SourceApp.GRAB, merchant = "Grab", service = "GrabBike", occurredAt = 1_040)
        val id = repository.reconcileObservation("momo")!!
        assertEquals(id, repository.reconcileObservation("grab"))
        val transaction = repository.detail(id)!!.transaction
        assertEquals(1, repository.observeAll().first().size)
        assertEquals("GrabBike", transaction.service)
        assertEquals("transportation", transaction.categoryId)
    }

    @Test fun manualCategoryLockSurvivesLaterReconciliation() = runTest {
        insertObservation("first", merchant = "Coffee", reference = "same")
        insertObservation("second", merchant = "Coffee", reference = "same", occurredAt = 1_100)
        val id = repository.reconcileObservation("first")!!
        repository.correctCategory(id, "health", locked = true)
        repository.reconcileObservation("second")
        assertEquals("health", repository.detail(id)?.transaction?.categoryId)
        assertEquals(true, repository.detail(id)?.transaction?.userCategoryLocked)
    }

    @Test fun createRuleFromCorrectionClassifiesFutureMatchingMerchant() = runTest {
        insertObservation("first", merchant = "Coffee")
        val first = repository.reconcileObservation("first")!!
        repository.correctCategory(first, "entertainment", locked = false)
        repository.createRuleFromCorrection(first, "entertainment")
        insertObservation("later", merchant = "Coffee", occurredAt = 10_000)
        val later = repository.reconcileObservation("later")!!
        assertEquals("entertainment", repository.detail(later)?.transaction?.categoryId)
    }

    private suspend fun insertObservation(
        id: String,
        merchant: String? = "Merchant",
        occurredAt: Long = 1_000,
        direction: DirectionHint = DirectionHint.DEBIT,
        description: String = "synthetic",
        reference: String? = null,
        sourceApp: SourceApp = SourceApp.GENERIC,
        funding: String? = null,
        channel: String? = null,
        service: String? = null,
    ) {
        database.rawEventDao().insert(
            RawNotificationEvent(
                id = "raw-$id", packageName = "fixture.app", notificationKey = id, notificationId = 1,
                postedAtEpochMs = occurredAt, title = null, text = null, bigText = null, subText = null,
                category = null, channelId = null, contentHash = "hash-$id", captureStatus = RawEventStatus.PARSED,
                createdAtEpochMs = occurredAt,
            ),
        )
        database.parsedObservationDao().insert(
            ParsedObservation(
                id = id, rawEventId = "raw-$id", sourceApp = sourceApp, amountVnd = 50_000,
                directionHint = direction, eventAtEpochMs = occurredAt, merchant = merchant,
                service = service, fundingSourceHint = funding, paymentChannelHint = channel, referenceHint = reference,
                descriptionNormalized = description, parserName = "fixture", parserVersion = 1,
                confidence = 0.5, createdAtEpochMs = occurredAt,
            ),
        )
    }
}
