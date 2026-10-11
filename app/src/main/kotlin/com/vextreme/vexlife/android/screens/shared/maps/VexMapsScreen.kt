package com.vextreme.vexlife.android.screens.shared.maps

import androidx.compose.runtime.Composable
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.preview.VexBlueprintScreenPreview
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint

/**
 * Home-level VexMaps destination.
 *
 * This first slice intentionally owns only the registered Screen destination. Map/search/route data,
 * device location, active navigation lifetime, and guidance remain separate concerns that will attach
 * through their rightful owners as each capability is earned.
 */
object VexMapsScreen : AppScreenBlueprint(
    screenKey = "vex-maps",
    titleRes = R.string.vexmaps_title,
) {
    override val content = layout {
        scrollableColumn("body") {
            card("foundation") {
                text(
                    id = "title",
                    textRes = R.string.vexmaps_foundation_title,
                    role = TextRole.Title,
                )
                text(
                    id = "message",
                    textRes = R.string.vexmaps_foundation_body,
                    role = TextRole.Supporting,
                )
            }
        }
    }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        darkTheme: Boolean,
    ) {
        VexMapsMapSurface(
            darkTheme = darkTheme,
        )
    }
}

@VexScreenPreviews
@Composable
private fun VexMapsScreenPreview() {
    VexBlueprintScreenPreview(VexMapsScreen)
}

// [VXG RealForever]
