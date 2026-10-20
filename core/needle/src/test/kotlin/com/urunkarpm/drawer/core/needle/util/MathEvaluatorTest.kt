package com.urunkarpm.drawer.core.needle.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MathEvaluatorTest {

    @Test
    fun testBasicArithmetic() {
        val r1 = MathEvaluator.evaluate("25 * 4")
        assertNotNull(r1)
        assertEquals("100", r1?.result)

        val r2 = MathEvaluator.evaluate("15 + 30")
        assertNotNull(r2)
        assertEquals("45", r2?.result)

        val r3 = MathEvaluator.evaluate("100 / 4")
        assertNotNull(r3)
        assertEquals("25", r3?.result)

        val r4 = MathEvaluator.evaluate("50 - 12")
        assertNotNull(r4)
        assertEquals("38", r4?.result)
    }

    @Test
    fun testNaturalLanguagePhrasing() {
        val r1 = MathEvaluator.evaluate("what is 25 plus 14")
        assertNotNull(r1)
        assertEquals("39", r1?.result)

        val r2 = MathEvaluator.evaluate("calculate 15 times 8")
        assertNotNull(r2)
        assertEquals("120", r2?.result)

        val r3 = MathEvaluator.evaluate("how much is 100 divided by 5?")
        assertNotNull(r3)
        assertEquals("20", r3?.result)

        val r4 = MathEvaluator.evaluate("50 minus 18")
        assertNotNull(r4)
        assertEquals("32", r4?.result)
    }

    @Test
    fun testPercentage() {
        val r1 = MathEvaluator.evaluate("20% of 150")
        assertNotNull(r1)
        assertEquals("30", r1?.result)

        val r2 = MathEvaluator.evaluate("calculate 15 percent of 200")
        assertNotNull(r2)
        assertEquals("30", r2?.result)
    }

    @Test
    fun testSquareRootAndPower() {
        val r1 = MathEvaluator.evaluate("sqrt(144)")
        assertNotNull(r1)
        assertEquals("12", r1?.result)

        val r2 = MathEvaluator.evaluate("square root of 81")
        assertNotNull(r2)
        assertEquals("9", r2?.result)

        val r3 = MathEvaluator.evaluate("2 ^ 8")
        assertNotNull(r3)
        assertEquals("256", r3?.result)
    }

    @Test
    fun testDecimalFormatting() {
        val r1 = MathEvaluator.evaluate("5 / 2")
        assertNotNull(r1)
        assertEquals("2.5", r1?.result)

        val r2 = MathEvaluator.evaluate("10 / 3")
        assertNotNull(r2)
        assertEquals("3.3333", r2?.result)
    }

    @Test
    fun testIsMathQuery() {
        assertTrue(MathEvaluator.isMathQuery("25 * 4"))
        assertTrue(MathEvaluator.isMathQuery("what is 15 + 30"))
        assertTrue(MathEvaluator.isMathQuery("calculate 20% of 150"))
        assertTrue(MathEvaluator.isMathQuery("how much is 100 / 4?"))
        assertTrue(MathEvaluator.isMathQuery("sqrt(49)"))
    }
}
