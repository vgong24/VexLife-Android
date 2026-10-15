package com.vextreme.vexlife.android.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

/**
 * Vex-authored text for custom Compose presentation.
 *
 * [id] is local to the inherited Vex Compose parent. The full semantic ref is formed automatically.
 */
@Composable
fun VexText(
    id: String,
    text: String,
    modifier: Modifier = VexModifier,
    role: TextRole = TextRole.Body,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val ref =
        vexComposeChildRef(
            id,
        )

    Text(
        text = text,
        modifier =
            modifier.vex(
                ref,
            ),
        color = color,
        style =
            when (role) {
                TextRole.Title ->
                    MaterialTheme.typography.headlineMedium

                TextRole.Body ->
                    MaterialTheme.typography.bodyLarge

                TextRole.Supporting ->
                    MaterialTheme.typography.bodyMedium

                TextRole.Label ->
                    MaterialTheme.typography.labelSmall
            },
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * Nested Vex column. Child identity is parent-derived and becomes the inherited parent for content.
 */
@Composable
fun VexColumn(
    id: String,
    modifier: Modifier = VexModifier,
    verticalArrangement: Arrangement.Vertical =
        Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal =
        Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ref =
        vexComposeChildRef(
            id,
        )

    Column(
        modifier =
            modifier.vex(
                ref,
            ),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
    ) {
        val columnScope = this
        VexComposeChildScope(
            childRef = ref,
        ) {
            columnScope.content()
        }
    }
}

/**
 * Nested Vex row. Child identity is parent-derived and becomes the inherited parent for content.
 */
@Composable
fun VexRow(
    id: String,
    modifier: Modifier = VexModifier,
    horizontalArrangement: Arrangement.Horizontal =
        Arrangement.Start,
    verticalAlignment: Alignment.Vertical =
        Alignment.Top,
    content: @Composable RowScope.() -> Unit,
) {
    val ref =
        vexComposeChildRef(
            id,
        )

    Row(
        modifier =
            modifier.vex(
                ref,
            ),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
    ) {
        val rowScope = this
        VexComposeChildScope(
            childRef = ref,
        ) {
            rowScope.content()
        }
    }
}

/**
 * Nested Vex box/stack.
 */
@Composable
fun VexBox(
    id: String,
    modifier: Modifier = VexModifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val ref =
        vexComposeChildRef(
            id,
        )

    Box(
        modifier =
            modifier.vex(
                ref,
            ),
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints,
    ) {
        val boxScope = this
        VexComposeChildScope(
            childRef = ref,
        ) {
            boxScope.content()
        }
    }
}

/**
 * Nested Material card with inherited Vex semantic continuity.
 */
@Composable
fun VexCard(
    id: String,
    modifier: Modifier = VexModifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ref =
        vexComposeChildRef(
            id,
        )

    Card(
        modifier =
            modifier.vex(
                ref,
            ),
    ) {
        val columnScope = this
        VexComposeChildScope(
            childRef = ref,
        ) {
            columnScope.content()
        }
    }
}

// [VXG RealForever]
