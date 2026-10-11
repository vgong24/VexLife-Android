package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.ScreenActionContext
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.runtime.appAction

/**
 * Placeholder destination for the future VexStream Music relationship.
 *
 * This screen intentionally does not invent streaming transport, pairing, or service state yet.
 * It proves that Home can navigate to a stable destination and that feature-specific Invoke effects
 * stay registered beside the feature instead of accumulating in VexLifeApp.
 */
object VexStreamMusicScreen : AppScreenBlueprint(
    screenKey = "vexstream-music",
    titleRes = R.string.vexstream_music_title,
) {
    val PlaceholderAction = buttonRef(
        id = "body.placeholder.actions.run",
        labelRes = R.string.vexstream_music_placeholder_action,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.VexStreamMusicPlaceholder),
    )

    override val content = layout {
        scrollableColumn("body") {
            card("placeholder") {
                text(
                    id = "title",
                    textRes = R.string.vexstream_music_placeholder_title,
                    role = TextRole.Title,
                )
                text(
                    id = "message",
                    textRes = R.string.vexstream_music_placeholder_body,
                    role = TextRole.Supporting,
                )
                button(PlaceholderAction)
            }
        }
    }

    internal override fun appActionRegistrations(
        context: ScreenActionContext,
    ): AppActionRegistrations =
        AppActionRegistrations.of(
            appAction(
                action = AppActions.VexStreamMusicPlaceholder,
                ownerRef = screenId,
            ) {
                // No product effect yet.
            },
        )
}

@Composable
fun VexStreamMusicScreenContent(
    runtime: VexRuntime,
) {
    VexElementTree(
        placement = VexStreamMusicScreen.content,
        registry = runtime.registry,
        handler = runtime,
        modifier = Modifier.fillMaxSize(),
        selectedChild = runtime.presentation::selected,
    )
}

@VexScreenPreviews
@Composable
private fun VexStreamMusicScreenPreview() {
    VexPreviewHost(
        id = "vexstream-music",
        startScreen = VexStreamMusicScreen,
    ) { context ->
        VexStreamMusicScreenContent(context.runtime)
    }
}
