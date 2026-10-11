package com.vextreme.vexlife.android.screens.shared.vex

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
 * Placeholder destination for the future conversational Vex relationship.
 *
 * This screen does not invent model/session/voice lifecycle yet. It exists as a stable UI
 * destination and demonstrates that its Invoke effect stays scoped beside the feature source.
 */
object TalkToVexScreen : AppScreenBlueprint(
    screenKey = "talk-to-vex",
    titleRes = R.string.talk_to_vex_title,
) {
    val PlaceholderAction = buttonRef(
        id = "body.placeholder.actions.run",
        labelRes = R.string.talk_to_vex_placeholder_action,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.TalkToVexPlaceholder),
    )

    override val content = layout {
        scrollableColumn("body") {
            card("placeholder") {
                text(
                    id = "title",
                    textRes = R.string.talk_to_vex_placeholder_title,
                    role = TextRole.Title,
                )
                text(
                    id = "message",
                    textRes = R.string.talk_to_vex_placeholder_body,
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
                action = AppActions.TalkToVexPlaceholder,
                ownerRef = screenId,
            ) {
                // No product effect yet.
            },
        )
}

@Composable
fun TalkToVexScreenContent(
    runtime: VexRuntime,
) {
    VexElementTree(
        placement = TalkToVexScreen.content,
        registry = runtime.registry,
        handler = runtime,
        modifier = Modifier.fillMaxSize(),
        selectedChild = runtime.presentation::selected,
    )
}

@VexScreenPreviews
@Composable
private fun TalkToVexScreenPreview() {
    VexPreviewHost(
        id = "talk-to-vex",
        startScreen = TalkToVexScreen,
    ) { context ->
        TalkToVexScreenContent(context.runtime)
    }
}
