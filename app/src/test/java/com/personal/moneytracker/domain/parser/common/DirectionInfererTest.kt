package com.personal.moneytracker.domain.parser.common

import com.personal.moneytracker.domain.parser.DirectionHint
import org.junit.Assert.assertEquals
import org.junit.Test

class DirectionInfererTest {
    @Test fun infersConservativeDirections() {
        assertEquals(DirectionHint.DEBIT, DirectionInferer.infer("Payment -125.000đ"))
        assertEquals(DirectionHint.CREDIT, DirectionInferer.infer("Received +125.000đ"))
        assertEquals(DirectionHint.TRANSFER, DirectionInferer.infer("Chuyển tiền 125.000đ"))
        assertEquals(DirectionHint.UNKNOWN, DirectionInferer.infer("Balance notice 125.000đ"))
        assertEquals(DirectionHint.UNKNOWN, DirectionInferer.infer("Debit and credit 125.000đ"))
    }
}
