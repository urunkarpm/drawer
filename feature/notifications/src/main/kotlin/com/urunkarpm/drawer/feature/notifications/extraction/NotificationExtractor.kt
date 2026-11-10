package com.urunkarpm.drawer.feature.notifications.extraction

import java.util.regex.Pattern

private val otpPattern = Pattern.compile("(?i)(?:code|otp|verification|password|secret)\\s*(?:is|:)?\\s*(\\d{4,8})\\b|\\b(\\d{4,8})\\s+is your (?:code|otp)")

// ponytail: direct regex OTP code extractor from notification title/body; ceiling: 4-8 digit numeric codes; upgrade path: ML Kit text entity extraction.
fun extractOtp(text: String): String? {
    val matcher = otpPattern.matcher(text)
    return if (matcher.find()) {
        matcher.group(1) ?: matcher.group(2)
    } else null
}

class NotificationExtractor {
    fun extractOtp(text: String): String? = com.urunkarpm.drawer.feature.notifications.extraction.extractOtp(text)
}
