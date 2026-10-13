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
import com.vextreme.vexlife.android.data.events.EventsAttentionDisposition
import com.vextreme.vexlife.android.data.events.EventsAttentionLabExpectedAction
import com.vextreme.vexlife.android.data.events.EventsAttentionLabPlacement
import com.vextreme.vexlife.android.data.events.EventsAttentionLabScenario
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
import com.vextreme.vexlife.android.screens.primary.events.traditionalApproach.EventsArchitectureMode
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
            relays = relays,
        )
    }
}

/**
 * Dynamic Events realization.
 *
 * The Screen owns event meaning and attention policy; [VexStableLazyColumn] owns only Android lazy
 * list identity/visibility mechanics. This split is deliberate: a future list-based feature can
 * reuse stable repeated-item behavior without inheriting Events repositories or attention rules.
 */
@Composable
fun EventsScreenContent(
    runtime: VexRuntime,
    relays: VexLifeRelays,
) {
    val architectureMode by
        relays.eventsComparison
            .mode
            .collectAsState()

    val relayAttention by
        relays.events
            .outputStates
            .collectAsState(
                initial =
                    relays.events
                        .currentOutput,
            )

    val traditionalAttention by
        relays.eventsComparison
            .traditionalController
            .uiStates
            .collectAsState(
                initial =
                    relays.eventsComparison
                        .traditionalController
                        .current,
            )

    val attention =
        when (architectureMode) {
            EventsArchitectureMode.VEXTREME_RELAY ->
                relayAttention.toPresentationState()

            EventsArchitectureMode.TRADITIONAL_CONTROLLER ->
                traditionalAttention.toPresentationState()
        }

    val actions =
        when (architectureMode) {
            EventsArchitectureMode.VEXTREME_RELAY ->
                EventsPresentationActions(
                    acknowledge =
                        relays.events::acknowledge,
                    later =
                        relays.events::later,
                    suspend =
                        relays.events::suspend,
                    suppress =
                        relays.events::suppress,
                    completeCurrentScenario =
                        relays.events::completeCurrentScenario,
                )

            EventsArchitectureMode.TRADITIONAL_CONTROLLER ->
                EventsPresentationActions(
                    acknowledge =
                        relays.eventsComparison
                            .traditionalController::acknowledge,
                    later =
                        relays.eventsComparison
                            .traditionalController::later,
                    suspend =
                        relays.eventsComparison
                            .traditionalController::suspend,
                    suppress =
                        relays.eventsComparison
                            .traditionalController::suppress,
                    completeCurrentScenario =
                        relays.eventsComparison
                            .traditionalController::completeCurrentScenario,
                )
        }

    val scenario =
        attention.scenario

    val activeCandidateRef =
        attention.candidateEventRef
    val visibleCandidateRef =
        attention
            .takeIf { state ->
                state.candidateReady
            }
            ?.candidateEventRef

    /*
     * Keep one presentation snapshot while the same attention candidate is active. Canonical
     * provider truth and queue counts continue to advance underneath it, but provider arrivals
     * cannot insert/reorder rows under the anchored popup. Switching comparison architecture also
     * refreshes the snapshot so the selected implementation is what the human actually sees.
     */
    var presentationEvents by
        remember {
            mutableStateOf<List<VexExternalEvent>?>(null)
        }

    LaunchedEffect(
        architectureMode,
        activeCandidateRef,
        scenario?.scenarioRef,
    ) {
        presentationEvents =
            if (activeCandidateRef == null) {
                null
            } else {
                attention.events
            }
    }

    val displayedEvents =
        presentationEvents ?: attention.events

    VexStableLazyColumn(
        listRef = EventsScreen.content.element.elementId,
        items = displayedEvents,
        itemRef = VexExternalEvent::eventRef,
        contentType = { "events-event-row" },
        requestedVisibleItemRef =
            scenario?.targetEventRef,
        userScrollEnabled =
            scenario?.blockingAcknowledgeOnly != true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        onVisibleItemRefsChanged = { eventRefs ->
            relays.events
                .visibleTargetsChanged(
                    eventRefs,
                )
            relays.eventsComparison
                .traditionalController
                .visibleTargetsChanged(
                    eventRefs,
                )
        },
        emptyContent = {
            when (attention.list) {
                EventsPresentationListState.Unobserved ->
                    Unit

                EventsPresentationListState.Empty ->
                    Text(
                        text =
                            stringResource(
                                R.string.events_preview_body,
                            ),
                        style =
                            MaterialTheme.typography.bodyLarge,
                    )

                is EventsPresentationListState.Content ->
                    Unit
            }
        },
    ) { event ->
        EventRow(
            event = event,
            isAttentionCandidate =
                visibleCandidateRef ==
                    event.eventRef,
            disposition =
                attention.dispositionByEventRef[
                    event.eventRef
                ],
            attention = attention,
            scenario = scenario,
            onAcknowledge = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.attentionAction(
                        event.eventRef,
                        "acknowledge",
                    ),
                )
                actions.acknowledge(
                    event.eventRef,
                )
                if (
                    scenario?.targetEventRef ==
                    event.eventRef
                ) {
                    actions.completeCurrentScenario()
                }
            },
            onDefer = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.attentionAction(
                        event.eventRef,
                        "later",
                    ),
                )
                actions.later(
                    event.eventRef,
                )
                if (
                    scenario?.targetEventRef ==
                    event.eventRef
                ) {
                    actions.completeCurrentScenario()
                }
            },
            onSuspend = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.attentionAction(
                        event.eventRef,
                        "suspend",
                    ),
                )
                actions.suspend(
                    event.eventRef,
                )
                if (
                    scenario?.targetEventRef ==
                    event.eventRef
                ) {
                    actions.completeCurrentScenario()
                }
            },
            onSuppress = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.attentionAction(
                        event.eventRef,
                        "suppress",
                    ),
                )
                actions.suppress(
                    event.eventRef,
                )
                if (
                    scenario?.targetEventRef ==
                    event.eventRef
                ) {
                    actions.completeCurrentScenario()
                }
            },
            onDismiss = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.overlay(
                        event.eventRef,
                    ),
                    action = "Dismissed",
                )
                actions.later(
                    event.eventRef,
                )
                if (
                    scenario?.targetEventRef ==
                    event.eventRef
                ) {
                    actions.completeCurrentScenario()
                }
            },
        )
    }
}

private data class EventsPresentationActions(
    val acknowledge: (String) -> Unit,
    val later: (String) -> Unit,
    val suspend: (String) -> Unit,
    val suppress: (String) -> Unit,
    val completeCurrentScenario: () -> Unit,
)

/**
 * Events-specific row composition.
 *
 * Keeping this beside Events is intentional: kind/title/detail/disposition and its attention actions
 * are product meaning, not generic list-cell behavior. The reusable list only asks this row for one
 * stable [VexExternalEvent.eventRef].
 */
@Composable
private fun EventRow(
    event: VexExternalEvent,
    isAttentionCandidate: Boolean,
    disposition: EventsAttentionDisposition?,
    attention: EventsPresentationState,
    scenario: EventsAttentionLabScenario?,
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
                    if (isAttentionCandidate) {
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

        if (isAttentionCandidate) {
            val preferredPlacements =
                scenario
                    ?.preferredOverlayPlacements()
                    ?: listOf(
                        VexOverlayPlacement.BELOW,
                        VexOverlayPlacement.ABOVE,
                        VexOverlayPlacement.END,
                        VexOverlayPlacement.START,
                    )
            val blocking =
                scenario?.blockingAcknowledgeOnly == true
            val expectedAction =
                scenario?.expectedAction

            VexAnchoredOverlay(
                presentationRef =
                    EventsPresentationRefs.overlay(
                        event.eventRef,
                    ),
                onDismissRequest = {
                    if (scenario == null) {
                        onDismiss()
                    }
                },
                preferredPlacements =
                    preferredPlacements,
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.events_attention_candidate,
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
                            R.string.events_attention_compact_queue,
                            attention.unhandledCount,
                            attention.currentQueueCount,
                            attention.nextSequenceCount,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                )

                scenario?.let { labScenario ->
                    val targetPosition =
                        labScenario.targetPositionFromTop

                    Text(
                        text =
                            if (targetPosition == null) {
                                stringResource(
                                    R.string.events_attention_lab_compact_no_target,
                                    labScenario.ordinal,
                                    labScenario.kind.name,
                                )
                            } else {
                                stringResource(
                                    R.string.events_attention_lab_compact,
                                    labScenario.ordinal,
                                    labScenario.kind.name,
                                    targetPosition + 1,
                                    labScenario.listCountAtPlan,
                                    labScenario.preferredPlacement.name,
                                )
                            },
                        style = MaterialTheme.typography.bodySmall,
                    )

                    if (
                        labScenario.mode ==
                            com.vextreme.vexlife.android.data.events.EventsAttentionLabMode.SEEDED_RANDOM
                    ) {
                        Text(
                            text =
                                stringResource(
                                    R.string.events_attention_lab_seed,
                                    labScenario.seed,
                                ),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    if (blocking) {
                        Text(
                            text =
                                stringResource(
                                    R.string.events_attention_blocking_compact,
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.inversePrimary,
                        )
                    }

                    Text(
                        text =
                            stringResource(
                                R.string.events_attention_press,
                                labScenario.expectedAction.name,
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inversePrimary,
                    )
                    Text(
                        text =
                            stringResource(
                                labScenario.expectedResultRes(),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } ?: Text(
                    text =
                        stringResource(
                            R.string.events_attention_choose_compact,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                )

                if (scenario != null) {
                    AttentionActionButton(
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
                        CompactAttentionButton(
                            labelRes =
                                R.string.events_attention_acknowledge,
                            eventRef = event.eventRef,
                            actionRef = "acknowledge",
                            onClick = onAcknowledge,
                        )
                        CompactAttentionButton(
                            labelRes =
                                R.string.events_attention_later,
                            eventRef = event.eventRef,
                            actionRef = "later",
                            onClick = onDefer,
                        )
                    }
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(4.dp),
                    ) {
                        CompactAttentionButton(
                            labelRes =
                                R.string.events_attention_suspend,
                            eventRef = event.eventRef,
                            actionRef = "suspend",
                            onClick = onSuspend,
                        )
                        CompactAttentionButton(
                            labelRes =
                                R.string.events_attention_suppress,
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
private fun AttentionActionButton(
    eventRef: String,
    action: EventsAttentionLabExpectedAction?,
    onAcknowledge: () -> Unit,
    onDefer: () -> Unit,
    onSuspend: () -> Unit,
    onSuppress: () -> Unit,
) {
    when (action) {
        EventsAttentionLabExpectedAction.ACKNOWLEDGE ->
            CompactAttentionButton(
                labelRes = R.string.events_attention_acknowledge,
                eventRef = eventRef,
                actionRef = "acknowledge",
                onClick = onAcknowledge,
            )

        EventsAttentionLabExpectedAction.LATER ->
            CompactAttentionButton(
                labelRes = R.string.events_attention_later,
                eventRef = eventRef,
                actionRef = "later",
                onClick = onDefer,
            )

        EventsAttentionLabExpectedAction.SUSPEND ->
            CompactAttentionButton(
                labelRes = R.string.events_attention_suspend,
                eventRef = eventRef,
                actionRef = "suspend",
                onClick = onSuspend,
            )

        EventsAttentionLabExpectedAction.SUPPRESS ->
            CompactAttentionButton(
                labelRes = R.string.events_attention_suppress,
                eventRef = eventRef,
                actionRef = "suppress",
                onClick = onSuppress,
            )

        null ->
            Unit
    }
}

@Composable
private fun CompactAttentionButton(
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
                EventsPresentationRefs.attentionAction(
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

private fun EventsAttentionLabScenario.expectedResultRes(): Int =
    when (expectedAction) {
        EventsAttentionLabExpectedAction.ACKNOWLEDGE ->
            if (blockingAcknowledgeOnly) {
                R.string.events_attention_expected_blocking_short
            } else {
                R.string.events_attention_expected_ack_short
            }

        EventsAttentionLabExpectedAction.LATER ->
            R.string.events_attention_expected_later_short

        EventsAttentionLabExpectedAction.SUSPEND ->
            R.string.events_attention_expected_suspend_short

        EventsAttentionLabExpectedAction.SUPPRESS ->
            R.string.events_attention_expected_suppress_short
    }

private fun EventsAttentionLabScenario.preferredOverlayPlacements():
    List<VexOverlayPlacement> =
    when (preferredPlacement) {
        EventsAttentionLabPlacement.ABOVE ->
            listOf(
                VexOverlayPlacement.ABOVE,
                VexOverlayPlacement.BELOW,
                VexOverlayPlacement.END,
                VexOverlayPlacement.START,
            )

        EventsAttentionLabPlacement.BELOW ->
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
            relays = context.relays,
        )
    }
}

/**
 * Feature-owned visual fixture for the interesting state: several provider events are already
 * present and no attention preference has handled the oldest candidate yet.
 *
 * This preview is intentionally beside Events rather than in the shared preview host. It lets a
 * human inspect row composition + anchored attention under every @VexScreenPreviews device/font/theme
 * form without turning Preview infrastructure into another scenario registry.
 */
@VexScreenPreviews
@Composable
private fun EventsAttentionPreview() {
    VexPreviewHost(
        id = "events-attention",
        startScreen = EventsScreen,
        repositoriesFactory = ::eventsAttentionPreviewRepositories,
    ) { context ->
        EventsScreenContent(
            runtime = context.runtime,
            relays = context.relays,
        )
    }
}

private fun eventsAttentionPreviewRepositories(): AppRepositories {
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
                            "This row demonstrates stable repeated identity and anchored attention.",
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
