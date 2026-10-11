package com.vextreme.vexlife.android.pages.shared.debug

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.PageBlueprint
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.elements.close
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.elements.open
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.pages.shared.debug.loghistory.LogHistoryDialog
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.VexRuntime

object DebugMenuPage : PageBlueprint(
    pageId = "debug-menu",
    titleRes = R.string.debug_menu_title,
) {
    val LogDialog = dialogRef("log-history.dialog")

    val DarkMode = toggleRef(
        id = "body.dark-mode",
        labelRes = R.string.dark_mode,
        intent = invoke(AppActions.ToggleDarkMode),
    )

    val ShowLog = buttonRef(
        id = "body.log-history.actions.show",
        labelRes = R.string.log_history_show,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.History,
        intent = open(LogDialog),
    )

    val ResetLog = buttonRef(
        id = "body.log-history.actions.reset",
        labelRes = R.string.log_history_reset,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Reset,
        intent = invoke(AppActions.ResetLogSection),
    )

    val CloseLog = buttonRef(
        id = "body.log-history.dialog.close",
        labelRes = R.string.close,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Close,
        intent = close(LogDialog),
    )

    override val content = layout {
        scrollableColumn("body") {
            toggle(DarkMode)

            card("log-history") {
                text(
                    id = "title",
                    textRes = R.string.log_history,
                    role = TextRole.Title,
                )
                row("actions") {
                    button(ShowLog)
                    button(ResetLog)
                }
                text(
                    id = "helper",
                    textRes = R.string.log_history_helper,
                    role = TextRole.Supporting,
                )
            }

            dialog(LogDialog) {
                button(CloseLog)
            }
        }
    }
}

@Composable
fun DebugMenuPageContent(
    runtime: VexRuntime,
    logger: VexLogger,
    darkTheme: Boolean,
) {
    VexElementTree(
        placement = DebugMenuPage.content,
        registry = runtime.registry,
        handler = runtime,
        modifier = Modifier.fillMaxSize(),
        toggleChecked = { it.elementId == DebugMenuPage.DarkMode.elementId && darkTheme },
        selectedChild = runtime.presentation::selected,
    )

    if (runtime.presentation.isOpen(DebugMenuPage.LogDialog)) {
        LogHistoryDialog(
            runtime = runtime,
            logger = logger,
        )
    }
}
