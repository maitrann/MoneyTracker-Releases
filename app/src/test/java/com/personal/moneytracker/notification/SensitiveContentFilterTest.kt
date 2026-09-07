package com.personal.moneytracker.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveContentFilterTest {
    @Test fun rejectsOtpAndVerificationMessages() {
        assertTrue(SensitiveContentFilter.isSensitive("Mã OTP", "123456, không chia sẻ"))
        assertTrue(SensitiveContentFilter.isSensitive("Verification code", "Code 123456"))
        assertTrue(SensitiveContentFilter.isSensitive("Smart OTP", null))
    }

    @Test fun keepsNormalAmountNotifications() {
        assertFalse(SensitiveContentFilter.isSensitive("Thanh toán thành công", "-125.000 VND tại cửa hàng"))
    }
}
