package com.personal.moneytracker.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Minimal REST implementation; tokens are supplied in-memory and never persisted or logged. */
@Singleton
class GoogleSheetsRestRemote @Inject constructor() : GoogleSheetsRemote {
    override suspend fun findTransactionRow(config: SheetConfig, accessToken: String, transactionId: String): RemoteWriteResult = request(
        "GET", url(config, "${config.sheetName}!A:A"), accessToken, null,
    ) { response ->
        val values = JSONObject(response).optJSONArray("values") ?: JSONArray()
        (0 until values.length()).firstOrNull { values.optJSONArray(it)?.optString(0) == transactionId }
            ?.let { RemoteWriteResult.Found(it + 1) } ?: RemoteWriteResult.Missing
    }

    override suspend fun append(config: SheetConfig, accessToken: String, row: List<String>): RemoteWriteResult = request(
        "POST", "${url(config, config.sheetName)}:append?valueInputOption=RAW&insertDataOption=INSERT_ROWS", accessToken, body(row),
    ) { RemoteWriteResult.Found(-1) }

    override suspend fun update(config: SheetConfig, accessToken: String, rowNumber: Int, row: List<String>): RemoteWriteResult = request(
        "PUT", "${url(config, "${config.sheetName}!A$rowNumber:S$rowNumber")}?valueInputOption=RAW", accessToken, body(row),
    ) { RemoteWriteResult.Found(rowNumber) }

    private fun url(config: SheetConfig, range: String): String = "https://sheets.googleapis.com/v4/spreadsheets/${config.spreadsheetId}/values/${URLEncoder.encode(range, "UTF-8")}" 
    private fun body(row: List<String>) = JSONObject().put("majorDimension", "ROWS").put("values", JSONArray().put(JSONArray(row)))
    private suspend fun request(method: String, target: String, token: String, json: JSONObject?, success: (String) -> RemoteWriteResult): RemoteWriteResult = withContext(Dispatchers.IO) {
        try {
            val c = URL(target).openConnection() as HttpURLConnection
            c.requestMethod = method; c.setRequestProperty("Authorization", "Bearer $token"); c.setRequestProperty("Content-Type", "application/json")
            c.connectTimeout = 15_000; c.readTimeout = 15_000
            if (json != null) { c.doOutput = true; c.outputStream.use { it.write(json.toString().toByteArray()) } }
            when (val code = c.responseCode) {
                in 200..299 -> success(c.inputStream.bufferedReader().use { it.readText() })
                401, 403 -> RemoteWriteResult.AuthorizationFailure
                408, 429, in 500..599 -> RemoteWriteResult.TransientFailure
                else -> RemoteWriteResult.PermanentFailure
            }
        } catch (_: Exception) { RemoteWriteResult.TransientFailure }
    }
}
