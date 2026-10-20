package com.vextreme.vexlife.android.screens.shared.maps

import android.location.Address
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsAndroidGeocoderProjectionTest {
    @Test
    fun addressProjectsToProviderScopedDestinationCandidate() {
        val address =
            Address(Locale.US).apply {
                latitude = 34.5794
                longitude = -118.1165
                featureName = "Palmdale"
                locality = "Palmdale"
                adminArea = "CA"
                countryName = "United States"
                setAddressLine(
                    0,
                    "Palmdale, CA, United States",
                )
            }

        val result =
            requireNotNull(
                projectVexMapsAndroidGeocoderAddress(
                    address,
                ),
            )

        assertEquals(
            "Palmdale",
            result.name,
        )
        assertEquals(
            "Palmdale, CA, United States",
            result.subtitle,
        )
        assertEquals(
            34.5794,
            result.coordinate.latitude,
            0.0,
        )
        assertEquals(
            -118.1165,
            result.coordinate.longitude,
            0.0,
        )
        assertTrue(
            result.placeRef
                .startsWith(
                    "android-geocoder.",
                ),
        )
        assertFalse(
            result.placeRef
                .contains(
                    "Palmdale",
                    ignoreCase = true,
                ),
        )
    }

    @Test
    fun addressWithoutCoordinatesIsNotPromotedToPlaceTruth() {
        val address =
            Address(Locale.US).apply {
                featureName = "Somewhere"
            }

        assertNotNull(address)
        assertEquals(
            null,
            projectVexMapsAndroidGeocoderAddress(
                address,
            ),
        )
    }
}

// [VXG RealForever]
