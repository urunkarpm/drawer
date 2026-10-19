package com.urunkarpm.drawer.feature.notifications

import com.urunkarpm.drawer.feature.notifications.extraction.NotificationExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationExtractorTest {
    private val extractor = NotificationExtractor()

    @Test
    fun testExtractOtpFromNotification() {
        val sms = "Your verification code is 492019. Valid for 10 minutes."
        val otp = extractor.extractOtp(sms)
        assertNotNull(otp)
        assertEquals("492019", otp)
    }

    @Test
    fun testExtractOtpWithPrefixAndColon() {
        val sms = "Your OTP: 884129 for banking transaction"
        val otp = extractor.extractOtp(sms)
        assertNotNull(otp)
        assertEquals("884129", otp)
    }

    @Test
    fun testExtractOtpWithIsYourOtp() {
        val sms = "552194 is your OTP for account login."
        val otp = extractor.extractOtp(sms)
        assertNotNull(otp)
        assertEquals("552194", otp)
    }

    @Test
    fun testNoOtpInRegularMessage() {
        val text = "Hey what time are we meeting tonight?"
        val otp = extractor.extractOtp(text)
        assertNull(otp)
    }
}
