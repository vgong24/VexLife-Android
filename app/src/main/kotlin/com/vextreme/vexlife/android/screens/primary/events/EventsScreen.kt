package com.vextreme.vexlife.android.screens.primary.events

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
import com.vextreme.vexlife.android.data.events.EventsAttentionLabPlacement
import com.vextreme.vexlife.android.data.events.EventsAttentionLabRepository
import com.vextreme.vexlife.android.data.events.EventsAttentionLabScenario
import com.vextreme.vexlife.android.data.events.EventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.EventsRepository
import com.vextreme.vexlife.android.data.events.RelayEventsRepository
import com.vextreme.vexlife.android.data.events.VexExternalEvent
import com.vextreme.vexlife.android.data.events.VexExternalEventKind
import com.vextreme.vexlife.android.data.events.VexExternalEventSourceSnapshot
import com.vextreme.vexlife.android.elements.VexAnchoredTooltip
import com.vextreme.vexlife.android.elements.VexStableLazyColumn
import com.vextreme.vexlife.android.elements.VexTooltipPlacement
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexPreviewRepositories
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint

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
        darkTheme: Boolean,
    ) {
        EventsScreenContent(
            runtime = runtime,
            repository = repositories.events,
            attentionPreferences =
                repositories.eventsAttention,
            attentionLab =
                repositories.eventsAttentionLab,
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
    repository: EventsRepository,
    attentionPreferences: EventsAttentionPreferenceRepository,
    attentionLab: EventsAttentionLabRepository,
) {
    val feed by repository.states.collectAsState(
        initial = repository.current,
    )
    val preferences by attentionPreferences.states.collectAsState(
        initial = attentionPreferences.current,
    )
    val labState by attentionLab.states.collectAsState(
        initial = attentionLab.current,
    )
    val scenario =
        labState.currentScenario

    /*
     * BLOCKING_FREEZE_TOP freezes only the displayed projection. The repository/feed continues to
     * admit provider updates so acknowledgement can release the freeze and reveal accumulated truth.
     * This is lab behavior, not an EventsRepository fork.
     */
    var frozenEvents by
        remember {
            mutableStateOf<List<VexExternalEvent>?>(null)
        }

    LaunchedEffect(scenario?.scenarioRef) {
        frozenEvents =
            if (scenario?.freezeProjection == true) {
                feed.events
            } else {
                null
            }
    }

    val displayedEvents =
        frozenEvents ?: feed.events

    val attentionRelay =
        remember {
            EventsAttentionRelay()
        }

    val currentScreenId =
        runtime.currentScreen.screenId
    val transientBlocked =
        runtime.presentation.hasOpenPresentation

    LaunchedEffect(feed) {
        attentionRelay.feedChanged(feed)
    }
    LaunchedEffect(preferences) {
        attentionRelay.preferencesChanged(preferences)
    }
    LaunchedEffect(labState) {
        attentionRelay.labChanged(labState)
    }
    LaunchedEffect(currentScreenId) {
        attentionRelay.screenChanged(currentScreenId)
    }
    LaunchedEffect(transientBlocked) {
        attentionRelay.transientBlockedChanged(
            transientBlocked,
        )
    }

    val attention by attentionRelay.states.collectAsState(
        initial = attentionRelay.current,
    )
    val candidateRef =
        attention.outputState
            .takeIf { output ->
                output.availability ==
                    EventsAttentionAvailability.READY
            }
            ?.candidate
            ?.eventRef

    VexStableLazyColumn(
        listRef = EventsScreen.content.element.elementId,
        items = displayedEvents,
        itemRef = VexExternalEvent::eventRef,
        contentType = { "events-event-row" },
        requestedVisibleItemRef =
            scenario?.targetEventRef,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        onVisibleItemRefsChanged =
            attentionRelay::visibleTargetsChanged,
        emptyContent = {
            Text(
                text = stringResource(R.string.events_preview_body),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
    ) { event ->
        EventRow(
            event = event,
            isAttentionCandidate =
                candidateRef == event.eventRef,
            disposition =
                preferences.dispositionByEventRef[
                    event.eventRef
                ],
            attention = attention.outputState,
            scenario = scenario,
            onAcknowledge = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.attentionAction(
                        event.eventRef,
                        "acknowledge",
                    ),
                )
                attentionPreferences.acknowledge(
                    event.eventRef,
                )
                if (scenario?.targetEventRef == event.eventRef) {
                    attentionLab.completeCurrent()
                }
            },
            onDefer = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.attentionAction(
                        event.eventRef,
                        "later",
                    ),
                )
                attentionPreferences.defer(
                    event.eventRef,
                )
                if (scenario?.targetEventRef == event.eventRef) {
                    attentionLab.completeCurrent()
                }
            },
            onSuspend = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.attentionAction(
                        event.eventRef,
                        "suspend",
                    ),
                )
                attentionPreferences.suspend(
                    event.eventRef,
                )
                if (scenario?.targetEventRef == event.eventRef) {
                    attentionLab.completeCurrent()
                }
            },
            onSuppress = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.attentionAction(
                        event.eventRef,
                        "suppress",
                    ),
                )
                attentionPreferences.suppress(
                    event.eventRef,
                )
                if (scenario?.targetEventRef == event.eventRef) {
                    attentionLab.completeCurrent()
                }
            },
            onDismiss = {
                runtime.recordDynamicInteraction(
                    EventsPresentationRefs.tooltip(
                        event.eventRef,
                    ),
                    action = "Dismissed",
                )
                attentionPreferences.defer(
                    event.eventRef,
                )
                if (scenario?.targetEventRef == event.eventRef) {
                    attentionLab.completeCurrent()
                }
            },
        )
    }
}

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
    attention: EventsAttentionOutputState,
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
                    ?.preferredTooltipPlacements()
                    ?: listOf(
                        VexTooltipPlacement.BELOW,
                        VexTooltipPlacement.ABOVE,
                        VexTooltipPlacement.END,
                        VexTooltipPlacement.START,
                    )
            val blocking =
                scenario?.blockingAcknowledgeOnly == true

            VexAnchoredTooltip(
                presentationRef =
                    EventsPresentationRefs.tooltip(
                        event.eventRef,
                    ),
                onDismissRequest = {
                    if (!blocking) {
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
                    text =
                        stringResource(
                            R.string.events_attention_anchor_hint,
                        ),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = event.detail,
                    style = MaterialTheme.typography.bodyMedium,
                )

                /*
                 * Relay-owned queue metadata. If these numbers are wrong, the failure belongs to the
                 * queue/attention relationship, not to a second Compose-side model.
                 */
                Text(
                    text =
                        stringResource(
                            R.string.events_attention_queue_summary,
                            attention.unhandledCount,
                            attention.queuedAfterCurrentCount,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text =
                        stringResource(
                            R.string.events_attention_sequence_summary,
                            attention.currentQueueCount,
                            attention.nextSequenceCount,
                        ),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text =
                        stringResource(
                            R.string.events_attention_current_ref,
                            event.eventRef,
                        ),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text =
                        stringResource(
                            R.string.events_attention_state,
                            attention.availability.name,
                        ),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text =
                        stringResource(
                            R.string.events_attention_handled_summary,
                            attention.acknowledgedCount,
                            attention.deferredCount,
                            attention.suspendedCount,
                            attention.suppressedCount,
                        ),
                    style = MaterialTheme.typography.labelSmall,
                )

                scenario?.let { labScenario ->
                    Text(
                        text =
                            stringResource(
                                R.string.events_attention_lab_scenario,
                                labScenario.kind.name,
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inversePrimary,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.events_attention_lab_mode,
                                labScenario.mode.name,
                                labScenario.seed,
                                labScenario.ordinal,
                            ),
                        style = MaterialTheme.typography.labelSmall,
                    )

                    val position =
                        labScenario.targetPositionFromTop

                    if (position != null) {
                        Text(
                            text =
                                stringResource(
                                    R.string.events_attention_lab_target,
                                    position + 1,
                                    labScenario.listCountAtPlan,
                                    labScenario.targetEventRef.orEmpty(),
                                ),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }

                    Text(
                        text =
                            stringResource(
                                R.string.events_attention_lab_placement,
                                labScenario.preferredPlacement.name,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.events_attention_lab_expected_action,
                                labScenario.expectedAction.name,
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inversePrimary,
                    )

                    if (labScenario.freezeProjection) {
                        Text(
                            text =
                                stringResource(
                                    R.string.events_attention_lab_blocking,
                                ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.inversePrimary,
                        )
                    }
                }

                Text(
                    text =
                        stringResource(
                            R.string.events_attention_reason_ready,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                )

                Text(
                    text =
                        stringResource(
                            R.string.events_attention_expected_next,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.inversePrimary,
                )
                Text(
                    text =
                        stringResource(
                            R.string.events_attention_acknowledge_expectation,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                )

                if (!blocking) {
                    Text(
                        text =
                            stringResource(
                                R.string.events_attention_later_expectation,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.events_attention_suspend_expectation,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.events_attention_suppress_expectation,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Text(
                    text =
                        stringResource(
                            R.string.events_attention_failure_hint,
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inversePrimary,
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onAcknowledge,
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor =
                                    MaterialTheme.colorScheme.inversePrimary,
                            ),
                        modifier =
                            Modifier.testTag(
                                EventsPresentationRefs.attentionAction(
                                    event.eventRef,
                                    "acknowledge",
                                ),
                            ),
                    ) {
                        Text(
                            stringResource(
                                R.string.events_attention_acknowledge,
                            ),
                        )
                    }

                    if (!blocking) {
                        TextButton(
                            onClick = onDefer,
                            colors =
                                ButtonDefaults.textButtonColors(
                                    contentColor =
                                        MaterialTheme.colorScheme.inversePrimary,
                                ),
                            modifier =
                                Modifier.testTag(
                                    EventsPresentationRefs.attentionAction(
                                        event.eventRef,
                                        "later",
                                    ),
                                ),
                        ) {
                            Text(
                                stringResource(
                                    R.string.events_attention_later,
                                ),
                            )
                        }
                    }
                }

                if (!blocking) {
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = onSuspend,
                            colors =
                                ButtonDefaults.textButtonColors(
                                    contentColor =
                                        MaterialTheme.colorScheme.inversePrimary,
                                ),
                            modifier =
                                Modifier.testTag(
                                    EventsPresentationRefs.attentionAction(
                                        event.eventRef,
                                        "suspend",
                                    ),
                                ),
                        ) {
                            Text(
                                stringResource(
                                    R.string.events_attention_suspend,
                                ),
                            )
                        }
                        TextButton(
                            onClick = onSuppress,
                            colors =
                                ButtonDefaults.textButtonColors(
                                    contentColor =
                                        MaterialTheme.colorScheme.inversePrimary,
                                ),
                            modifier =
                                Modifier.testTag(
                                    EventsPresentationRefs.attentionAction(
                                        event.eventRef,
                                        "suppress",
                                    ),
                                ),
                        ) {
                            Text(
                                stringResource(
                                    R.string.events_attention_suppress,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun EventsAttentionLabScenario.preferredTooltipPlacements():
    List<VexTooltipPlacement> =
    when (preferredPlacement) {
        EventsAttentionLabPlacement.ABOVE ->
            listOf(
                VexTooltipPlacement.ABOVE,
                VexTooltipPlacement.BELOW,
                VexTooltipPlacement.END,
                VexTooltipPlacement.START,
            )

        EventsAttentionLabPlacement.BELOW ->
            listOf(
                VexTooltipPlacement.BELOW,
                VexTooltipPlacement.ABOVE,
                VexTooltipPlacement.END,
                VexTooltipPlacement.START,
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
            repository = context.repositories.events,
            attentionPreferences =
                context.repositories.eventsAttention,
            attentionLab =
                context.repositories.eventsAttentionLab,
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
            repository = context.repositories.events,
            attentionPreferences =
                context.repositories.eventsAttention,
            attentionLab =
                context.repositories.eventsAttentionLab,
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
                            "Large-text Preview pressures row height and tooltip placement.",
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
