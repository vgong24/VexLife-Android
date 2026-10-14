package com.vextreme.vexlife.android.screens.shared.debug

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexPreviewRepositories
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexElementTree
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.elements.localEffect
import com.vextreme.vexlife.android.elements.open
import com.vextreme.vexlife.android.data.events.SyntheticExternalEventController
import com.vextreme.vexlife.android.data.loghistory.LogHistoryFixture
import com.vextreme.vexlife.android.data.loghistory.LogHistoryRepository
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.ScreenActionContext
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.primary.events.traditionalApproach.EventsArchitectureComparisonLab
import com.vextreme.vexlife.android.screens.primary.events.traditionalApproach.EventsArchitectureMode
import com.vextreme.vexlife.android.screens.shared.debug.loghistory.LogHistorySheet
import com.vextreme.vexlife.android.shell.AppShell
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.appAction
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.ui.VexLifeRelays

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

    val UseTraditionalEventsArchitecture = toggleRef(
        id = "body.events-architecture.use-traditional",
        labelRes = R.string.events_architecture_use_traditional,
        intent = localEffect(),
    )

    val EventsArchitectureComparisonStatus = textFieldRef(
        id = "body.events-architecture.live-comparison",
        labelRes = R.string.events_architecture_live_comparison,
        readOnly = true,
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

    val ResetSyntheticEventLab = buttonRef(
        id = "body.events-simulator.actions.reset-lab",
        labelRes = R.string.events_simulator_reset_lab,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Reset,
        intent = invoke(AppActions.ResetSyntheticEventLab),
    )

    val UseRotationAttentionLab = buttonRef(
        id = "body.events-simulator.actions.use-rotation",
        labelRes = R.string.events_simulator_use_rotation,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.UseRotationAttentionLab),
    )

    val UseRandomAttentionLab = buttonRef(
        id = "body.events-simulator.actions.use-random",
        labelRes = R.string.events_simulator_use_random,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.UseRandomAttentionLab),
    )

    val NextAttentionLabScenario = buttonRef(
        id = "body.events-simulator.actions.next-scenario",
        labelRes = R.string.events_simulator_next_scenario,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.NextAttentionLabScenario),
    )

    override val content = layout {
        scrollableColumn("body") {
            toggle(DarkMode)

            card("events-architecture") {
                text(
                    id = "title",
                    textRes = R.string.events_architecture_comparison_title,
                    role = TextRole.Title,
                )
                toggle(UseTraditionalEventsArchitecture)
                textField(EventsArchitectureComparisonStatus)
                text(
                    id = "helper",
                    textRes = R.string.events_architecture_comparison_helper,
                    role = TextRole.Supporting,
                )
            }

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
                row("reset-actions") {
                    button(ResetSyntheticEventLab)
                }
                row("scenario-mode-actions") {
                    button(UseRotationAttentionLab)
                    button(UseRandomAttentionLab)
                }
                row("scenario-actions") {
                    button(NextAttentionLabScenario)
                }
                text(
                    id = "helper",
                    textRes = R.string.events_simulator_helper,
                    role = TextRole.Supporting,
                )
            }

            card("events-simulator-guide") {
                text(
                    id = "title",
                    textRes = R.string.events_simulator_guide_title,
                    role = TextRole.Title,
                )
                text(
                    id = "body",
                    textRes = R.string.events_simulator_guide_body,
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
        val eventsAttention =
            context.repositories.eventsAttention
        val eventsAttentionLab =
            context.repositories.eventsAttentionLab
        val events =
            context.repositories.events

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
            appAction(
                action = AppActions.ResetSyntheticEventLab,
                ownerRef = screenId,
            ) {
                syntheticEvents.stop()
                syntheticEvents.resetCurrent()
                eventsAttention.resetAll()
                eventsAttentionLab.reset()
            },
            appAction(
                action = AppActions.UseRotationAttentionLab,
                ownerRef = screenId,
            ) {
                eventsAttentionLab.useRotation()
            },
            appAction(
                action = AppActions.UseRandomAttentionLab,
                ownerRef = screenId,
            ) {
                eventsAttentionLab.useSeededRandom(
                    System.currentTimeMillis(),
                )
            },
            appAction(
                action = AppActions.NextAttentionLabScenario,
                ownerRef = screenId,
            ) {
                if (events.current.events.size < 12) {
                    syntheticEvents.emitBurst(
                        12 - events.current.events.size,
                    )
                }

                eventsAttention.resetAll()

                val scenario =
                    eventsAttentionLab.planNext(
                        events.current.events,
                    )

                if (scenario.freezeProjection) {
                    syntheticEvents.start()
                }
            },
        )
    }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        relays: VexLifeRelays,
        darkTheme: Boolean,
    ) {
        DebugMenuScreenContent(
            runtime = runtime,
            logHistoryRepository = repositories.logHistory,
            eventsArchitectureComparison = relays.eventsComparison,
            darkTheme = darkTheme,
        )
    }
}

@Composable
fun DebugMenuScreenContent(
    runtime: VexRuntime,
    logHistoryRepository: LogHistoryRepository,
    eventsArchitectureComparison: EventsArchitectureComparisonLab,
    darkTheme: Boolean,
) {
    val architectureMode by
        eventsArchitectureComparison
            .mode
            .collectAsState()

    val architectureComparison by
        eventsArchitectureComparison
            .comparisonStates
            .collectAsState()

    val handler =
        ElementActionHandler { element ->
            if (
                element ===
                DebugMenuScreen
                    .UseTraditionalEventsArchitecture
            ) {
                runtime.activate(element)

                if (
                    architectureMode ==
                    EventsArchitectureMode
                        .TRADITIONAL_CONTROLLER
                ) {
                    eventsArchitectureComparison
                        .useVextremeRelay()
                } else {
                    eventsArchitectureComparison
                        .useTraditionalController()
                }
            } else {
                runtime.activate(element)
            }
        }

    VexElementTree(
        placement = DebugMenuScreen.content,
        registry = runtime.registry,
        handler = handler,
        modifier = Modifier.fillMaxSize(),
        toggleChecked = { element ->
            when (element.elementId) {
                DebugMenuScreen.DarkMode.elementId ->
                    darkTheme

                DebugMenuScreen
                    .UseTraditionalEventsArchitecture
                    .elementId ->
                    architectureMode ==
                        EventsArchitectureMode
                            .TRADITIONAL_CONTROLLER

                else ->
                    false
            }
        },
        textFieldValue = { element ->
            when (element.elementId) {
                DebugMenuScreen
                    .EventsArchitectureComparisonStatus
                    .elementId ->
                    architectureComparison.diagnosticSummary

                else ->
                    ""
            }
        },
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
            eventsArchitectureComparison = context.relays.eventsComparison,
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
            eventsArchitectureComparison = context.relays.eventsComparison,
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
