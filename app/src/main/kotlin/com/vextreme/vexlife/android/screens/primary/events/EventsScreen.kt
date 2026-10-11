package com.vextreme.vexlife.android.screens.primary.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.events.EventsRepository
import com.vextreme.vexlife.android.data.events.VexExternalEvent
import com.vextreme.vexlife.android.preview.VexPreviewHost
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

    fun eventRowElementId(eventRef: String): String =
        "element.events.dynamic.$eventRef"

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        darkTheme: Boolean,
    ) {
        EventsScreenContent(
            repository = repositories.events,
        )
    }
}

@Composable
fun EventsScreenContent(
    repository: EventsRepository,
) {
    val feed by repository.states.collectAsState(
        initial = repository.current,
    )

    LazyColumn(
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
                EventRow(event)
            }
        }
    }
}

@Composable
private fun EventRow(event: VexExternalEvent) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(
                    EventsScreen.eventRowElementId(
                        event.eventRef,
                    ),
                ),
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
            repository = context.repositories.events,
        )
    }
}

// [VXG RealForever]
