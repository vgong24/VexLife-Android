package com.vextreme.vexlife.android.screens.shared.debug

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexPreviewRepositories
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.elements.open
import com.vextreme.vexlife.android.data.events.SyntheticExternalEventController
import com.vextreme.vexlife.android.data.loghistory.LogHistoryFixture
import com.vextreme.vexlife.android.data.loghistory.LogHistoryRepository
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.ScreenActionContext
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.debug.loghistory.LogHistorySheet
import com.vextreme.vexlife.android.shell.AppShell
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.appAction
import com.vextreme.vexlife.android.runtime.VexRuntime

object DebugMenuScreen : AppScreenBlueprint(
    screenKey = "debug-menu",
    titleRes = R.string.debug_menu_title,
) {
    val LogSheet = modalBottomSheetRef("log-history.sheet")

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
        intent = open(LogSheet),
    )

    val ResetLog = buttonRef(
        id = "body.log-history.actions.reset",
        labelRes = R.string.log_history_reset,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Reset,
        intent = invoke(AppActions.ClearLogHistory),
    )

    val StartSyntheticEvents = buttonRef(
        id = "body.events-simulator.actions.start",
        labelRes = R.string.events_simulator_start,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.StartSyntheticEvents),
    )

    val StopSyntheticEvents = buttonRef(
        id = "body.events-simulator.actions.stop",
        labelRes = R.string.events_simulator_stop,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.StopSyntheticEvents),
    )

    val EmitSyntheticEvent = buttonRef(
        id = "body.events-simulator.actions.emit-next",
        labelRes = R.string.events_simulator_emit_next,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.EmitSyntheticEvent),
    )

    val EmitSyntheticEventBurst = buttonRef(
        id = "body.events-simulator.actions.emit-burst",
        labelRes = R.string.events_simulator_emit_burst,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.EmitSyntheticEventBurst),
    )

    val InsertHistoricalSyntheticEvent = buttonRef(
        id = "body.events-simulator.actions.insert-historical",
        labelRes = R.string.events_simulator_insert_historical,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.InsertHistoricalSyntheticEvent),
    )

    val RemoveOldestSyntheticEvent = buttonRef(
        id = "body.events-simulator.actions.remove-oldest",
        labelRes = R.string.events_simulator_remove_oldest,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.RemoveOldestSyntheticEvent),
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

            card("events-simulator") {
                text(
                    id = "title",
                    textRes = R.string.events_simulator_title,
                    role = TextRole.Title,
                )
                row("timer-actions") {
                    button(StartSyntheticEvents)
                    button(StopSyntheticEvents)
                }
                row("emit-actions") {
                    button(EmitSyntheticEvent)
                    button(EmitSyntheticEventBurst)
                }
                row("mutation-actions") {
                    button(InsertHistoricalSyntheticEvent)
                    button(RemoveOldestSyntheticEvent)
                }
                text(
                    id = "helper",
                    textRes = R.string.events_simulator_helper,
                    role = TextRole.Supporting,
                )
            }

            modalBottomSheet(LogSheet) {}
        }
    }

    internal override fun appActionRegistrations(
        context: ScreenActionContext,
    ): AppActionRegistrations {
        // Capture only the dependencies these registrations need. ScreenActionContext is a
        // construction-time bag, not something action closures should retain wholesale.
        val toggleDarkMode = context.toggleDarkMode
        val logger = context.logger
        val logPersistence = context.logPersistence
        val syntheticEvents =
            SyntheticExternalEventController(
                repository = context.repositories.events,
                scope = context.scope,
            )

        return AppActionRegistrations.of(
            appAction(
                action = AppActions.ToggleDarkMode,
                ownerRef = screenId,
            ) {
                toggleDarkMode()
            },
            appAction(
                action = AppActions.ClearLogHistory,
                ownerRef = screenId,
            ) {
                logger.clearHistory()
                logPersistence.clear()
            },
            appAction(
                action = AppActions.StartSyntheticEvents,
                ownerRef = screenId,
            ) {
                syntheticEvents.start()
            },
            appAction(
                action = AppActions.StopSyntheticEvents,
                ownerRef = screenId,
            ) {
                syntheticEvents.stop()
            },
            appAction(
                action = AppActions.EmitSyntheticEvent,
                ownerRef = screenId,
            ) {
                syntheticEvents.emitNext()
            },
            appAction(
                action = AppActions.EmitSyntheticEventBurst,
                ownerRef = screenId,
            ) {
                syntheticEvents.emitBurst()
            },
            appAction(
                action = AppActions.InsertHistoricalSyntheticEvent,
                ownerRef = screenId,
            ) {
                syntheticEvents.insertHistorical()
            },
            appAction(
                action = AppActions.RemoveOldestSyntheticEvent,
                ownerRef = screenId,
            ) {
                syntheticEvents.removeOldest()
            },
        )
    }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        darkTheme: Boolean,
    ) {
        DebugMenuScreenContent(
            runtime = runtime,
            logHistoryRepository = repositories.logHistory,
            darkTheme = darkTheme,
        )
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

    if (runtime.presentation.isOpen(DebugMenuScreen.LogSheet)) {
        LogHistorySheet(
            runtime = runtime,
            repository = logHistoryRepository,
        )
    }
}

/**
 * Normal Debug screen plus an edge preview with persisted history and the sheet open.
 */
@VexScreenPreviews
@Composable
private fun DebugMenuScreenPreview() {
    VexPreviewHost(
        id = "debug-menu",
        startScreen = DebugMenuScreen,
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
        id = "debug-history-open",
        startScreen = DebugMenuScreen,
        repositoriesFactory = {
            VexPreviewRepositories.withLogHistory(
                debugHistoryPreviewFixture(),
            )
        },
        previewSetup = {
            open(DebugMenuScreen.LogSheet)
        },
    ) { context ->
        DebugMenuScreenContent(
            runtime = context.runtime,
            logHistoryRepository = context.repositories.logHistory,
            darkTheme = context.darkTheme,
        )
    }
}


/**
 * Preview data belongs beside DebugMenuScreen because it describes a meaningful state of THIS screen.
 * The shared preview layer only knows how to host repositories/runtime; it does not know Debug's
 * sheet/history scenario.
 */
private fun debugHistoryPreviewFixture(): LogHistoryFixture =
    LogHistoryFixture.build {
        appOpened(HomeScreen)
        afterSeconds(120)
        click(
            element = AppShell.Elements.DebugMenu,
            from = HomeScreen,
            to = DebugMenuScreen,
        )
    }.append {
        afterSeconds(30)
        click(
            element = DebugMenuScreen.ResetLog,
            from = DebugMenuScreen,
        )
    }
