package com.personal.moneytracker.sync

import com.personal.moneytracker.data.local.entity.CanonicalTransaction
import java.time.Instant

data class SheetConfig(val spreadsheetId: String, val sheetName: String)

sealed interface AccessTokenResult {
    data class Granted(val token: String) : AccessTokenResult
    data object AuthorizationRequired : AccessTokenResult
}

sealed interface RemoteWriteResult {
    data class Found(val row: Int) : RemoteWriteResult
    data object Missing : RemoteWriteResult
    data object TransientFailure : RemoteWriteResult
    data object AuthorizationFailure : RemoteWriteResult
    data object PermanentFailure : RemoteWriteResult
}

interface GoogleAuthorizationGateway {
    /** Never prompts: workers use this only after a user has explicitly granted the Sheets scope. */
    suspend fun accessTokenOrNull(): AccessTokenResult
}

interface GoogleSheetsRemote {
    suspend fun findTransactionRow(config: SheetConfig, accessToken: String, transactionId: String): RemoteWriteResult
    suspend fun append(config: SheetConfig, accessToken: String, row: List<String>): RemoteWriteResult
    suspend fun update(config: SheetConfig, accessToken: String, rowNumber: Int, row: List<String>): RemoteWriteResult
}

fun CanonicalTransaction.toSheetRow(sourceApps: String): List<String> = listOf(
    id, Instant.ofEpochMilli(occurredAtEpochMs).toString(), type.name, amountVnd.toString(), currency,
    merchant.orEmpty(), service.orEmpty(), categoryId, subcategory.orEmpty(), fundingSource.orEmpty(),
    paymentChannel.orEmpty(), description.orEmpty(), note.orEmpty(), matchConfidence.toString(),
    classificationConfidence.toString(), sourceApps, Instant.ofEpochMilli(createdAtEpochMs).toString(),
    Instant.ofEpochMilli(updatedAtEpochMs).toString(), "false",
)
