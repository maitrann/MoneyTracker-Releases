package com.personal.moneytracker.domain.parser

import com.personal.moneytracker.data.local.entity.RawNotificationEvent

abstract class SourceParserSkeleton(
    final override val name: String,
    private val sourceApp: SourceApp,
) : NotificationParser {
    final override val version: Int = 1

    // Package identifiers and formats remain empty until verified sanitized device fixtures exist.
    protected open val verifiedPackages: Set<String> = emptySet()

    final override fun supports(event: RawNotificationEvent): Boolean = event.packageName in verifiedPackages

    final override fun parse(event: RawNotificationEvent): ParseResult = ParseResult.Unparsed(
        reason = "No verified ${sourceApp.name} notification fixture matches this format",
        parserName = name,
    )
}
