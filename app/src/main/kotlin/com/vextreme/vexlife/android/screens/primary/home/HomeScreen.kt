package com.vextreme.vexlife.android.screens.primary.home

import androidx.compose.runtime.Composable
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.preview.VexBlueprintScreenPreview
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.navigateTo
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen

object HomeScreen : AppScreenBlueprint(
    screenKey = "home",
    titleRes = R.string.home_title,
) {
    /**
     * These are runtime-owned navigation actions, not AppActionRegistrations.
     *
     * The destination Screens each contain their own Invoke action so this file demonstrates the
     * difference between "navigate somewhere" and "ask a feature owner to perform an effect."
     */
    val VexStreamMusic = buttonRef(
        id = "body.systems.vexstream-music",
        labelRes = R.string.home_vexstream_music,
        style = VexElementStyles.SecondaryAction,
        intent = navigateTo(VexStreamMusicScreen),
    )

    val TalkToVex = buttonRef(
        id = "body.systems.talk-to-vex",
        labelRes = R.string.home_talk_to_vex,
        style = VexElementStyles.SecondaryAction,
        intent = navigateTo(TalkToVexScreen),
    )

    override val content = layout {
        scrollableColumn("body") {
            card("preview") {
                text(
                    id = "title",
                    textRes = R.string.home_preview_title,
                    role = TextRole.Title,
                )
                text(
                    id = "message",
                    textRes = R.string.home_preview_body,
                    role = TextRole.Supporting,
                )
            }

            card("systems") {
                text(
                    id = "title",
                    textRes = R.string.home_systems_title,
                    role = TextRole.Title,
                )
                button(VexStreamMusic)
                button(TalkToVex)
            }
        }
    }
}


@VexScreenPreviews
@Composable
private fun HomeScreenPreview() {
    VexBlueprintScreenPreview(HomeScreen)
}
