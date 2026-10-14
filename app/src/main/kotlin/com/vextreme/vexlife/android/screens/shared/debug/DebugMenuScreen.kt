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
import com.vextreme.vexlife.android.data.events.EventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.EventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.EventsRepository
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
                /*
                 * This Debug action means "prepare one complete Events attention exercise".
                 *
                 * Keep the multi-owner fixture choreography out of this registration lambda so the
                 * action map stays readable. The helper below documents every condition/effect.
                 */
                prepareNextEventsAttentionExercise(
                    events = events,
                    syntheticEvents = syntheticEvents,
                    attentionPreferences = eventsAttention,
                    attentionLab = eventsAttentionLab,
                )
            },
        )
    }

    /**
     * Prepare one complete Events developer-lab attention exercise.
     *
     * This is Debug-menu orchestration, not reusable Relay/Scenario infrastructure. It coordinates
     * several rightful owners only because one human Debug action needs a repeatable environment.
     *
     * Read in order:
     *
     * 1. ENSURE ROW DEPTH
     *    The attention lab exercises top/middle/bottom anchored targets, so keep at least
     *    [ATTENTION_EXERCISE_MIN_EVENT_COUNT] current Events rows available.
     *
     * 2. RESET HUMAN DISPOSITION
     *    Clear acknowledge/defer/suspend/suppress state so an older lab run cannot hide the row the
     *    next exercise intends to target.
     *
     * 3. PLAN AGAINST REFRESHED EVENT TRUTH
     *    [SyntheticExternalEventController.emitBurst] publishes through EventsRepository
     *    synchronously before returning, so the refreshed [EventsRepository.current] read includes
     *    any rows just added above.
     *
     * 4. APPLY OPTIONAL PROVIDER PRESSURE
     *    A freeze-projection exercise deliberately keeps new provider events arriving while the
     *    overlay is active. Starting the synthetic producer tests that presentation remains stable
     *    while canonical feed truth continues changing.
     *
     * Consequences:
     * - may add synthetic provider events;
     * - resets Events attention-preference state;
     * - commits one current attention-lab exercise;
     * - may start timed synthetic provider arrivals.
     *
     * These owners commit independently. EventsConvergenceRelay observes their StateFlows and
     * converges them; this helper does NOT claim one cross-repository atomic transaction.
     */
    private fun prepareNextEventsAttentionExercise(
        events: EventsRepository,
        syntheticEvents: SyntheticExternalEventController,
        attentionPreferences: EventsAttentionPreferenceRepository,
        attentionLab: EventsAttentionLabRepository,
    ) {
        // 1. ENSURE ROW DEPTH
        val currentEventCount =
            events.current.events.size

        if (
            currentEventCount <
                ATTENTION_EXERCISE_MIN_EVENT_COUNT
        ) {
            syntheticEvents.emitBurst(
                ATTENTION_EXERCISE_MIN_EVENT_COUNT -
                    currentEventCount,
            )
        }

        // 2. RESET HUMAN DISPOSITION
        attentionPreferences.resetAll()

        // 3. PLAN AGAINST REFRESHED EVENT TRUTH
        //
        // "exercise" is intentionally used here instead of "scenario" to make its meaning local:
        // this is one developer-lab overlay exercise, not a generic Scenario subsystem.
        val exercise =
            attentionLab.planNext(
                events.current.events,
            )

        // 4. APPLY OPTIONAL PROVIDER PRESSURE
        if (exercise.freezeProjection) {
            syntheticEvents.start()
        }
    }

    private const val ATTENTION_EXERCISE_MIN_EVENT_COUNT: Int =
        12

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
