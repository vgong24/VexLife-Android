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
    fun onlyFreshLocatedResultSatisfiesStartPreflight() {
        assertTrue(
            isVexMapsNavigationStartPreflightSatisfied(
                VexDeviceLocationResult.Located(
                    latitude = 34.5794,
                    longitude = -118.1165,
                    accuracyMeters = 8f,
                ),
            ),
        )
        assertFalse(
            isVexMapsNavigationStartPreflightSatisfied(
                VexDeviceLocationResult.LocationDisabled,
            ),
        )
        assertFalse(
            isVexMapsNavigationStartPreflightSatisfied(
                VexDeviceLocationResult.Unavailable,
            ),
        )
        assertFalse(
            isVexMapsNavigationStartPreflightSatisfied(
                VexDeviceLocationResult.PermissionMissing,
            ),
        )
    }

    @Test
    fun inaccurateLocatedResultDoesNotSatisfyStartPreflight() {
        assertFalse(
            isVexMapsNavigationStartPreflightSatisfied(
                VexDeviceLocationResult.Located(
                    latitude = 34.5794,
                    longitude = -118.1165,
                    accuracyMeters = 81f,
                ),
            ),
        )
        assertTrue(
            isVexMapsNavigationStartPreflightSatisfied(
                VexDeviceLocationResult.Located(
                    latitude = 34.5794,
                    longitude = -118.1165,
                    accuracyMeters = 80f,
                ),
            ),
        )
    }

    @Test
    fun startPreflightUsesTheSuppliedMatchingPolicy() {
        val policy =
            VexRouteMatchingPolicy(
                maximumAccuracyMeters = 30f,
            )

        assertFalse(
            isVexMapsNavigationStartPreflightSatisfied(
                result =
                    VexDeviceLocationResult.Located(
                        latitude = 34.5794,
                        longitude = -118.1165,
                        accuracyMeters = 31f,
                    ),
                matchingPolicy = policy,
            ),
        )
        assertTrue(
            isVexMapsNavigationStartPreflightSatisfied(
                result =
                    VexDeviceLocationResult.Located(
                        latitude = 34.5794,
                        longitude = -118.1165,
                        accuracyMeters = 30f,
                    ),
                matchingPolicy = policy,
            ),
        )
    }

    @Test
    fun unknownAccuracyKeepsExistingMatcherAdmission() {
        assertTrue(
            isVexMapsNavigationStartPreflightSatisfied(
                VexDeviceLocationResult.Located(
                    latitude = 34.5794,
                    longitude = -118.1165,
                    accuracyMeters = null,
                ),
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
