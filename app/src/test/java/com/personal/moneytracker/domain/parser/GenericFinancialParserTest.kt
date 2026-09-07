package com.personal.moneytracker.domain.parser

import com.personal.moneytracker.domain.parser.generic.GenericFinancialParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericFinancialParserTest {
    private val parser = GenericFinancialParser()

    @Test fun parsesExplicitSyntheticFinancialNotification() {
        val result = parser.parse(rawEvent(text = "Payment\n-125.000đ")) as ParseResult.Parsed
        assertEquals(125000L, result.observation.amountVnd)
        assertEquals(DirectionHint.DEBIT, result.observation.directionHint)
        assertEquals(SourceApp.GENERIC, result.observation.sourceApp)
        assertEquals(null, result.observation.merchant)
    }

    @Test fun amountWithUnknownDirectionRemainsUnknown() {
        val result = parser.parse(rawEvent(text = "Balance notice 125.000đ")) as ParseResult.Parsed
        assertEquals(DirectionHint.UNKNOWN, result.observation.directionHint)
    }

    @Test fun unrecognizedAndAmbiguousInputIsUnparsed() {
        assertTrue(parser.parse(rawEvent(text = "Hello")) is ParseResult.Unparsed)
        assertTrue(parser.parse(rawEvent(text = "Old 100.000đ new 125.000đ")) is ParseResult.Unparsed)
    }
}
