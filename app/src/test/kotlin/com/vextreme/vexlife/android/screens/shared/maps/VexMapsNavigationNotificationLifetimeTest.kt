package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsNavigationNotificationLifetimeTest {
    @Test
    fun arrivedNotificationOutlivesForegroundService() {
        assertTrue(
            shouldRetainVexMapsNavigationNotification(
                VexNavigationSessionPhase.ARRIVED,
            ),
        )
    }

    @Test
    fun nonArrivedPhasesDoNotDetachNotification() {
        listOf(
            VexNavigationSessionPhase.IDLE,
            VexNavigationSessionPhase.READY,
            VexNavigationSessionPhase.ACTIVE,
            VexNavigationSessionPhase.OFF_ROUTE,
            VexNavigationSessionPhase.LOCATION_HELD,
        ).forEach { phase ->
            assertFalse(
                shouldRetainVexMapsNavigationNotification(
                    phase,
                ),
            )
        }
    }
}

// [VXG RealForever]
