package com.urunkarpm.drawer.core.needle.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * High-performance, zero-dependency mathematical expression evaluator.
 * Handles arithmetic, natural language math phrasing ("plus", "minus", "times", "divided by"),
 * percentages ("20% of 150"), powers, and square roots.
 */
object MathEvaluator {

    data class EvaluationResult(
        val expression: String,
        val result: String,
        val numericValue: Double
    )

    private val decimalFormat = DecimalFormat("0.####", DecimalFormatSymbols(Locale.US))

    /**
     * Checks whether the input query appears to be a mathematical calculation.
     */
    fun isMathQuery(query: String): Boolean {
        val trimmed = query.trim()
        val lower = trimmed.lowercase()

        // Explicit calculation triggers
        if (lower.startsWith("calculate") || lower.startsWith("calc ") ||
            lower.startsWith("what is ") || lower.startsWith("how much is ") ||
            lower.contains("percent of") || lower.contains("% of") ||
            lower.startsWith("square root of") || lower.startsWith("sqrt")
        ) {
            val stripped = stripQueryPrefix(trimmed)
            if (hasMathTokens(stripped)) return true
        }

        // Direct math expressions like "25 * 4", "15 + 30", "100 / 5", "50 - 12"
        if (hasMathTokens(trimmed) && trimmed.any { it.isDigit() }) {
            // Must contain at least one operator or percentage
            val hasOperator = trimmed.any { it in "+*/^÷×" } ||
                trimmed.contains("%") ||
                Regex("\\b\\d+\\s*-\\s*\\d+\\b").containsMatchIn(trimmed) ||
                lower.contains(" plus ") || lower.contains(" minus ") ||
                lower.contains(" times ") || lower.contains(" divided by ")
            if (hasOperator) return true
        }

        return false
    }

    /**
     * Evaluates a natural language or raw math expression into a formatted result.
     */
    fun evaluate(query: String): EvaluationResult? {
        val stripped = stripQueryPrefix(query.trim())
        val sanitized = sanitizeExpression(stripped)
        if (sanitized.isBlank()) return null

        return try {
            val parser = Parser(sanitized)
            val value = parser.parse()
            if (value.isNaN() || value.isInfinite()) return null
            val formatted = decimalFormat.format(value)
            EvaluationResult(
                expression = stripped,
                result = formatted,
                numericValue = value
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun stripQueryPrefix(input: String): String {
        return input.replace(Regex("(?i)^(?:calculate\\s+|calc\\s+|what\\s+is\\s+|how\\s+much\\s+is\\s+|solve\\s+|evaluate\\s+)"), "")
            .replace(Regex("(?i)\\?+$"), "")
            .trim()
    }

    private fun hasMathTokens(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("+") || lower.contains("-") || lower.contains("*") ||
            lower.contains("/") || lower.contains("÷") || lower.contains("×") ||
            lower.contains("^") || lower.contains("%") || lower.contains("sqrt") ||
            lower.contains("plus") || lower.contains("minus") || lower.contains("times") ||
            lower.contains("divided")
    }

    private fun sanitizeExpression(text: String): String {
        var expr = text.trim()

        // Handle "square root of X"
        expr = expr.replace(Regex("(?i)square\\s+root\\s+of\\s+([0-9.]+)"), "sqrt($1)")

        // Handle "X percent of Y" or "X% of Y" -> (X * Y) / 100
        val percentOfRegex = Regex("(?i)([0-9.]+)\\s*(?:%|percent)\\s+of\\s+([0-9.]+)")
        val percentMatch = percentOfRegex.find(expr)
        if (percentMatch != null) {
            val p = percentMatch.groupValues[1]
            val total = percentMatch.groupValues[2]
            return "(($p * $total) / 100)"
        }

        // Replace natural language operator words with symbols
        expr = expr.replace(Regex("(?i)\\bplus\\b"), "+")
            .replace(Regex("(?i)\\bminus\\b"), "-")
            .replace(Regex("(?i)\\btimes\\b|\\bmultiplied\\s+by\\b"), "*")
            .replace(Regex("(?i)\\bdivided\\s+by\\b|\\bover\\b"), "/")
            .replace("×", "*")
            .replace("÷", "/")

        return expr
    }

    /**
     * Recursive-descent expression parser.
     * Grammar:
     *   expression = term { ("+" | "-") term }
     *   term       = factor { ("*" | "/" | "%") factor }
     *   factor     = power { "^" power }
     *   power      = ("+" | "-") power | "sqrt(" expression ")" | primary
     *   primary    = number | "(" expression ")"
     */
    private class Parser(private val input: String) {
        private var pos = 0
        private val length = input.length

        fun parse(): Double {
            val result = parseExpression()
            skipWhitespace()
            if (pos < length) throw IllegalArgumentException("Unexpected character at pos $pos")
            return result
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipWhitespace()
                when (peek()) {
                    '+' -> {
                        pos++
                        value += parseTerm()
                    }
                    '-' -> {
                        pos++
                        value -= parseTerm()
                    }
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                skipWhitespace()
                when (peek()) {
                    '*' -> {
                        pos++
                        value *= parseFactor()
                    }
                    '/' -> {
                        pos++
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        value /= divisor
                    }
                    '%' -> {
                        pos++
                        value %= parseFactor()
                    }
                    else -> return value
                }
            }
        }

        private fun parseFactor(): Double {
            var value = parsePower()
            skipWhitespace()
            if (peek() == '^') {
                pos++
                val exponent = parseFactor()
                value = value.pow(exponent)
            }
            return value
        }

        private fun parsePower(): Double {
            skipWhitespace()
            if (peek() == '+') {
                pos++
                return parsePower()
            }
            if (peek() == '-') {
                pos++
                return -parsePower()
            }
            if (input.startsWith("sqrt", pos, ignoreCase = true)) {
                pos += 4
                skipWhitespace()
                if (peek() == '(') {
                    pos++
                    val inner = parseExpression()
                    skipWhitespace()
                    if (peek() == ')') pos++
                    return sqrt(inner)
                }
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Double {
            skipWhitespace()
            if (peek() == '(') {
                pos++
                val value = parseExpression()
                skipWhitespace()
                if (peek() == ')') pos++
                return value
            }

            val start = pos
            while (pos < length && (input[pos].isDigit() || input[pos] == '.')) {
                pos++
            }
            if (start == pos) throw IllegalArgumentException("Expected number at pos $pos in '$input'")
            return input.substring(start, pos).toDouble()
        }

        private fun skipWhitespace() {
            while (pos < length && input[pos].isWhitespace()) {
                pos++
            }
        }

        private fun peek(): Char = if (pos < length) input[pos] else '\u0000'
    }
}
