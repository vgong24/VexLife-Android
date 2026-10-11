package com.vextreme.vexlife.android.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vextreme.vexlife.android.blueprint.EventsElements
import com.vextreme.vexlife.android.blueprint.HomeElements
import com.vextreme.vexlife.android.blueprint.Screens
import com.vextreme.vexlife.android.blueprint.SettingsElements
import com.vextreme.vexlife.android.elements.ColumnElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.ScrollableColumnElement
import com.vextreme.vexlife.android.elements.TextElement
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexScrollableColumn

@Composable
fun PrimaryScreenBody(screen: ScreenElement) {
    when (screen.screenId) {
        Screens.Home.screenId -> PreviewBody(
            HomeElements.Root,
            HomeElements.PreviewCard,
            HomeElements.PreviewTitle,
            HomeElements.PreviewBody,
        )
        Screens.Events.screenId -> PreviewBody(
            EventsElements.Root,
            EventsElements.PreviewCard,
            EventsElements.PreviewTitle,
            EventsElements.PreviewBody,
        )
        Screens.Settings.screenId -> PreviewBody(
            SettingsElements.Root,
            SettingsElements.PreviewCard,
            SettingsElements.PreviewTitle,
            SettingsElements.PreviewBody,
        )
    }
}

@Composable
private fun PreviewBody(
    root: ScrollableColumnElement,
    card: ColumnElement,
    title: TextElement,
    body: TextElement,
) {
    VexScrollableColumn(
        element = root,
        modifier = Modifier.fillMaxSize(),
    ) {
        VexContainer(card) {
            Text(
                text = stringResource(title.textRes),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(body.textRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
