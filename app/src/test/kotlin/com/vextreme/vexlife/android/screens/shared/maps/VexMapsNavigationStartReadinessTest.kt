package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsNavigationStartReadinessTest {
    @Test
    fun permissionAloneDoesNotMakeNavigationStartable() {
        assertFalse(
            canStartVexMapsNavigation(
                precisePermissionGranted = true,
                hasCurrentLocation = false,
                locationFixReady = false,
            ),
        )
    }

    @Test
    fun staleLocationStateDoesNotMakeNavigationStartable() {
        assertFalse(
            canStartVexMapsNavigation(
                precisePermissionGranted = true,
                hasCurrentLocation = true,
                locationFixReady = false,
            ),
        )
    }

    @Test
    fun precisePermissionAndSuccessfulCurrentFixAreBothRequired() {
        assertTrue(
            canStartVexMapsNavigation(
                precisePermissionGranted = true,
                hasCurrentLocation = true,
                locationFixReady = true,
            ),
        )
    }

    @Test
    fun revokedPermissionHoldsStartEvenWithRetainedFix() {
        assertFalse(
            canStartVexMapsNavigation(
                precisePermissionGranted = false,
                hasCurrentLocation = true,
                locationFixReady = true,
            ),
        )
    }
}

// [VXG RealForever]
