package com.personal.moneytracker.domain.parser

import com.personal.moneytracker.data.local.entity.RawEventStatus
import com.personal.moneytracker.data.local.entity.RawNotificationEvent

class ParserRegistry(
    private val sourceParsers: List<NotificationParser>,
    private val genericParser: NotificationParser,
) {
    fun parse(event: RawNotificationEvent): ParseResult {
        if (event.captureStatus == RawEventStatus.SENSITIVE_IGNORED) return ParseResult.SensitiveIgnored()
        val parser = sourceParsers.firstOrNull { it.supports(event) } ?: genericParser
        return parser.parse(event)
    }
}
