package com.vextreme.vexlife.android.screens.shared.maps

import androidx.annotation.StringRes
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.maps.VexMapsAppearancePreference
import com.vextreme.vexlife.android.ui.VexThemePresentation

internal enum class VexMapsOnlineStyle(
    val styleRef: String,
    @StringRes val labelRes: Int,
    val styleUri: String,
) {
    DARK(
        styleRef = "dark",
        labelRes = R.string.vexmaps_style_dark,
        styleUri =
            "https://tiles.openfreemap.org/styles/dark",
    ),
    LIBERTY(
        styleRef = "liberty",
        labelRes = R.string.vexmaps_style_liberty,
        styleUri =
            "https://tiles.openfreemap.org/styles/liberty",
    ),
    ;

}

internal fun VexMapsAppearancePreference.resolveVexMapsOnlineStyle(
    themePresentation: VexThemePresentation,
): VexMapsOnlineStyle =
    when (this) {
        VexMapsAppearancePreference.AUTOMATIC ->
            when (themePresentation) {
                VexThemePresentation.Unresolved ->
                    error(
                        "Automatic VexMaps appearance requires resolved Vex theme presentation",
                    )

                VexThemePresentation.Light ->
                    VexMapsOnlineStyle.LIBERTY

                VexThemePresentation.Dark ->
                    VexMapsOnlineStyle.DARK
            }

        VexMapsAppearancePreference.LIGHT ->
            VexMapsOnlineStyle.LIBERTY

        VexMapsAppearancePreference.DARK ->
            VexMapsOnlineStyle.DARK
    }

// [VXG RealForever]
