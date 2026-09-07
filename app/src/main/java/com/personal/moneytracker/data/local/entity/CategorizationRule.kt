package com.personal.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.personal.moneytracker.domain.model.RuleField
import com.personal.moneytracker.domain.model.RuleOperator
import com.personal.moneytracker.domain.model.RuleSource

@Entity(tableName = "categorization_rules")
data class CategorizationRule(
    @PrimaryKey val id: String,
    val priority: Int,
    val enabled: Boolean,
    val field: RuleField,
    val operator: RuleOperator,
    val pattern: String,
    val targetCategoryId: String,
    val targetSubcategory: String?,
    val targetService: String?,
    val source: RuleSource,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
