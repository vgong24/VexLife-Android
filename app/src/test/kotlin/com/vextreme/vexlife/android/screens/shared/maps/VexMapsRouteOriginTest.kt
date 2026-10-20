package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexMapsRouteOriginTest {
    @Test
    fun pinnedOriginAllowsPlanningWithoutDeviceLocation() {
        val pinned =
            VexMapCoordinate(
                latitude = 34.5794,
                longitude = -118.1165,
            )

        assertEquals(
            pinned,
            resolveVexMapsRouteOrigin(
                selectedOrigin = pinned,
                deviceOrigin = null,
            ),
        )
    }

    @Test
    fun pinnedOriginOverridesDeviceLocation() {
        val pinned =
            VexMapCoordinate(
                latitude = 34.5794,
                longitude = -118.1165,
            )
        val device =
            VexMapCoordinate(
                latitude = 34.0522,
                longitude = -118.2437,
            )

        assertEquals(
            pinned,
            resolveVexMapsRouteOrigin(
                selectedOrigin = pinned,
                deviceOrigin = device,
            ),
        )
    }

    @Test
    fun deviceLocationRemainsConvenientDefaultWhenNoPinExists() {
        val device =
            VexMapCoordinate(
                latitude = 34.0522,
                longitude = -118.2437,
            )

        assertEquals(
            device,
            resolveVexMapsRouteOrigin(
                selectedOrigin = null,
                deviceOrigin = device,
            ),
        )
    }

    @Test
    fun routeStartIsAbsentOnlyWhenNeitherSourceExists() {
        assertNull(
            resolveVexMapsRouteOrigin(
                selectedOrigin = null,
                deviceOrigin = null,
            ),
        )
    }
}

// [VXG RealForever]
