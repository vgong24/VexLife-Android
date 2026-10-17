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
    fun legacyLibertyStorageBecomesLightAppearance() {
        assertEquals(
            VexMapsAppearancePreference.LIGHT,
            vexMapsAppearancePreferenceFromStoredValue(
                "liberty",
            ),
        )
    }

    @Test
    fun explicitAppearanceValuesRoundTripWithoutProviderNames() {
        assertEquals(
            "light",
            VexMapsAppearancePreference
                .LIGHT
                .toStoredAppearanceValue(),
        )
        assertEquals(
            VexMapsAppearancePreference.LIGHT,
            vexMapsAppearancePreferenceFromStoredValue(
                "light",
            ),
        )

        assertEquals(
            "dark",
            VexMapsAppearancePreference
                .DARK
                .toStoredAppearanceValue(),
        )
        assertEquals(
            VexMapsAppearancePreference.DARK,
            vexMapsAppearancePreferenceFromStoredValue(
                "dark",
            ),
        )
    }
}

// [VXG RealForever]
