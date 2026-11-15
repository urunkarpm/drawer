package com.urunkarpm.drawer.feature.home.component

import android.os.Build
import android.telephony.CellSignalStrength
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DuoStatusIconTest {

    @Test
    fun resolveNetworkTypeLabel_5G_NR() {
        assertEquals("5G", resolveNetworkTypeLabel(TelephonyManager.NETWORK_TYPE_NR, 0))
    }

    @Test
    fun resolveNetworkTypeLabel_4G_LTE() {
        assertEquals("4G", resolveNetworkTypeLabel(TelephonyManager.NETWORK_TYPE_LTE, 0))
    }

    @Test
    fun resolveNetworkTypeLabel_3G_HSPA_and_UMTS() {
        assertEquals("3G", resolveNetworkTypeLabel(TelephonyManager.NETWORK_TYPE_HSPA, 0))
        assertEquals("3G", resolveNetworkTypeLabel(TelephonyManager.NETWORK_TYPE_HSPAP, 0))
        assertEquals("3G", resolveNetworkTypeLabel(TelephonyManager.NETWORK_TYPE_UMTS, 0))
    }

    @Test
    fun resolveNetworkTypeLabel_2G_EDGE_and_GPRS() {
        assertEquals("2G", resolveNetworkTypeLabel(TelephonyManager.NETWORK_TYPE_EDGE, 0))
        assertEquals("2G", resolveNetworkTypeLabel(TelephonyManager.NETWORK_TYPE_GPRS, 0))
    }

    @Test
    fun resolveNetworkTypeLabel_override_5G_NSA() {
        assertEquals(
            "5G",
            resolveNetworkTypeLabel(
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA
            )
        )
    }

    @Test
    fun resolveNetworkTypeLabel_override_5G_Advanced() {
        assertEquals(
            "5G+",
            resolveNetworkTypeLabel(
                TelephonyManager.NETWORK_TYPE_NR,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED
            )
        )
    }

    @Test
    fun resolveNetworkTypeLabel_override_LTE_CA() {
        assertEquals(
            "4G+",
            resolveNetworkTypeLabel(
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA
            )
        )
    }

    @Test
    fun resolveNetworkTypeLabel_unknown() {
        assertNull(resolveNetworkTypeLabel(TelephonyManager.NETWORK_TYPE_UNKNOWN, 0))
    }

    @Test
    fun duoSystemStatus_defaultNetworkType() {
        val status = DuoSystemStatus()
        assertNull(status.networkType)

        val updated = status.copy(networkType = "5G")
        assertEquals("5G", updated.networkType)
    }

    @Test
    fun extractSignalLevel_null_returnsZero() {
        assertEquals(0, extractSignalLevel(null))
    }

    @Test
    fun extractSignalLevel_mockedSignalStrength() {
        val mockSignal = io.mockk.mockk<android.telephony.SignalStrength>()
        io.mockk.every { mockSignal.level } returns 4
        assertEquals(4, extractSignalLevel(mockSignal))
    }

    @Test
    fun extractSignalLevel_noSuchMethodError_returnsZero() {
        val mockSignal = io.mockk.mockk<android.telephony.SignalStrength>()
        io.mockk.every { mockSignal.level } throws NoSuchMethodError("getLevel")
        assertEquals(0, extractSignalLevel(mockSignal))
    }

    @Test
    fun cellSignalStrengthToLevel_evaluatesDbmThresholds() {
        val mockCss = io.mockk.mockk<CellSignalStrength>()
        io.mockk.every { mockCss.level } returns 1
        io.mockk.every { mockCss.asuLevel } returns 0

        io.mockk.every { mockCss.dbm } returns -75
        assertEquals(4, cellSignalStrengthToLevel(mockCss))

        io.mockk.every { mockCss.dbm } returns -95
        assertEquals(3, cellSignalStrengthToLevel(mockCss))

        io.mockk.every { mockCss.dbm } returns -110
        assertEquals(2, cellSignalStrengthToLevel(mockCss))

        io.mockk.every { mockCss.dbm } returns -118
        assertEquals(1, cellSignalStrengthToLevel(mockCss))

        io.mockk.every { mockCss.dbm } returns -125
        // When dbm < -120 and reported level is 1, maxOf returns 1
        assertEquals(1, cellSignalStrengthToLevel(mockCss))
    }

    @Test
    fun extractSignalLevel_xiaomiBug_signalLevelOne_rescuedByCellSignalStrengthDbm() {
        val mockSignal = io.mockk.mockk<android.telephony.SignalStrength>()
        val mockCell = io.mockk.mockk<CellSignalStrength>()

        // Xiaomi RIL bug: getLevel() returns 1 despite full signal
        io.mockk.every { mockSignal.level } returns 1
        io.mockk.every { mockSignal.gsmSignalStrength } returns 0
        io.mockk.every { mockSignal.cellSignalStrengths } returns listOf(mockCell)

        // Raw dBm indicates excellent reception (-75 dBm = 4 bars)
        io.mockk.every { mockCell.level } returns 1
        io.mockk.every { mockCell.dbm } returns -75
        io.mockk.every { mockCell.asuLevel } returns 0

        assertEquals(4, extractSignalLevel(mockSignal))
    }

    @Test
    fun extractSignalLevel_xiaomiBug_rescuedByGsmAsu() {
        val mockSignal = io.mockk.mockk<android.telephony.SignalStrength>()

        // Xiaomi reports level 1, empty cellSignalStrengths, but valid GSM ASU
        io.mockk.every { mockSignal.level } returns 1
        io.mockk.every { mockSignal.cellSignalStrengths } returns emptyList()
        io.mockk.every { mockSignal.gsmSignalStrength } returns 28 // 28 ASU = 4 bars

        assertEquals(4, extractSignalLevel(mockSignal))
    }

    @Test
    fun extractSignalLevel_weakSignal_remainsOne() {
        val mockSignal = io.mockk.mockk<android.telephony.SignalStrength>()
        val mockCell = io.mockk.mockk<CellSignalStrength>()

        io.mockk.every { mockSignal.level } returns 1
        io.mockk.every { mockSignal.gsmSignalStrength } returns 5 // 5 ASU = 1 bar
        io.mockk.every { mockSignal.cellSignalStrengths } returns listOf(mockCell)

        io.mockk.every { mockCell.level } returns 1
        io.mockk.every { mockCell.dbm } returns -118 // -118 dBm = 1 bar
        io.mockk.every { mockCell.asuLevel } returns 12 // 1 bar range

        assertEquals(1, extractSignalLevel(mockSignal))
    }
}
