package com.urunkarpm.drawer.feature.notifications.extraction

import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationExtractor @Inject constructor() {
    private val otpPattern = Pattern.compile("(?i)(?:code|otp|verification|password|secret)\\s*(?:is|:)?\\s*(\\d{4,8})\\b|\\b(\\d{4,8})\\s+is your (?:code|otp)")

    fun extractOtp(text: String): String? {
        val matcher = otpPattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(1) ?: matcher.group(2)
        } else null
    }
}
