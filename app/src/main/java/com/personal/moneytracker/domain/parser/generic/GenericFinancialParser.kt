package com.personal.moneytracker.domain.parser.generic

import com.personal.moneytracker.data.local.entity.RawNotificationEvent
import com.personal.moneytracker.domain.parser.NotificationParser
import com.personal.moneytracker.domain.parser.ObservationDraft
import com.personal.moneytracker.domain.parser.ParseResult
import com.personal.moneytracker.domain.parser.SourceApp
import com.personal.moneytracker.domain.parser.common.AmountParseResult
import com.personal.moneytracker.domain.parser.common.DirectionInferer
import com.personal.moneytracker.domain.parser.common.NotificationTextNormalizer
import com.personal.moneytracker.domain.parser.common.VndAmountParser
import javax.inject.Inject

class GenericFinancialParser @Inject constructor() : NotificationParser {
    override val name = "generic-financial"
    override val version = 1
    override fun supports(event: RawNotificationEvent): Boolean = true

    override fun parse(event: RawNotificationEvent): ParseResult {
        val text = NotificationTextNormalizer.combine(event)
        if (text.isBlank()) return ParseResult.Unparsed("No notification text", name)
        return when (val amount = VndAmountParser.parse(text)) {
            AmountParseResult.Missing -> ParseResult.Unparsed("No explicit VND or signed amount", name)
            is AmountParseResult.Ambiguous -> ParseResult.Unparsed("Multiple amount candidates", name)
            is AmountParseResult.Found -> ParseResult.Parsed(
                ObservationDraft(
                    sourceApp = SourceApp.GENERIC,
                    amountVnd = amount.amountVnd,
                    directionHint = DirectionInferer.infer(text),
                    eventAtEpochMs = event.postedAtEpochMs,
                    descriptionNormalized = text,
                    parserName = name,
                    parserVersion = version,
                    confidence = 0.45,
                ),
            )
        }
    }
}
