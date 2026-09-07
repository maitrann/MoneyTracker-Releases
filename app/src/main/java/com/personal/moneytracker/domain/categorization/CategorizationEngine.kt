package com.personal.moneytracker.domain.categorization

import com.personal.moneytracker.data.local.entity.CanonicalTransaction
import com.personal.moneytracker.data.local.entity.CategorizationRule
import com.personal.moneytracker.domain.model.RuleField
import com.personal.moneytracker.domain.model.RuleOperator
import com.personal.moneytracker.domain.model.RuleSource

data class CategorizationDecision(val categoryId: String, val subcategory: String?, val confidence: Double, val reason: String)

class CategorizationEngine {
    fun categorize(transaction: CanonicalTransaction, rules: List<CategorizationRule>): CategorizationDecision {
        if (transaction.userCategoryLocked) return CategorizationDecision(transaction.categoryId, transaction.subcategory, transaction.classificationConfidence, "User category lock")
        val candidates = rules.filter { it.enabled && matches(it, transaction) }
        val selected = candidates.sortedWith(compareByDescending<CategorizationRule> { rank(it) }.thenByDescending { it.priority }.thenBy { it.id }).firstOrNull()
        return if (selected == null) CategorizationDecision("unclassified", null, 0.0, "Fallback: no matching rule")
        else CategorizationDecision(selected.targetCategoryId, selected.targetSubcategory, if (selected.source == RuleSource.USER) 0.95 else 0.9, "${selected.source} rule ${selected.id}")
    }

    private fun rank(rule: CategorizationRule): Int = when {
        rule.source == RuleSource.USER -> 4
        rule.field == RuleField.SERVICE -> 3
        rule.field == RuleField.MERCHANT -> 2
        else -> 1
    }

    private fun matches(rule: CategorizationRule, transaction: CanonicalTransaction): Boolean {
        val value = when (rule.field) {
            RuleField.MERCHANT -> transaction.merchant
            RuleField.SERVICE -> transaction.service
            RuleField.DESCRIPTION -> transaction.description
            RuleField.SOURCE_APP -> null
            RuleField.PAYMENT_CHANNEL -> transaction.paymentChannel
        } ?: return false
        return when (rule.operator) {
            RuleOperator.EQUALS -> value.equals(rule.pattern, ignoreCase = true)
            RuleOperator.CONTAINS -> value.contains(rule.pattern, ignoreCase = true)
            RuleOperator.STARTS_WITH -> value.startsWith(rule.pattern, ignoreCase = true)
            RuleOperator.REGEX -> runCatching { Regex(rule.pattern, RegexOption.IGNORE_CASE).containsMatchIn(value) }.getOrDefault(false)
        }
    }
}
