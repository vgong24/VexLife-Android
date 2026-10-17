package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.data.maps.VexMapsAppearancePreference
import org.junit.Assert.assertSame
import org.junit.Test

class VexMapsAppearancePreferenceTest {
    @Test
    fun automaticFollowsCurrentAppAppearance() {
        assertSame(
            VexMapsOnlineStyle.LIBERTY,
            VexMapsAppearancePreference
                .AUTOMATIC
                .resolveVexMapsOnlineStyle(
                    darkTheme = false,
                ),
        )
        assertSame(
            VexMapsOnlineStyle.DARK,
            VexMapsAppearancePreference
                .AUTOMATIC
                .resolveVexMapsOnlineStyle(
                    darkTheme = true,
                ),
        )
    }

    @Test
    fun explicitAppearanceIgnoresAppAppearanceChanges() {
        assertSame(
            VexMapsOnlineStyle.LIBERTY,
            VexMapsAppearancePreference
                .LIGHT
                .resolveVexMapsOnlineStyle(
                    darkTheme = true,
                ),
        )
        assertSame(
            VexMapsOnlineStyle.DARK,
            VexMapsAppearancePreference
                .DARK
                .resolveVexMapsOnlineStyle(
                    darkTheme = false,
                ),
        )
    }
}

// [VXG RealForever]
