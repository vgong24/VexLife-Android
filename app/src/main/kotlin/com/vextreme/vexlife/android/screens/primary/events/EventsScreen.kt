package com.vextreme.vexlife.android.screens.primary.events

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.events.EventsAlertDisposition
import com.vextreme.vexlife.android.data.events.EventsDebugMenuAlertExpectedAction
import com.vextreme.vexlife.android.data.events.EventsDebugMenuAlertPlacement
import com.vextreme.vexlife.android.data.events.EventsDebugMenuAlertCase
import com.vextreme.vexlife.android.data.events.RelayEventsRepository
import com.vextreme.vexlife.android.data.events.VexExternalEvent
import com.vextreme.vexlife.android.data.events.VexExternalEventKind
import com.vextreme.vexlife.android.data.events.VexExternalEventSourceSnapshot
import com.vextreme.vexlife.android.elements.VexAnchoredOverlay
import com.vextreme.vexlife.android.elements.VexStableLazyColumn
import com.vextreme.vexlife.android.elements.VexOverlayPlacement
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexPreviewRepositories
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.ui.VexLifeRelays

object EventsScreen : AppScreenBlueprint(
    screenKey = "events",
    titleRes = R.string.events_title,
) {
    override val content = layout {
        scrollableColumn("body") {
            text(
                id = "empty",
                textRes = R.string.events_preview_body,
            )
        }
    }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        relays: VexLifeRelays,
        darkTheme: Boolean,
    ) {
        EventsScreenContent(
            runtime = runtime,
            relay = relays.events,
        )
    }
}

/**
 * Dynamic Events realization.
 *
 * The Screen owns event meaning and convergence policy; [VexStableLazyColumn] owns only Android lazy
 * list identity/visibility mechanics. This split is deliberate: a future list-based feature can
 * reuse stable repeated-item behavior without inheriting Events repositories or convergence rules.
 */
@Composable
fun EventsScreenContent(
    runtime: VexRuntime,
    relay: EventsConvergenceRelay,
) {
    val convergence by relay.outputStates.collectAsState(
        initial = relay.currentOutput,
    )
    val debugMenuAlertCase =
        convergence.debugMenuAlertCase

    val activeCandidateRef =
        convergence.candidate?.eventRef
    val visibleCandidateRef =
        convergence
            .takeIf { output ->
                output.availability ==
                    EventsConvergenceAvailability.READY
            }
            ?.candidate
            ?.eventRef

    /*
     * Keep one presentation snapshot while the same convergence candidate is active. Canonical
     * provider truth and Relay queue counts continue to advance underneath it, but five-second
     * arrivals cannot insert/reorder rows under the anchored popup. A candidate change refreshes the
     * snapshot from current provider truth.
     *
     * BLOCKING_FREEZE_TOP adds user-scroll lock and acknowledge-only dismissal on top of this common
     * anti-flicker presentation hold.
     */
    var presentationEvents by
        remember {
            mutableStateOf<List<VexExternalEvent>?>(null)
        }

    LaunchedEffect(
        activeCandidateRef,
        debugMenuAlertCase?.alertCaseRef,
    ) {
        presentationEvents =
            if (activeCandidateRef == null) {
                null
            } else {
                convergence.events
            }
    }

    val displayedEvents =
        presentationEvents ?: convergence.events

    VexStableLazyColumn(
        listRef = EventsScreen.content.element.elementId,
        items = displayedEvents,
        itemRef = VexExternalEvent::eventRef,
        contentType = { "events-event-row" },
        requestedVisibleItemRef =
            debugMenuAlertCase?.targetEventRef,
        userScrollEnabled =
            debugMenuAlertCase?.blockingAcknowledgeOnly != true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        onVisibleItemRefsChanged =
            relay::visibleTargetsChanged,
        emptyContent = {
            when (convergence.list) {
                EventsConvergenceListState.Unobserved ->
                    Unit

                EventsConvergenceListState.Empty ->
                    Text(
                        text = stringResource(R.string.events_preview_body),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                is EventsConvergenceListState.Content ->
                    Unit
            }
        },
    ) { event ->
        EventRow(
            event = event,
            isAlertCandidate =
                visibleCandidateRef == event.eventRef,
            disposition =
                convergence.dispositionByEventRef[
                    event.eventRef
                ],
            convergence = convergence,
            debugMenuAlertCase = debugMenuAlertCase,
            onAcknowledge = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.alertAction(
                        event.eventRef,
                        "acknowledge",
                    ),
                )
                relay.acknowledge(
                    event.eventRef,
                )
                if (debugMenuAlertCase?.targetEventRef == event.eventRef) {
                    relay.completeCurrentAlertCase()
                }
            },
            onDefer = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.alertAction(
                        event.eventRef,
                        "later",
                    ),
                )
                relay.later(
                    event.eventRef,
                )
                if (debugMenuAlertCase?.targetEventRef == event.eventRef) {
                    relay.completeCurrentAlertCase()
                }
            },
            onSuspend = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.alertAction(
                        event.eventRef,
                        "suspend",
                    ),
                )
                relay.suspend(
                    event.eventRef,
                )
                if (debugMenuAlertCase?.targetEventRef == event.eventRef) {
                    relay.completeCurrentAlertCase()
                }
            },
            onSuppress = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.alertAction(
                        event.eventRef,
                        "suppress",
                    ),
                )
                relay.suppress(
                    event.eventRef,
                )
                if (debugMenuAlertCase?.targetEventRef == event.eventRef) {
                    relay.completeCurrentAlertCase()
                }
            },
            onDismiss = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.overlay(
                        event.eventRef,
                    ),
                    action = "Dismissed",
                )
                relay.later(
                    event.eventRef,
                )
                if (debugMenuAlertCase?.targetEventRef == event.eventRef) {
                    relay.completeCurrentAlertCase()
                }
            },
        )
    }
}

/**
 * Events-specific row composition.
 *
 * Keeping this beside Events is intentional: kind/title/detail/disposition and its convergence actions
 * are product meaning, not generic list-cell behavior. The reusable list only asks this row for one
 * stable [VexExternalEvent.eventRef].
 */
@Composable
private fun EventRow(
    event: VexExternalEvent,
    isAlertCandidate: Boolean,
    disposition: EventsAlertDisposition?,
    convergence: EventsConvergenceOutputState,
    alertCase: EventsDebugMenuAlertCase?,
    onAcknowledge: () -> Unit,
    onDefer: () -> Unit,
    onSuspend: () -> Unit,
    onSuppress: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (isAlertCandidate) {
                        Modifier.border(
                            width = 2.dp,
                            color =
                                MaterialTheme.colorScheme.primary,
                            shape =
                                MaterialTheme.shapes.medium,
                        )
                    } else {
                        Modifier
                    },
                )
                .testTag(
                    EventsPresentationRefs.row(
                        event.eventRef,
                    ),
                ),
    ) {
        Card(
            modifier =
                Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = event.kind.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = event.detail,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = event.sourceRef,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = event.eventRef,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                disposition?.let {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (isAlertCandidate) {
            val preferredPlacements =
                alertCase
                    ?.preferredOverlayPlacements()
                    ?: listOf(
                        VexOverlayPlacement.BELOW,
                        VexOverlayPlacement.ABOVE,
                        VexOverlayPlacement.END,
                        VexOverlayPlacement.START,
                    )
            val blocking =
                debugMenuAlertCase?.blockingAcknowledgeOnly == true
            val expectedAction =
                debugMenuAlertCase?.expectedAction

            VexAnchoredOverlay(
                presentationRef =
                    EventsPresentationRefs.overlay(
                        event.eventRef,
                    ),
                onDismissRequest = {
                    if (debugMenuAlertCase == null) {
                        onDismiss()
                    }
                },
                preferredPlacements =
                    preferredPlacements,
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.events_alert_candidate,
                        ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.inversePrimary,
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text =
                        stringResource(
                            R.string.events_alert_compact_queue,
                            convergence.unhandledCount,
                            convergence.currentQueueCount,
                            convergence.nextSequenceCount,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                )

                debugMenuAlertCase?.let { debugMenuAlertCase ->
                    val targetPosition =
                        debugMenuAlertCase.targetPositionFromTop

                    Text(
                        text =
                            if (targetPosition == null) {
                                stringResource(
                                    R.string.events_debug_menu_alert_case_compact_no_target,
                                    debugMenuAlertCase.ordinal,
                                    debugMenuAlertCase.type.name,
                                )
                            } else {
                                stringResource(
                                    R.string.events_debug_menu_alert_case_compact,
                                    debugMenuAlertCase.ordinal,
                                    debugMenuAlertCase.type.name,
                                    targetPosition + 1,
                                    debugMenuAlertCase.listCountAtPlan,
                                    debugMenuAlertCase.preferredPlacement.name,
                                )
                            },
                        style = MaterialTheme.typography.bodySmall,
                    )

                    if (
                        debugMenuAlertCase.mode ==
                            com.vextreme.vexlife.android.data.events.EventsDebugMenuAlertMode.SEEDED_RANDOM
                    ) {
                        Text(
                            text =
                                stringResource(
                                    R.string.events_debug_menu_alert_case_seed,
                                    debugMenuAlertCase.seed,
                                ),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    if (blocking) {
                        Text(
                            text =
                                stringResource(
                                    R.string.events_alert_blocking_compact,
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.inversePrimary,
                        )
                    }

                    Text(
                        text =
                            stringResource(
                                R.string.events_alert_press,
                                debugMenuAlertCase.expectedAction.name,
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inversePrimary,
                    )
                    Text(
                        text =
                            stringResource(
                                debugMenuAlertCase.expectedResultRes(),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } ?: Text(
                    text =
                        stringResource(
                            R.string.events_alert_choose_compact,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                )

                if (debugMenuAlertCase != null) {
                    AlertActionButton(
                        eventRef = event.eventRef,
                        action = expectedAction,
                        onAcknowledge = onAcknowledge,
                        onDefer = onDefer,
                        onSuspend = onSuspend,
                        onSuppress = onSuppress,
                    )
                } else {
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(4.dp),
                    ) {
                        CompactAlertButton(
                            labelRes =
                                R.string.events_alert_acknowledge,
                            eventRef = event.eventRef,
                            actionRef = "acknowledge",
                            onClick = onAcknowledge,
                        )
                        CompactAlertButton(
                            labelRes =
                                R.string.events_alert_later,
                            eventRef = event.eventRef,
                            actionRef = "later",
                            onClick = onDefer,
                        )
                    }
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(4.dp),
                    ) {
                        CompactAlertButton(
                            labelRes =
                                R.string.events_alert_suspend,
                            eventRef = event.eventRef,
                            actionRef = "suspend",
                            onClick = onSuspend,
                        )
                        CompactAlertButton(
                            labelRes =
                                R.string.events_alert_suppress,
                            eventRef = event.eventRef,
                            actionRef = "suppress",
                            onClick = onSuppress,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertActionButton(
    eventRef: String,
    action: EventsDebugMenuAlertExpectedAction?,
    onAcknowledge: () -> Unit,
    onDefer: () -> Unit,
    onSuspend: () -> Unit,
    onSuppress: () -> Unit,
) {
    when (action) {
        EventsDebugMenuAlertExpectedAction.ACKNOWLEDGE ->
            CompactAlertButton(
                labelRes = R.string.events_alert_acknowledge,
                eventRef = eventRef,
                actionRef = "acknowledge",
                onClick = onAcknowledge,
            )

        EventsDebugMenuAlertExpectedAction.LATER ->
            CompactAlertButton(
                labelRes = R.string.events_alert_later,
                eventRef = eventRef,
                actionRef = "later",
                onClick = onDefer,
            )

        EventsDebugMenuAlertExpectedAction.SUSPEND ->
            CompactAlertButton(
                labelRes = R.string.events_alert_suspend,
                eventRef = eventRef,
                actionRef = "suspend",
                onClick = onSuspend,
            )

        EventsDebugMenuAlertExpectedAction.SUPPRESS ->
            CompactAlertButton(
                labelRes = R.string.events_alert_suppress,
                eventRef = eventRef,
                actionRef = "suppress",
                onClick = onSuppress,
            )

        null ->
            Unit
    }
}

@Composable
private fun CompactAlertButton(
    labelRes: Int,
    eventRef: String,
    actionRef: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor =
                    MaterialTheme.colorScheme.inversePrimary,
            ),
        modifier =
            Modifier.testTag(
                EventsPresentationRefs.alertAction(
                    eventRef,
                    actionRef,
                ),
            ),
    ) {
        Text(
            stringResource(labelRes),
        )
    }
}

private fun EventsDebugMenuAlertCase.expectedResultRes(): Int =
    when (expectedAction) {
        EventsDebugMenuAlertExpectedAction.ACKNOWLEDGE ->
            if (blockingAcknowledgeOnly) {
                R.string.events_alert_expected_blocking_short
            } else {
                R.string.events_alert_expected_ack_short
            }

        EventsDebugMenuAlertExpectedAction.LATER ->
            R.string.events_alert_expected_later_short

        EventsDebugMenuAlertExpectedAction.SUSPEND ->
            R.string.events_alert_expected_suspend_short

        EventsDebugMenuAlertExpectedAction.SUPPRESS ->
            R.string.events_alert_expected_suppress_short
    }

private fun EventsDebugMenuAlertCase.preferredOverlayPlacements():
    List<VexOverlayPlacement> =
    when (preferredPlacement) {
        EventsDebugMenuAlertPlacement.ABOVE ->
            listOf(
                VexOverlayPlacement.ABOVE,
                VexOverlayPlacement.BELOW,
                VexOverlayPlacement.END,
                VexOverlayPlacement.START,
            )

        EventsDebugMenuAlertPlacement.BELOW ->
            listOf(
                VexOverlayPlacement.BELOW,
                VexOverlayPlacement.ABOVE,
                VexOverlayPlacement.END,
                VexOverlayPlacement.START,
            )
    }

/**
 * Empty/default screen preview.
 */
@VexScreenPreviews
@Composable
private fun EventsScreenPreview() {
    VexPreviewHost(
        id = "events",
        startScreen = EventsScreen,
    ) { context ->
        EventsScreenContent(
            runtime = context.runtime,
            relay = context.relays.events,
        )
    }
}

/**
 * Feature-owned visual fixture for the interesting state: several provider events are already
 * present and no convergence preference has handled the oldest candidate yet.
 *
 * This preview is intentionally beside Events rather than in the shared preview host. It lets a
 * human inspect row composition + anchored convergence under every @VexScreenPreviews device/font/theme
 * form without turning Preview infrastructure into another alertCase registry.
 */
@VexScreenPreviews
@Composable
private fun EventsAlertPreview() {
    VexPreviewHost(
        id = "events-alert",
        startScreen = EventsScreen,
        repositoriesFactory = ::eventsAlertPreviewRepositories,
    ) { context ->
        EventsScreenContent(
            runtime = context.runtime,
            relay = context.relays.events,
        )
    }
}

private fun eventsAlertPreviewRepositories(): AppRepositories {
    val repositories =
        VexPreviewRepositories.empty()
    val events =
        RelayEventsRepository()
    val sourceRef =
        "source.preview.external-information"

    events.accept(
        VexExternalEventSourceSnapshot(
            sourceRef = sourceRef,
            sourceRevision = 1L,
            events =
                listOf(
                    VexExternalEvent(
                        eventRef = "event.preview.message.001",
                        sourceRef = sourceRef,
                        kind = VexExternalEventKind.MESSAGE,
                        title = "Preview message",
                        detail =
                            "This row demonstrates stable repeated identity and anchored convergence.",
                        occurredAtEpochMillis = 1_000L,
                    ),
                    VexExternalEvent(
                        eventRef = "event.preview.call.002",
                        sourceRef = sourceRef,
                        kind = VexExternalEventKind.CALL,
                        title = "Preview call",
                        detail =
                            "A second row makes ordering and list spacing visible in Preview.",
                        occurredAtEpochMillis = 2_000L,
                    ),
                    VexExternalEvent(
                        eventRef = "event.preview.map.003",
                        sourceRef = sourceRef,
                        kind = VexExternalEventKind.MAP,
                        title = "Preview map update",
                        detail =
                            "Large-text Preview pressures row height and overlay placement.",
                        occurredAtEpochMillis = 3_000L,
                    ),
                ),
        ),
    )

    return repositories.copy(
        events = events,
    )
}

// [VXG RealForever]
