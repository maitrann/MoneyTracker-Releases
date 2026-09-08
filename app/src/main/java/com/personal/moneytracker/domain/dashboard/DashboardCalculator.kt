package com.personal.moneytracker.domain.dashboard

import com.personal.moneytracker.data.local.entity.CanonicalTransaction
import com.personal.moneytracker.domain.model.SyncState
import com.personal.moneytracker.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * A single category's aggregated totals for the currently selected month.
 *
 * [totalVnd] only ever sums EXPENSE amounts. Income and internal transfers are not mixed into a
 * category breakdown to avoid a misleading "spending by category" number.
 */
data class CategoryTotal(
    val categoryId: String,
    val categoryName: String,
    val totalVnd: Long,
    val count: Int,
)

/**
 * Everything the Dashboard screen needs to render for one selected month + optional search filter.
 */
data class DashboardSummary(
    val month: YearMonth,
    val incomeVnd: Long,
    val expenseVnd: Long,
    val netCashFlowVnd: Long,
    val categoryTotals: List<CategoryTotal>,
    val recentTransactions: List<CanonicalTransaction>,
    val pendingSyncCount: Int,
    val transactionCountInMonth: Int,
)

/**
 * Pure Kotlin aggregation logic for the Dashboard (Phase 7).
 *
 * Deliberately has no Android/Room dependency so it can be unit tested without Robolectric.
 *
 * Accounting rule (non-negotiable, see AGENTS.md / PLANS.md Phase 7):
 * [TransactionType.INTERNAL_TRANSFER] transactions are excluded from income, expense, net cash
 * flow, and category totals. They may still appear in [DashboardSummary.recentTransactions] so the
 * user can see where their money moved, they are just not counted as spending or earning.
 *
 * Pending-sync count is computed across ALL transactions passed in, not scoped to the selected
 * month: "N pending" is meant to answer "is anything waiting to reach Google Sheets right now",
 * which is a global sync-health question, not a per-month one.
 */
object DashboardCalculator {

    private val COUNTED_TYPES = setOf(TransactionType.EXPENSE, TransactionType.INCOME)
    private val PENDING_STATES = setOf(
        SyncState.NOT_SYNCED,
        SyncState.QUEUED,
        SyncState.SYNCING,
        SyncState.FAILED,
        SyncState.AUTH_REQUIRED,
    )

    fun summarize(
        allTransactions: List<CanonicalTransaction>,
        month: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault(),
        searchQuery: String? = null,
        categoryNames: Map<String, String> = emptyMap(),
        recentLimit: Int = 20,
    ): DashboardSummary {
        val monthTransactions = allTransactions.filter { it.occurredInMonth(month, zoneId) }
        val query = searchQuery?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()
        val filtered = if (query == null) monthTransactions else monthTransactions.filter { it.matchesQuery(query) }

        val income = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amountVnd }
        val expense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountVnd }

        val categoryTotals = filtered
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (categoryId, transactions) ->
                CategoryTotal(
                    categoryId = categoryId,
                    categoryName = categoryNames[categoryId] ?: categoryId,
                    totalVnd = transactions.sumOf { it.amountVnd },
                    count = transactions.size,
                )
            }
            .sortedByDescending { it.totalVnd }

        val recent = filtered.sortedByDescending { it.occurredAtEpochMs }.take(recentLimit)

        val pendingSyncCount = allTransactions.count { it.syncState in PENDING_STATES }

        return DashboardSummary(
            month = month,
            incomeVnd = income,
            expenseVnd = expense,
            netCashFlowVnd = income - expense,
            categoryTotals = categoryTotals,
            recentTransactions = recent,
            pendingSyncCount = pendingSyncCount,
            transactionCountInMonth = filtered.size,
        )
    }

    private fun CanonicalTransaction.occurredInMonth(month: YearMonth, zoneId: ZoneId): Boolean {
        val occurredMonth = YearMonth.from(Instant.ofEpochMilli(occurredAtEpochMs).atZone(zoneId))
        return occurredMonth == month
    }

    private fun CanonicalTransaction.matchesQuery(query: String): Boolean {
        return listOf(merchant, service, categoryId, subcategory, description, note, fundingSource, paymentChannel)
            .any { it?.lowercase()?.contains(query) == true }
    }
}
