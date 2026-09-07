package com.personal.moneytracker.domain.parser

import com.personal.moneytracker.data.local.entity.RawNotificationEvent
import com.personal.moneytracker.domain.parser.generic.GenericFinancialParser
import com.personal.moneytracker.domain.parser.grab.GrabNotificationParser
import com.personal.moneytracker.domain.parser.momo.MomoNotificationParser
import com.personal.moneytracker.domain.parser.techcombank.TechcombankNotificationParser
import com.personal.moneytracker.domain.parser.timo.TimoNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserRegistryTest {
    @Test fun routesToFirstSupportingSourceParser() {
        val source = object : NotificationParser {
            override val name = "fixture-source"
            override val version = 1
            override fun supports(event: RawNotificationEvent) = event.packageName == "fixture.app"
            override fun parse(event: RawNotificationEvent) = ParseResult.Ignored("fixture", name)
        }
        val result = ParserRegistry(listOf(source), GenericFinancialParser()).parse(rawEvent(packageName = "fixture.app"))
        assertEquals("fixture-source", (result as ParseResult.Ignored).parserName)
    }

    @Test fun sourceSkeletonsDoNotGuessWithoutVerifiedFixtures() {
        val parsers = listOf(MomoNotificationParser(), TimoNotificationParser(), TechcombankNotificationParser(), GrabNotificationParser())
        assertTrue(parsers.none { it.supports(rawEvent(packageName = "unverified.app")) })
        val result = ParserRegistry(parsers, GenericFinancialParser()).parse(rawEvent(packageName = "unverified.app"))
        assertEquals("generic-financial", (result as ParseResult.Parsed).observation.parserName)
    }

    @Test fun sensitiveEventNeverReachesParsers() {
        val result = ParserRegistry(emptyList(), GenericFinancialParser()).parse(rawEvent(status = com.personal.moneytracker.data.local.entity.RawEventStatus.SENSITIVE_IGNORED, text = null))
        assertTrue(result is ParseResult.SensitiveIgnored)
    }
}
