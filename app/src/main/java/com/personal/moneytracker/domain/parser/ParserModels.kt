package com.personal.moneytracker.domain.parser

import com.personal.moneytracker.data.local.entity.RawNotificationEvent

enum class DirectionHint { DEBIT, CREDIT, TRANSFER, UNKNOWN }
enum class SourceApp { GENERIC, MOMO, TIMO, TECHCOMBANK, GRAB }

data class ObservationDraft(
    val sourceApp: SourceApp,
    val amountVnd: Long,
    val directionHint: DirectionHint,
    val eventAtEpochMs: Long,
    val merchant: String? = null,
    val service: String? = null,
    val fundingSourceHint: String? = null,
    val paymentChannelHint: String? = null,
    val referenceHint: String? = null,
    val descriptionNormalized: String? = null,
    val parserName: String,
    val parserVersion: Int,
    val confidence: Double,
)

sealed interface ParseResult {
    data class Parsed(val observation: ObservationDraft) : ParseResult
    data class Ignored(val reason: String, val parserName: String? = null) : ParseResult
    data class Unparsed(val reason: String, val parserName: String? = null) : ParseResult
    data class SensitiveIgnored(val reason: String = "Sensitive authentication content removed") : ParseResult
}

interface NotificationParser {
    val name: String
    val version: Int
    fun supports(event: RawNotificationEvent): Boolean
    fun parse(event: RawNotificationEvent): ParseResult
}
