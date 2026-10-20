package com.vextreme.vexlife.android.elements

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * A status glyph whose only interaction is to explain what the represented state means.
 *
 * This is intentionally not the universal shape for every icon:
 *
 * - decorative icons should remain non-interactive and absent from accessibility semantics;
 * - action icons should perform their named action;
 * - an explainable status icon represents current state and lets a human ask "what does this mean?"
 *   without changing that state.
 *
 * Tap and long-press intentionally open the same anchored explanation. Long-press is supported for
 * people who expect tooltip behavior, but tap remains available because long-press alone is poorly
 * discoverable.
 *
 * The caller owns the status truth and the human wording. This component owns only the reusable
 * Android presentation mechanics: semantic identity, accessible status description, explain
 * interaction, and target-relative overlay presentation. Do not create feature-local tooltip/status
 * wrappers that duplicate these mechanics merely to change the explanation text.
 *
 * [id] is local to the inherited Vex Compose parent. The resulting semantic identity remains stable
 * while the icon image, status text, and explanation may change with caller-owned state.
 */
@Composable
fun VexExplainableStatusIcon(
    id: String,
    icon: ElementIcon,
    statusLabel: String,
    explanationTitle: String,
    explanationBody: String,
    modifier: Modifier = VexModifier,
    stateDescriptionText: String = statusLabel,
    preferredPlacements: List<VexOverlayPlacement> =
        listOf(
            VexOverlayPlacement.BELOW,
            VexOverlayPlacement.ABOVE,
            VexOverlayPlacement.END,
            VexOverlayPlacement.START,
        ),
) {
    require(statusLabel.isNotBlank()) {
        "explainable status label must not be blank"
    }
    require(explanationTitle.isNotBlank()) {
        "explainable status explanation title must not be blank"
    }
    require(explanationBody.isNotBlank()) {
        "explainable status explanation body must not be blank"
    }

    val semanticId =
        vexComposeChildRef(
            id,
        )
    var explanationVisible by
        remember(semanticId) {
            mutableStateOf(false)
        }

    Box(
        modifier =
            modifier
                .size(48.dp)
                .vex(
                    semanticId,
                )
                .semantics(
                    mergeDescendants = true,
                ) {
                    contentDescription = statusLabel
                    stateDescription = stateDescriptionText
                    role = Role.Button
                }
                .combinedClickable(
                    role = Role.Button,
                    onClick = {
                        explanationVisible = true
                    },
                    onLongClick = {
                        explanationVisible = true
                    },
                ),
        contentAlignment =
            Alignment.Center,
    ) {
        VexIcon(
            icon = icon,
            contentDescription = null,
        )

        if (explanationVisible) {
            VexAnchoredOverlay(
                presentationRef =
                    "$semanticId.explanation",
                onDismissRequest = {
                    explanationVisible = false
                },
                preferredPlacements =
                    preferredPlacements,
            ) {
                Text(
                    text =
                        explanationTitle,
                    style =
                        MaterialTheme.typography.titleSmall,
                )
                Text(
                    text =
                        explanationBody,
                    style =
                        MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// [VXG RealForever]
