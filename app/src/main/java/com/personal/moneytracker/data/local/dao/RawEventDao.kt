package com.personal.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.moneytracker.data.local.entity.RawNotificationEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface RawEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: RawNotificationEvent): Long

    @Query("SELECT * FROM raw_notification_events ORDER BY postedAtEpochMs DESC")
    fun observeAll(): Flow<List<RawNotificationEvent>>

    @Query("SELECT COUNT(*) FROM raw_notification_events")
    suspend fun count(): Int

    @Query("SELECT * FROM raw_notification_events WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): RawNotificationEvent?

    @Query("UPDATE raw_notification_events SET captureStatus = :status, parserName = :parserName, processingReason = :reason WHERE id = :id")
    suspend fun updateProcessing(id: String, status: com.personal.moneytracker.data.local.entity.RawEventStatus, parserName: String?, reason: String?)
}
