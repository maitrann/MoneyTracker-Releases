package com.personal.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.moneytracker.data.local.entity.ParsedObservation
import kotlinx.coroutines.flow.Flow

@Dao
interface ParsedObservationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(observation: ParsedObservation): Long

    @Query("SELECT * FROM parsed_observations WHERE rawEventId = :rawEventId LIMIT 1")
    suspend fun findByRawEventId(rawEventId: String): ParsedObservation?

    @Query("SELECT * FROM parsed_observations WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ParsedObservation?

    @Query("SELECT * FROM parsed_observations")
    fun observeAll(): Flow<List<ParsedObservation>>

    @Query("SELECT COUNT(*) FROM parsed_observations")
    suspend fun count(): Int
}
