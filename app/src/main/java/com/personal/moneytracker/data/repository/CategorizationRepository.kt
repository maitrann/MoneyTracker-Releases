package com.personal.moneytracker.data.repository

import com.personal.moneytracker.data.local.AppDatabase
import com.personal.moneytracker.data.local.entity.CanonicalTransaction
import com.personal.moneytracker.data.local.entity.CategorizationRule
import com.personal.moneytracker.data.local.entity.Category
import com.personal.moneytracker.domain.categorization.CategorizationDecision
import com.personal.moneytracker.domain.categorization.CategorizationEngine
import com.personal.moneytracker.domain.model.RuleField
import com.personal.moneytracker.domain.model.RuleOperator
import com.personal.moneytracker.domain.model.RuleSource
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategorizationRepository @Inject constructor(
    private val database: AppDatabase,
    private val engine: CategorizationEngine,
) {
    fun observeRules(): Flow<List<CategorizationRule>> = database.categorizationDao().observeRules()
    fun observeCategories(): Flow<List<Category>> = database.categorizationDao().observeCategories()
    suspend fun initialize() = ensureSystemData()

    suspend fun categorize(transaction: CanonicalTransaction): CanonicalTransaction {
        ensureSystemData()
        val decision = engine.categorize(transaction, database.categorizationDao().activeRules())
        return transaction.copy(categoryId = decision.categoryId, subcategory = decision.subcategory, classificationConfidence = decision.confidence)
    }

    suspend fun createUserRule(field: RuleField, operator: RuleOperator, pattern: String, categoryId: String, now: Long = System.currentTimeMillis()) {
        database.categorizationDao().insertRule(CategorizationRule(UUID.randomUUID().toString(), 1000, true, field, operator, pattern, categoryId, null, null, RuleSource.USER, now, now))
    }

    suspend fun setRuleEnabled(rule: CategorizationRule, enabled: Boolean) = database.categorizationDao().updateRule(rule.copy(enabled = enabled, updatedAtEpochMs = System.currentTimeMillis()))

    private suspend fun ensureSystemData() {
        database.categorizationDao().insertCategories(SYSTEM_CATEGORIES)
        database.categorizationDao().insertRules(SYSTEM_RULES)
    }

    private companion object {
        val SYSTEM_CATEGORIES = listOf(
            "food" to "Food", "transportation" to "Transportation", "shopping" to "Shopping/Groceries", "other" to "Other/Delivery", "unclassified" to "Unclassified",
        ).mapIndexed { index, (id, name) -> Category(id, name, null, true, index) }
        val SYSTEM_RULES = listOf(
            "system-grab-food" to ("GrabFood" to "food"),
            "system-grab-bike" to ("GrabBike" to "transportation"),
            "system-grab-car" to ("GrabCar" to "transportation"),
            "system-grab-mart" to ("GrabMart" to "shopping"),
            "system-grab-express" to ("GrabExpress" to "other"),
        ).mapIndexed { index, (id, value) -> CategorizationRule(id, 100 - index, true, RuleField.SERVICE, RuleOperator.EQUALS, value.first, value.second, null, null, RuleSource.SYSTEM, 0, 0) }
    }
}
