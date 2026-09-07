package com.personal.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.personal.moneytracker.data.local.entity.CanonicalTransaction
import com.personal.moneytracker.data.local.entity.ParsedObservation
import com.personal.moneytracker.data.local.entity.TransactionObservationLink
import com.personal.moneytracker.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

data class TransactionWithObservations(
    val transaction: CanonicalTransaction,
    val links: List<TransactionObservationLink>,
    val observations: List<ParsedObservation>,
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: CanonicalTransaction)

    @Update
    suspend fun update(transaction: CanonicalTransaction)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: TransactionObservationLink): Long

    @Query("SELECT * FROM canonical_transactions ORDER BY occurredAtEpochMs DESC")
    fun observeAll(): Flow<List<CanonicalTransaction>>

    @Query("SELECT * FROM canonical_transactions ORDER BY occurredAtEpochMs DESC")
    suspend fun all(): List<CanonicalTransaction>

    @Query("SELECT * FROM canonical_transactions WHERE reconciliationReadyAtEpochMs <= :now AND syncState NOT IN ('SYNCED', 'AUTH_REQUIRED') ORDER BY occurredAtEpochMs ASC")
    suspend fun readyToSync(now: Long): List<CanonicalTransaction>

    @Query("UPDATE canonical_transactions SET syncState = 'QUEUED' WHERE syncState = 'AUTH_REQUIRED'")
    suspend fun requeueAuthorizationRequired()

    @Query("SELECT * FROM canonical_transactions WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CanonicalTransaction?

    @Query("SELECT * FROM transaction_observation_links WHERE transactionId = :transactionId")
    suspend fun linksForTransaction(transactionId: String): List<TransactionObservationLink>

    @Query("SELECT * FROM transaction_observation_links WHERE observationId = :observationId LIMIT 1")
    suspend fun linkForObservation(observationId: String): TransactionObservationLink?

    @Query("SELECT * FROM parsed_observations WHERE id IN (SELECT observationId FROM transaction_observation_links WHERE transactionId = :transactionId)")
    suspend fun observationsForTransaction(transactionId: String): List<ParsedObservation>

    @Transaction
    suspend fun detail(id: String): TransactionWithObservations? {
        val transaction = findById(id) ?: return null
        return TransactionWithObservations(transaction, linksForTransaction(id), observationsForTransaction(id))
    }
}
