package com.vextreme.vexlife.android.pages.primary.events

import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.PageBlueprint
import com.vextreme.vexlife.android.elements.TextRole

object EventsPage : PageBlueprint(
    pageId = "events",
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
