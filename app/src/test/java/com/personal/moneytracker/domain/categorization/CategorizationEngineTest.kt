package com.personal.moneytracker.domain.categorization

import com.personal.moneytracker.data.local.entity.CanonicalTransaction
import com.personal.moneytracker.data.local.entity.CategorizationRule
import com.personal.moneytracker.domain.model.RuleField
import com.personal.moneytracker.domain.model.RuleOperator
import com.personal.moneytracker.domain.model.RuleSource
import com.personal.moneytracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class CategorizationEngineTest {
    private val engine = CategorizationEngine()
    private fun transaction(service: String? = "GrabFood", merchant: String? = "Grab", locked: Boolean = false, category: String = "unclassified") = CanonicalTransaction(
        id = "t", occurredAtEpochMs = 0, amountVnd = 1, type = TransactionType.EXPENSE,
        merchant = merchant, service = service, categoryId = category, subcategory = null,
        fundingSource = null, paymentChannel = null, description = null, note = null,
        classificationConfidence = 0.0, matchConfidence = 0.0, userCategoryLocked = locked,
        reconciliationReadyAtEpochMs = 0, createdAtEpochMs = 0, updatedAtEpochMs = 0,
    )
    private fun rule(id: String, source: RuleSource, field: RuleField, pattern: String, category: String, priority: Int = 1) = CategorizationRule(
        id, priority, true, field, RuleOperator.EQUALS, pattern, category, null, null, source, 0, 0,
    )

    @Test fun userLockHasAbsolutePrecedence() {
        val decision = engine.categorize(transaction(locked = true, category = "health"), listOf(rule("system", RuleSource.SYSTEM, RuleField.SERVICE, "GrabFood", "food")))
        assertEquals("health", decision.categoryId)
    }

    @Test fun userRuleBeatsSystemServiceAndMerchant() {
        val decision = engine.categorize(transaction(), listOf(
            rule("merchant", RuleSource.SYSTEM, RuleField.MERCHANT, "Grab", "other"),
            rule("service", RuleSource.SYSTEM, RuleField.SERVICE, "GrabFood", "food"),
            rule("user", RuleSource.USER, RuleField.MERCHANT, "Grab", "shopping"),
        ))
        assertEquals("shopping", decision.categoryId)
    }

    @Test fun systemServiceBeatsSystemMerchant() {
        val decision = engine.categorize(transaction(), listOf(
            rule("merchant", RuleSource.SYSTEM, RuleField.MERCHANT, "Grab", "other"),
            rule("service", RuleSource.SYSTEM, RuleField.SERVICE, "GrabFood", "food"),
        ))
        assertEquals("food", decision.categoryId)
    }

    @Test fun genericGrabFallsBackWithoutServiceEvidence() {
        val decision = engine.categorize(transaction(service = null), listOf(rule("food", RuleSource.SYSTEM, RuleField.SERVICE, "GrabFood", "food")))
        assertEquals("unclassified", decision.categoryId)
    }
}
