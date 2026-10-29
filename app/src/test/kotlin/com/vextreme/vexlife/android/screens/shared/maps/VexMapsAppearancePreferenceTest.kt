package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.data.maps.VexMapsAppearancePreference
import com.vextreme.vexlife.android.ui.VexThemePresentation
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
                    themePresentation =
                        VexThemePresentation.Light,
                ),
        )
        assertSame(
            VexMapsOnlineStyle.DARK,
            VexMapsAppearancePreference
                .AUTOMATIC
                .resolveVexMapsOnlineStyle(
                    themePresentation =
                        VexThemePresentation.Dark,
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
                    themePresentation =
                        VexThemePresentation.Dark,
                ),
        )
        assertSame(
            VexMapsOnlineStyle.DARK,
            VexMapsAppearancePreference
                .DARK
                .resolveVexMapsOnlineStyle(
                    themePresentation =
                        VexThemePresentation.Light,
                ),
        )
    }
}

// [VXG RealForever]
