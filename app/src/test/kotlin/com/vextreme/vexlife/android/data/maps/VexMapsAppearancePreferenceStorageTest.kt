package com.vextreme.vexlife.android.data.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexMapsAppearancePreferenceStorageTest {
    @Test
    fun automaticUsesTheExistingNullableStorageEncoding() {
        assertNull(
            VexMapsAppearancePreference
                .AUTOMATIC
                .toStoredAppearanceValue(),
        )
        assertEquals(
            VexMapsAppearancePreference.AUTOMATIC,
            vexMapsAppearancePreferenceFromStoredValue(
                null,
            ),
        )
    }

    @Test
    fun legacyProviderStylesDoNotInventExplicitHumanPreference() {
        listOf(
            "liberty",
            "dark",
        ).forEach { legacyValue ->
            assertEquals(
                VexMapsAppearancePreference.AUTOMATIC,
                vexMapsAppearancePreferenceFromStoredValue(
                    legacyValue,
                ),
            )
        }
    }

    @Test
    fun explicitAppearanceValuesRoundTripWithoutProviderNames() {
        assertEquals(
            "appearance.light",
            VexMapsAppearancePreference
                .LIGHT
                .toStoredAppearanceValue(),
        )
        assertEquals(
            VexMapsAppearancePreference.LIGHT,
            vexMapsAppearancePreferenceFromStoredValue(
                "appearance.light",
            ),
        )

        assertEquals(
            "appearance.dark",
            VexMapsAppearancePreference
                .DARK
                .toStoredAppearanceValue(),
        )
        assertEquals(
            VexMapsAppearancePreference.DARK,
            vexMapsAppearancePreferenceFromStoredValue(
                "appearance.dark",
            ),
        )
    }
}

// [VXG RealForever]
