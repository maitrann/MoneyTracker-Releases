package com.personal.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String,
    val displayName: String,
    val parentId: String?,
    val isSystem: Boolean,
    val sortOrder: Int,
    val active: Boolean = true,
)
