package com.personal.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.personal.moneytracker.data.local.entity.Category
import com.personal.moneytracker.data.local.entity.CategorizationRule
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorizationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCategories(categories: List<Category>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRules(rules: List<CategorizationRule>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertRule(rule: CategorizationRule)
    @Update suspend fun updateRule(rule: CategorizationRule)
    @Query("SELECT * FROM categorization_rules WHERE enabled = 1") suspend fun activeRules(): List<CategorizationRule>
    @Query("SELECT * FROM categorization_rules ORDER BY source DESC, priority DESC, id ASC") fun observeRules(): Flow<List<CategorizationRule>>
    @Query("SELECT * FROM categories WHERE active = 1 ORDER BY sortOrder") fun observeCategories(): Flow<List<Category>>
}
