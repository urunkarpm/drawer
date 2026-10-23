package com.urunkarpm.drawer.feature.home.component

import android.os.Build
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
}
