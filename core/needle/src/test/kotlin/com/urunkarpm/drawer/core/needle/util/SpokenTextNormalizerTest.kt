package com.urunkarpm.drawer.core.needle.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenTextNormalizerTest {

    @Test
    fun testNormalizeSingleNumberWords() {
        assertEquals("set a timer for 30 seconds", SpokenTextNormalizer.normalize("set a timer for thirty seconds"))
        assertEquals("set timer for 5 minutes", SpokenTextNormalizer.normalize("set timer for five minutes"))
        assertEquals("timer 10 s", SpokenTextNormalizer.normalize("timer ten s"))
    }

    @Test
    fun testNormalizeCompoundNumberWords() {
        assertEquals("set a timer for 25 minutes", SpokenTextNormalizer.normalize("set a timer for twenty five minutes"))
        assertEquals("set a timer for 45 seconds", SpokenTextNormalizer.normalize("set a timer for forty-five seconds"))
    }

    @Test
    fun testNormalizeIdiomaticPhrases() {
        assertEquals("set a timer for 30 minutes", SpokenTextNormalizer.normalize("set a timer for half an hour"))
        assertEquals("timer 60 minutes", SpokenTextNormalizer.normalize("timer an hour"))
        assertEquals("set a timer for 1 minute", SpokenTextNormalizer.normalize("set a timer for a minute"))
    }

    @Test
    fun testNormalizeAlarmPhrases() {
        assertEquals("set alarm for 7 am", SpokenTextNormalizer.normalize("set alarm for seven am"))
        assertEquals("set alarm for 7 30 am", SpokenTextNormalizer.normalize("set alarm for seven thirty am"))
    }
}
