package com.personal.moneytracker.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NotificationFingerprintTest {
    @Test fun whitespaceAndCaseDoNotChangeFingerprint() {
        val first = NotificationFingerprint.create("app", "key", "Payment", "125 000 VND", null, null)
        val second = NotificationFingerprint.create("app", "key", " payment ", "125   000 vnd", null, null)
        assertEquals(first, second)
    }

    @Test fun differentNotificationKeysRemainDistinct() {
        assertNotEquals(
            NotificationFingerprint.create("app", "key-1", null, "50.000 VND", null, null),
            NotificationFingerprint.create("app", "key-2", null, "50.000 VND", null, null),
        )
    }
}
