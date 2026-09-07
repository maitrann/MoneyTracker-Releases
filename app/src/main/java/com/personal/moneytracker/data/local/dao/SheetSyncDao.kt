package com.personal.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.moneytracker.data.local.entity.SheetSyncRecord

@Dao
interface SheetSyncDao {
    @Query("SELECT * FROM sheet_sync_records WHERE transactionId = :transactionId LIMIT 1")
    suspend fun find(transactionId: String): SheetSyncRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: SheetSyncRecord)
}
