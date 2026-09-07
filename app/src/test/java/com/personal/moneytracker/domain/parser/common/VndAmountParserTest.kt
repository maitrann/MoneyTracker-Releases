package com.personal.moneytracker.domain.parser.common

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VndAmountParserTest {
    @Test fun parsesFixtureCases() {
        val json = javaClass.classLoader!!.getResource("fixtures/vnd_amounts.json")!!.readText()
        val cases = JSONObject(json).getJSONArray("cases")
        repeat(cases.length()) { index ->
            val fixture = cases.getJSONObject(index)
            val result = VndAmountParser.parse(fixture.getString("text"))
            assertTrue(fixture.getString("name"), result is AmountParseResult.Found)
            assertEquals(fixture.getString("name"), fixture.getLong("amount"), (result as AmountParseResult.Found).amountVnd)
        }
    }

    @Test fun missingAmountIsExplicit() {
        assertEquals(AmountParseResult.Missing, VndAmountParser.parse("Payment completed"))
    }

    @Test fun multipleCurrencyAmountsAreAmbiguous() {
        val result = VndAmountParser.parse("Old 100.000đ, new 125.000đ")
        assertTrue(result is AmountParseResult.Ambiguous)
        assertEquals(listOf(100000L, 125000L), (result as AmountParseResult.Ambiguous).candidates)
    }

    @Test fun multipleBareNumbersAreAmbiguousRatherThanGuessed() {
        assertTrue(VndAmountParser.parse("Order 123 and reference 456") is AmountParseResult.Ambiguous)
    }
}
