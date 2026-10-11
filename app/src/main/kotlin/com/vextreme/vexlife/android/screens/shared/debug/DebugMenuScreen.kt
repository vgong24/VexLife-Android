package com.vextreme.vexlife.android.screens.shared.debug

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexPreviewScenario
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.ScreenBlueprint
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.elements.close
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.elements.open
import com.vextreme.vexlife.android.data.loghistory.LogHistoryPersistenceQueue
import com.vextreme.vexlife.android.data.loghistory.LogHistoryRepository
import com.vextreme.vexlife.android.screens.shared.debug.loghistory.LogHistoryDialog
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.appAction
import com.vextreme.vexlife.android.runtime.VexRuntime

object DebugMenuScreen : ScreenBlueprint(
    screenKey = "debug-menu",
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
        intent = invoke(AppActions.ClearLogHistory),
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
fun DebugMenuScreenContent(
    runtime: VexRuntime,
    logHistoryRepository: LogHistoryRepository,
    darkTheme: Boolean,
) {
    VexElementTree(
        placement = DebugMenuScreen.content,
        registry = runtime.registry,
        handler = runtime,
        modifier = Modifier.fillMaxSize(),
        toggleChecked = { it.elementId == DebugMenuScreen.DarkMode.elementId && darkTheme },
        selectedChild = runtime.presentation::selected,
    )

    if (runtime.presentation.isOpen(DebugMenuScreen.LogDialog)) {
        LogHistoryDialog(
            runtime = runtime,
            repository = logHistoryRepository,
        )
    }
}

/**
 * Semantic effects triggered by DebugMenuScreen.
 *
 * These registrations stay beside the Screen that declares the actions. The app root supplies only
 * the dependencies whose lifetime is broader (theme setter, logger, persistence queue).
 */
internal fun debugMenuActionRegistrations(
    toggleDarkMode: () -> Unit,
    logger: VexLogger,
    logPersistence: LogHistoryPersistenceQueue,
): AppActionRegistrations =
    AppActionRegistrations.of(
        appAction(
            action = AppActions.ToggleDarkMode,
            ownerRef = DebugMenuScreen.screenId,
        ) {
            toggleDarkMode()
        },
        appAction(
            action = AppActions.ClearLogHistory,
            ownerRef = DebugMenuScreen.screenId,
        ) {
            logger.clearHistory()
            logPersistence.clear()
        },
    )

/**
 * Normal Debug screen plus an edge preview with persisted history and the dialog open.
 */
@VexScreenPreviews
@Composable
private fun DebugMenuScreenPreview() {
    VexPreviewHost(
        scenario = VexPreviewScenario(
            id = "debug-menu",
            startScreen = DebugMenuScreen,
        ),
    ) { context ->
        DebugMenuScreenContent(
            runtime = context.runtime,
            logHistoryRepository = context.repositories.logHistory,
            darkTheme = context.darkTheme,
        )
    }
}

@VexScreenPreviews
@Composable
private fun DebugMenuHistoryPreview() {
    VexPreviewHost(
        scenario = VexPreviewScenario.debugHistoryOpen(),
    ) { context ->
        DebugMenuScreenContent(
            runtime = context.runtime,
            logHistoryRepository = context.repositories.logHistory,
            darkTheme = context.darkTheme,
        )
    }
}
