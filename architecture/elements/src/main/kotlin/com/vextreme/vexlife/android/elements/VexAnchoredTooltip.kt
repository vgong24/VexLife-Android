package com.vextreme.vexlife.android.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.max

enum class VexTooltipPlacement {
    ABOVE,
    BELOW,
    START,
    END,
    FALLBACK,
}

data class VexResolvedTooltipPlacement(
    val placement: VexTooltipPlacement,
    val offset: IntOffset,
    val rect: IntRect,
    val clipped: Boolean,
)

fun resolveVexAnchoredTooltip(
    anchorBounds: IntRect,
    windowSize: IntSize,
    popupSize: IntSize,
    layoutDirection: LayoutDirection,
    marginPx: Int,
    preferred: List<VexTooltipPlacement> =
        listOf(
            VexTooltipPlacement.BELOW,
            VexTooltipPlacement.ABOVE,
            VexTooltipPlacement.END,
            VexTooltipPlacement.START,
        ),
): VexResolvedTooltipPlacement {
    require(marginPx >= 0) {
        "tooltip margin must be non-negative"
    }
    require(
        preferred.none {
            it == VexTooltipPlacement.FALLBACK
        },
    ) {
        "FALLBACK is resolver-owned, not caller preference"
    }

    val safe =
        IntRect(
            left = marginPx,
            top = marginPx,
            right = max(marginPx, windowSize.width - marginPx),
            bottom = max(marginPx, windowSize.height - marginPx),
        )

    fun room(placement: VexTooltipPlacement): Int =
        when (placement) {
            VexTooltipPlacement.ABOVE ->
                anchorBounds.top - safe.top

            VexTooltipPlacement.BELOW ->
                safe.bottom - anchorBounds.bottom

            VexTooltipPlacement.START ->
                if (layoutDirection == LayoutDirection.Ltr) {
                    anchorBounds.left - safe.left
                } else {
                    safe.right - anchorBounds.right
                }

            VexTooltipPlacement.END ->
                if (layoutDirection == LayoutDirection.Ltr) {
                    safe.right - anchorBounds.right
                } else {
                    anchorBounds.left - safe.left
                }

            VexTooltipPlacement.FALLBACK ->
                0
        }

    fun required(placement: VexTooltipPlacement): Int =
        when (placement) {
            VexTooltipPlacement.ABOVE,
            VexTooltipPlacement.BELOW,
            -> popupSize.height + marginPx

            VexTooltipPlacement.START,
            VexTooltipPlacement.END,
            -> popupSize.width + marginPx

            VexTooltipPlacement.FALLBACK ->
                Int.MAX_VALUE
        }

    val chosen =
        preferred
            .filter { placement ->
                room(placement) >= required(placement)
            }
            .maxWithOrNull(
                compareBy<VexTooltipPlacement>(
                    { placement -> room(placement) },
                    { placement -> -preferred.indexOf(placement) },
                ),
            )

    val placement =
        chosen ?: VexTooltipPlacement.FALLBACK

    val centeredX =
        anchorBounds.left +
            (anchorBounds.width - popupSize.width) / 2
    val centeredY =
        anchorBounds.top +
            (anchorBounds.height - popupSize.height) / 2

    val maxX =
        max(safe.left, safe.right - popupSize.width)
    val maxY =
        max(safe.top, safe.bottom - popupSize.height)

    fun clampX(value: Int): Int =
        value.coerceIn(safe.left, maxX)

    fun clampY(value: Int): Int =
        value.coerceIn(safe.top, maxY)

    val offset =
        when (placement) {
            VexTooltipPlacement.ABOVE ->
                IntOffset(
                    x = clampX(centeredX),
                    y =
                        anchorBounds.top -
                            popupSize.height -
                            marginPx,
                )

            VexTooltipPlacement.BELOW ->
                IntOffset(
                    x = clampX(centeredX),
                    y = anchorBounds.bottom + marginPx,
                )

            VexTooltipPlacement.START ->
                if (layoutDirection == LayoutDirection.Ltr) {
                    IntOffset(
                        x =
                            anchorBounds.left -
                                popupSize.width -
                                marginPx,
                        y = clampY(centeredY),
                    )
                } else {
                    IntOffset(
                        x = anchorBounds.right + marginPx,
                        y = clampY(centeredY),
                    )
                }

            VexTooltipPlacement.END ->
                if (layoutDirection == LayoutDirection.Ltr) {
                    IntOffset(
                        x = anchorBounds.right + marginPx,
                        y = clampY(centeredY),
                    )
                } else {
                    IntOffset(
                        x =
                            anchorBounds.left -
                                popupSize.width -
                                marginPx,
                        y = clampY(centeredY),
                    )
                }

            VexTooltipPlacement.FALLBACK ->
                IntOffset(
                    x = clampX(centeredX),
                    y =
                        clampY(
                            anchorBounds.bottom + marginPx,
                        ),
                )
        }

    val rect =
        IntRect(
            offset = offset,
            size = popupSize,
        )

    val clipped =
        rect.left < safe.left ||
            rect.top < safe.top ||
            rect.right > safe.right ||
            rect.bottom > safe.bottom

    return VexResolvedTooltipPlacement(
        placement = placement,
        offset = offset,
        rect = rect,
        clipped = clipped,
    )
}

private class VexAnchoredTooltipPositionProvider(
    private val marginPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset =
        resolveVexAnchoredTooltip(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            popupSize = popupContentSize,
            layoutDirection = layoutDirection,
            marginPx = marginPx,
        ).offset
}

@Composable
fun VexAnchoredTooltip(
    presentationRef: String,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    require(presentationRef.isNotBlank()) {
        "tooltip presentationRef must not be blank"
    }

    val density =
        LocalDensity.current
    val marginPx =
        with(density) {
            12.dp.roundToPx()
        }
    val provider =
        remember(marginPx) {
            VexAnchoredTooltipPositionProvider(
                marginPx = marginPx,
            )
        }

    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismissRequest,
        properties =
            PopupProperties(
                focusable = true,
            ),
    ) {
        Surface(
            modifier =
                Modifier
                    .widthIn(max = 320.dp)
                    .testTag(presentationRef),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

// [VXG RealForever]
