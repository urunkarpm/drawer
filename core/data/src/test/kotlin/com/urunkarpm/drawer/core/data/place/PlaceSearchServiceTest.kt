package com.urunkarpm.drawer.core.data.place

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceSearchServiceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var service: PlaceSearchService

    @Before
    fun setUp() {
        service = PlaceSearchService(testDispatcher)
    }

    @Test
    fun search_emptyQuery_returnsEmptyListWithoutNetwork() = runTest(testDispatcher) {
        val result = service.search("   ")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }
}
