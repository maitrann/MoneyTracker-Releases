package com.personal.moneytracker.domain.dashboard

import com.personal.moneytracker.data.local.entity.CanonicalTransaction
import com.personal.moneytracker.domain.model.SyncState
import com.personal.moneytracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Pure-Kotlin tests for Phase 7 dashboard aggregation. No Robolectric/Room needed since
 * [DashboardCalculator] takes a plain list of already-loaded [CanonicalTransaction] rows.
 */
class DashboardCalculatorTest {

    private val zone = ZoneOffset.UTC
    private val august = YearMonth.of(2026, 8)
    private val september = YearMonth.of(2026, 9)

    private fun epochMsFor(month: YearMonth, day: Int = 15): Long =
        month.atDay(day).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun transaction(
        id: String,
        type: TransactionType,
        amountVnd: Long,
        categoryId: String = "unclassified",
        month: YearMonth = august,
        syncState: SyncState = SyncState.SYNCED,
        merchant: String? = null,
        note: String? = null,
    ) = CanonicalTransaction(
        id = id,
        occurredAtEpochMs = epochMsFor(month),
        amountVnd = amountVnd,
        type = type,
        merchant = merchant,
        service = null,
        categoryId = categoryId,
        subcategory = null,
        fundingSource = null,
        paymentChannel = null,
        description = null,
        note = note,
        classificationConfidence = 1.0,
        matchConfidence = 1.0,
        syncState = syncState,
        reconciliationReadyAtEpochMs = 0,
        createdAtEpochMs = 0,
        updatedAtEpochMs = 0,
    )

    @Test fun incomeAndExpenseTotalsSumOnlyMatchingTypesInSelectedMonth() {
        val transactions = listOf(
            transaction("salary", TransactionType.INCOME, 10_000_000),
            transaction("rent", TransactionType.EXPENSE, 3_000_000),
            transaction("food", TransactionType.EXPENSE, 500_000),
            transaction("other-month-income", TransactionType.INCOME, 999_000, month = september),
        )

        val summary = DashboardCalculator.summarize(transactions, august, zone)

        assertEquals(10_000_000L, summary.incomeVnd)
        assertEquals(3_500_000L, summary.expenseVnd)
        assertEquals(6_500_000L, summary.netCashFlowVnd)
        assertEquals(3, summary.transactionCountInMonth)
    }

    @Test fun internalTransferIsExcludedFromIncomeExpenseAndNet() {
        val transactions = listOf(
            transaction("salary", TransactionType.INCOME, 5_000_000),
            transaction("moveToSavings", TransactionType.INTERNAL_TRANSFER, 2_000_000),
        )

        val summary = DashboardCalculator.summarize(transactions, august, zone)

        assertEquals(5_000_000L, summary.incomeVnd)
        assertEquals(0L, summary.expenseVnd)
        assertEquals(5_000_000L, summary.netCashFlowVnd)
        // still visible in the month bucket (recent list), just not counted as income/expense
        assertEquals(2, summary.transactionCountInMonth)
        assertTrue(summary.recentTransactions.any { it.id == "moveToSavings" })
    }

    @Test fun internalTransferIsExcludedFromCategoryTotals() {
        val transactions = listOf(
            transaction("food", TransactionType.EXPENSE, 200_000, categoryId = "food"),
            transaction("transfer", TransactionType.INTERNAL_TRANSFER, 1_000_000, categoryId = "food"),
        )

        val summary = DashboardCalculator.summarize(transactions, august, zone)

        assertEquals(1, summary.categoryTotals.size)
        assertEquals(200_000L, summary.categoryTotals.first().totalVnd)
    }

    @Test fun categoryTotalsGroupAndSortDescendingBySpend() {
        val transactions = listOf(
            transaction("food1", TransactionType.EXPENSE, 100_000, categoryId = "food"),
            transaction("food2", TransactionType.EXPENSE, 150_000, categoryId = "food"),
            transaction("transport1", TransactionType.EXPENSE, 400_000, categoryId = "transportation"),
        )

        val summary = DashboardCalculator.summarize(transactions, august, zone)

        assertEquals(2, summary.categoryTotals.size)
        assertEquals("transportation", summary.categoryTotals[0].categoryId)
        assertEquals(400_000L, summary.categoryTotals[0].totalVnd)
        assertEquals("food", summary.categoryTotals[1].categoryId)
        assertEquals(250_000L, summary.categoryTotals[1].totalVnd)
        assertEquals(2, summary.categoryTotals[1].count)
    }

    @Test fun transactionsOutsideSelectedMonthAreExcluded() {
        val transactions = listOf(
            transaction("aug", TransactionType.EXPENSE, 100_000, month = august),
            transaction("sep", TransactionType.EXPENSE, 999_000, month = september),
        )

        val summary = DashboardCalculator.summarize(transactions, august, zone)

        assertEquals(100_000L, summary.expenseVnd)
        assertEquals(1, summary.transactionCountInMonth)
    }

    @Test fun pendingSyncCountIsGlobalNotScopedToSelectedMonth() {
        val transactions = listOf(
            transaction("aug-pending", TransactionType.EXPENSE, 100_000, month = august, syncState = SyncState.QUEUED),
            transaction("sep-pending", TransactionType.EXPENSE, 200_000, month = september, syncState = SyncState.NOT_SYNCED),
            transaction("aug-synced", TransactionType.EXPENSE, 300_000, month = august, syncState = SyncState.SYNCED),
            transaction("aug-auth-required", TransactionType.EXPENSE, 400_000, month = august, syncState = SyncState.AUTH_REQUIRED),
        )

        val summary = DashboardCalculator.summarize(transactions, august, zone)

        assertEquals(3, summary.pendingSyncCount)
    }

    @Test fun searchFilterMatchesMerchantCategoryOrNoteCaseInsensitively() {
        val transactions = listOf(
            transaction("grab", TransactionType.EXPENSE, 125_000, categoryId = "food", merchant = "GrabFood"),
            transaction("rent", TransactionType.EXPENSE, 3_000_000, categoryId = "housing", merchant = "Landlord", note = "Monthly rent"),
        )

        val grabOnly = DashboardCalculator.summarize(transactions, august, zone, searchQuery = "grabfood")
        assertEquals(1, grabOnly.transactionCountInMonth)
        assertEquals("grab", grabOnly.recentTransactions.first().id)

        val rentByNote = DashboardCalculator.summarize(transactions, august, zone, searchQuery = "RENT")
        assertEquals(1, rentByNote.transactionCountInMonth)
        assertEquals("rent", rentByNote.recentTransactions.first().id)

        val noMatch = DashboardCalculator.summarize(transactions, august, zone, searchQuery = "nonexistent")
        assertTrue(noMatch.recentTransactions.isEmpty())
        assertFalse(noMatch.categoryTotals.isNotEmpty())
    }

    @Test fun categoryNamesMapIsUsedForDisplayNameWhenProvided() {
        val transactions = listOf(transaction("food1", TransactionType.EXPENSE, 100_000, categoryId = "food"))

        val summary = DashboardCalculator.summarize(
            transactions, august, zone, categoryNames = mapOf("food" to "Food & Drinks"),
        )

        assertEquals("Food & Drinks", summary.categoryTotals.first().categoryName)
    }

    @Test fun recentTransactionsAreSortedNewestFirstAndLimited() {
        val transactions = (1..25).map { day ->
            transaction("t$day", TransactionType.EXPENSE, 10_000L * day, month = august)
                .copy(occurredAtEpochMs = epochMsFor(august, day = (day % 28) + 1))
        }

        val summary = DashboardCalculator.summarize(transactions, august, zone, recentLimit = 5)

        assertEquals(5, summary.recentTransactions.size)
        val sortedDesc = summary.recentTransactions.map { it.occurredAtEpochMs }
        assertEquals(sortedDesc.sortedDescending(), sortedDesc)
    }
}
