package com.personal.moneytracker.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.moneytracker.data.repository.CategorizationRepository
import com.personal.moneytracker.data.repository.TransactionRepository
import com.personal.moneytracker.domain.dashboard.DashboardCalculator
import com.personal.moneytracker.domain.dashboard.DashboardSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

/**
 * Drives the Phase 7 Dashboard screen.
 *
 * All aggregation math lives in [DashboardCalculator] (plain Kotlin, unit-testable without
 * Android). This ViewModel is only responsible for: observing Room via the repositories, holding
 * the currently selected month + search text as UI state, and combining them into a
 * [DashboardSummary] the Composable can render directly.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categorizationRepository: CategorizationRepository,
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val summary: StateFlow<DashboardSummary> = combine(
        transactionRepository.observeAll(),
        categorizationRepository.observeCategories(),
        _selectedMonth,
        _searchQuery,
    ) { transactions, categories, month, query ->
        DashboardCalculator.summarize(
            allTransactions = transactions,
            month = month,
            searchQuery = query,
            categoryNames = categories.associate { it.id to it.displayName },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardCalculator.summarize(emptyList(), YearMonth.now()),
    )

    fun selectPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun selectNextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
