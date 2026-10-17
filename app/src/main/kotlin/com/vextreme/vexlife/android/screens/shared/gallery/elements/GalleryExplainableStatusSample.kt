package com.vextreme.vexlife.android.screens.shared.gallery.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexCard
import com.vextreme.vexlife.android.elements.VexComposeScope
import com.vextreme.vexlife.android.elements.VexExplainableStatusIcon
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexRow
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen

/**
 * Gallery pressure for the reusable "status can explain itself" interaction.
 *
 * Keep this example in Gallery rather than copying tooltip/icon mechanics into a feature. Product
 * features own the status truth and explanation wording; Elements owns how a status icon can be
 * inspected by tap, long-press, and accessibility while its anchored explanation stays visually
 * connected to the icon.
 *
 * The sample intentionally uses Home-unavailable wording because Conversation exposed the first
 * concrete need. That does NOT make the component Conversation- or Home-specific.
 */
@Composable
internal fun GalleryExplainableStatusSample() {
    VexComposeScope(
        parent =
            GalleryScreen.ElementsPanel,
    ) {
        VexCard(
            id = "explainable-status-sample",
            modifier =
                VexModifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp,
                    ),
        ) {
            VexText(
                id = "title",
                text =
                    stringResource(
                        R.string.gallery_explainable_status_title,
                    ),
                role = TextRole.Title,
            )
            VexText(
                id = "body",
                text =
                    stringResource(
                        R.string.gallery_explainable_status_body,
                    ),
                role = TextRole.Supporting,
            )
            VexRow(
                id = "example",
                modifier =
                    VexModifier
                        .fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp,
                    ),
            ) {
                VexExplainableStatusIcon(
                    id = "home-status",
                    icon = ElementIcon.Home,
                    statusLabel =
                        stringResource(
                            R.string.gallery_explainable_status_label,
                        ),
                    explanationTitle =
                        stringResource(
                            R.string.gallery_explainable_status_explanation_title,
                        ),
                    explanationBody =
                        stringResource(
                            R.string.gallery_explainable_status_explanation_body,
                        ),
                )
                VexText(
                    id = "hint",
                    text =
                        stringResource(
                            R.string.gallery_explainable_status_hint,
                        ),
                    role = TextRole.Supporting,
                )
            }
        }
    }
}

// [VXG RealForever]
