package com.vextreme.vexlife.android.pages.primary.settings

import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.PageBlueprint
import androidx.compose.runtime.Composable
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.preview.VexBlueprintPagePreview
import com.vextreme.vexlife.android.preview.VexPagePreviews

object SettingsPage : PageBlueprint(
    pageId = "settings",
    titleRes = R.string.settings_title,
) {
    override val content = layout {
        scrollableColumn("body") {
            card("preview") {
                text(
                    id = "title",
                    textRes = R.string.settings_preview_title,
                    role = TextRole.Title,
                )
                text(
                    id = "message",
                    textRes = R.string.settings_preview_body,
                    role = TextRole.Supporting,
                )
            }
        }
    }
}


// PREVIEW — stays beside SettingsPage so Android Studio shows the page while it is edited.
@VexPagePreviews
@Composable
private fun SettingsPagePreview() {
    VexBlueprintPagePreview(SettingsPage)
}
