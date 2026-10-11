package com.vextreme.vexlife.android.screens.primary.events

import androidx.compose.runtime.Composable
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.preview.VexBlueprintScreenPreview
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.elements.ScreenBlueprint
import com.vextreme.vexlife.android.elements.TextRole

object EventsScreen : ScreenBlueprint(
    screenKey = "events",
    titleRes = R.string.events_title,
) {
    override val content = layout {
        scrollableColumn("body") {
            card("preview") {
                text(
                    id = "title",
                    textRes = R.string.events_preview_title,
                    role = TextRole.Title,
                )
                text(
                    id = "message",
                    textRes = R.string.events_preview_body,
                    role = TextRole.Supporting,
                )
            }
        }
    }
}


@VexScreenPreviews
@Composable
private fun EventsScreenPreview() {
    VexBlueprintScreenPreview(EventsScreen)
}
