package com.vextreme.vexlife.android.screens.primary.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.events.EventsAttentionDisposition
import com.vextreme.vexlife.android.data.events.EventsAttentionPreferenceRepository
import com.vextreme.vexlife.android.data.events.EventsRepository
import com.vextreme.vexlife.android.data.events.VexExternalEvent
import com.vextreme.vexlife.android.elements.VexAnchoredTooltip
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import kotlinx.coroutines.flow.distinctUntilChanged

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
        )
    }
}

@Composable
fun EventsScreenContent(
    runtime: VexRuntime,
    repository: EventsRepository,
    attentionPreferences: EventsAttentionPreferenceRepository,
) {
    val feed by repository.states.collectAsState(
        initial = repository.current,
    )
    val preferences by attentionPreferences.states.collectAsState(
        initial = attentionPreferences.current,
    )

    val attentionRelay =
        remember {
            EventsAttentionRelay()
        }
    val listState =
        rememberLazyListState()

    val currentScreenId =
        runtime.currentScreen.screenId
    val transientBlocked =
        runtime.presentation.hasOpenPresentation
    val eventRefs =
        feed.events
            .map(VexExternalEvent::eventRef)
            .toSet()

    LaunchedEffect(feed) {
        attentionRelay.feedChanged(feed)
    }
    LaunchedEffect(preferences) {
        attentionRelay.preferencesChanged(preferences)
    }
    LaunchedEffect(currentScreenId) {
        attentionRelay.screenChanged(currentScreenId)
    }
    LaunchedEffect(transientBlocked) {
        attentionRelay.transientBlockedChanged(
            transientBlocked,
        )
    }
    LaunchedEffect(listState, eventRefs) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { item ->
                    item.key as? String
                }
                .toSet()
                .intersect(eventRefs)
        }
            .distinctUntilChanged()
            .collect { visible ->
                attentionRelay.visibleTargetsChanged(
                    visible,
                )
            }
    }

    val attention by attentionRelay.states.collectAsState(
        initial = attentionRelay.current,
    )
    val candidateRef =
        attention.outputState.candidate?.eventRef

    LazyColumn(
        state = listState,
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(EventsScreen.content.element.elementId)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (feed.events.isEmpty()) {
            item(key = "events-empty") {
                Text(
                    text = stringResource(R.string.events_preview_body),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            items(
                items = feed.events,
                key = VexExternalEvent::eventRef,
            ) { event ->
                EventRow(
                    event = event,
                    isAttentionCandidate =
                        candidateRef == event.eventRef,
                    disposition =
                        preferences.dispositionByEventRef[
                            event.eventRef
                        ],
                    onAcknowledge = {
                        attentionPreferences.acknowledge(
                            event.eventRef,
                        )
                    },
                    onDefer = {
                        attentionPreferences.defer(
                            event.eventRef,
                        )
                    },
                    onSuppress = {
                        attentionPreferences.suppress(
                            event.eventRef,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun EventRow(
    event: VexExternalEvent,
    isAttentionCandidate: Boolean,
    disposition: EventsAttentionDisposition?,
    onAcknowledge: () -> Unit,
    onDefer: () -> Unit,
    onSuppress: () -> Unit,
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
            VexAnchoredTooltip(
                presentationRef =
                    EventsPresentationRefs.tooltip(
                        event.eventRef,
                    ),
                onDismissRequest = onDefer,
            ) {
                Text(
                    text =
                        stringResource(
                            R.string.events_attention_candidate,
                        ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = event.detail,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onAcknowledge,
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
                    TextButton(
                        onClick = onDefer,
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
                    TextButton(
                        onClick = onSuppress,
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
        )
    }
}

// [VXG RealForever]
