package com.vextreme.vexlife.android.pages.primary.home

import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.PageBlueprint
import androidx.compose.runtime.Composable
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.preview.VexBlueprintPagePreview
import com.vextreme.vexlife.android.preview.VexPagePreviews

object HomePage : PageBlueprint(
    pageId = "home",
    titleRes = R.string.home_title,
) {
    override val content = layout {
        scrollableColumn("body") {
            card("preview") {
                text(
                    id = "title",
                    textRes = R.string.home_preview_title,
                    role = TextRole.Title,
                )
                text(
                    id = "message",
                    textRes = R.string.home_preview_body,
                    role = TextRole.Supporting,
                )
            }
        }
    }
}


// PREVIEW — intentionally kept beside HomePage so visual work starts from this file.
@VexPagePreviews
@Composable
private fun HomePagePreview() {
    VexBlueprintPagePreview(HomePage)
}
