package com.urunkarpm.drawer.core.needle.util

/**
 * Normalizes spoken quantities, number words, and idiomatic time phrases to digits.
 * E.g.:
 * "thirty seconds" -> "30 seconds"
 * "twenty five minutes" -> "25 minutes"
 * "half an hour" -> "30 minutes"
 * "an hour" -> "60 minutes"
 * "a minute" -> "1 minute"
 * "a second" -> "1 second"
 */
object SpokenTextNormalizer {
    private val numberWords = mapOf(
        "zero" to 0,
        "one" to 1,
        "two" to 2,
        "three" to 3,
        "four" to 4,
        "five" to 5,
        "six" to 6,
        "seven" to 7,
        "eight" to 8,
        "nine" to 9,
        "ten" to 10,
        "eleven" to 11,
        "twelve" to 12,
        "thirteen" to 13,
        "fourteen" to 14,
        "fifteen" to 15,
        "sixteen" to 16,
        "seventeen" to 17,
        "eighteen" to 18,
        "nineteen" to 19,
        "twenty" to 20,
        "thirty" to 30,
        "forty" to 40,
        "fourty" to 40,
        "fifty" to 50,
        "sixty" to 60,
        "seventy" to 70,
        "eighty" to 80,
        "ninety" to 90
    )

    fun normalize(input: String): String {
        var text = input.trim()

        // 1. Idiomatic time phrases
        text = text.replace(Regex("(?i)\\bhalf\\s+an?\\s+hour\\b"), "30 minutes")
        text = text.replace(Regex("(?i)\\ban?\\s+hour\\b"), "60 minutes")
        text = text.replace(Regex("(?i)\\ba\\s+minute\\b"), "1 minute")
        text = text.replace(Regex("(?i)\\ba\\s+second\\b"), "1 second")

        // 2. Compound tens + ones (e.g. "twenty five", "thirty-five")
        val tens = listOf(
            "twenty" to 20,
            "thirty" to 30,
            "forty" to 40,
            "fourty" to 40,
            "fifty" to 50,
            "sixty" to 60,
            "seventy" to 70,
            "eighty" to 80,
            "ninety" to 90
        )
        val ones = listOf(
            "one" to 1,
            "two" to 2,
            "three" to 3,
            "four" to 4,
            "five" to 5,
            "six" to 6,
            "seven" to 7,
            "eight" to 8,
            "nine" to 9
        )
        for ((tenWord, tenVal) in tens) {
            for ((oneWord, oneVal) in ones) {
                val pattern = Regex("(?i)\\b$tenWord[\\s-]+$oneWord\\b")
                text = text.replace(pattern, (tenVal + oneVal).toString())
            }
        }

        // 3. Single number words (e.g. "thirty" -> "30", "five" -> "5")
        for ((word, value) in numberWords) {
            val pattern = Regex("(?i)\\b$word\\b")
            text = text.replace(pattern, value.toString())
        }

        // 4. Strip trailing punctuation and quotes from speech recognition
        text = text.trim().trim('"', '\'', '`', '“', '”').trimEnd('.', '?', '!', ',', ';').trim()

        return text
    }
}
