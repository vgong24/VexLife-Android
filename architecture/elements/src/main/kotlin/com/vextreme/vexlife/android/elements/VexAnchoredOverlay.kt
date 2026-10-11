package com.vextreme.vexlife.android.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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

enum class VexOverlayPlacement {
    ABOVE,
    BELOW,
    START,
    END,
    FALLBACK,
}

data class VexResolvedOverlayPlacement(
    val placement: VexOverlayPlacement,
    val offset: IntOffset,
    val rect: IntRect,
    val clipped: Boolean,
)

fun resolveVexAnchoredOverlay(
    anchorBounds: IntRect,
    windowSize: IntSize,
    popupSize: IntSize,
    layoutDirection: LayoutDirection,
    marginPx: Int,
    preferred: List<VexOverlayPlacement> =
        listOf(
            VexOverlayPlacement.BELOW,
            VexOverlayPlacement.ABOVE,
            VexOverlayPlacement.END,
            VexOverlayPlacement.START,
        ),
): VexResolvedOverlayPlacement {
    require(marginPx >= 0) {
        "overlay margin must be non-negative"
    }
    require(
        preferred.none {
            it == VexOverlayPlacement.FALLBACK
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

    fun room(placement: VexOverlayPlacement): Int =
        when (placement) {
            VexOverlayPlacement.ABOVE ->
                anchorBounds.top - safe.top

            VexOverlayPlacement.BELOW ->
                safe.bottom - anchorBounds.bottom

            VexOverlayPlacement.START ->
                if (layoutDirection == LayoutDirection.Ltr) {
                    anchorBounds.left - safe.left
                } else {
                    safe.right - anchorBounds.right
                }

            VexOverlayPlacement.END ->
                if (layoutDirection == LayoutDirection.Ltr) {
                    safe.right - anchorBounds.right
                } else {
                    anchorBounds.left - safe.left
                }

            VexOverlayPlacement.FALLBACK ->
                0
        }

    fun required(placement: VexOverlayPlacement): Int =
        when (placement) {
            VexOverlayPlacement.ABOVE,
            VexOverlayPlacement.BELOW,
            -> popupSize.height + marginPx

            VexOverlayPlacement.START,
            VexOverlayPlacement.END,
            -> popupSize.width + marginPx

            VexOverlayPlacement.FALLBACK ->
                Int.MAX_VALUE
        }

    val chosen =
        preferred.firstOrNull { placement ->
            room(placement) >= required(placement)
        }

    val placement =
        chosen ?: VexOverlayPlacement.FALLBACK

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
            VexOverlayPlacement.ABOVE ->
                IntOffset(
                    x = clampX(centeredX),
                    y =
                        anchorBounds.top -
                            popupSize.height -
                            marginPx,
                )

            VexOverlayPlacement.BELOW ->
                IntOffset(
                    x = clampX(centeredX),
                    y = anchorBounds.bottom + marginPx,
                )

            VexOverlayPlacement.START ->
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

            VexOverlayPlacement.END ->
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

            VexOverlayPlacement.FALLBACK ->
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
            left = offset.x,
            top = offset.y,
            right = offset.x + popupSize.width,
            bottom = offset.y + popupSize.height,
        )

    val clipped =
        rect.left < safe.left ||
            rect.top < safe.top ||
            rect.right > safe.right ||
            rect.bottom > safe.bottom

    return VexResolvedOverlayPlacement(
        placement = placement,
        offset = offset,
        rect = rect,
        clipped = clipped,
    )
}

private class VexAnchoredOverlayPositionProvider(
    private val marginPx: Int,
    private val preferred: List<VexOverlayPlacement>,
    private val onResolvedPlacement: (VexOverlayPlacement) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val resolved =
            resolveVexAnchoredOverlay(
                anchorBounds = anchorBounds,
                windowSize = windowSize,
                popupSize = popupContentSize,
                layoutDirection = layoutDirection,
                marginPx = marginPx,
                preferred = preferred,
            )

        onResolvedPlacement(resolved.placement)
        return resolved.offset
    }
}

internal enum class VexOverlayCaretDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    NONE,
}

internal fun vexOverlayCaretDirection(
    placement: VexOverlayPlacement,
    layoutDirection: LayoutDirection,
): VexOverlayCaretDirection =
    when (placement) {
        VexOverlayPlacement.ABOVE ->
            VexOverlayCaretDirection.DOWN

        VexOverlayPlacement.BELOW ->
            VexOverlayCaretDirection.UP

        VexOverlayPlacement.START ->
            if (layoutDirection == LayoutDirection.Ltr) {
                VexOverlayCaretDirection.RIGHT
            } else {
                VexOverlayCaretDirection.LEFT
            }

        VexOverlayPlacement.END ->
            if (layoutDirection == LayoutDirection.Ltr) {
                VexOverlayCaretDirection.LEFT
            } else {
                VexOverlayCaretDirection.RIGHT
            }

        VexOverlayPlacement.FALLBACK ->
            VexOverlayCaretDirection.NONE
    }

/**
 * Target-relative transient overlay.
 *
 * The primitive owns only relational presentation mechanics:
 * - resolve a safe placement around the Compose anchor;
 * - render a directional caret from the resolved placement;
 * - preserve safe fallback when a preferred side cannot fit;
 * - provide the transient container/dismissal boundary.
 *
 * Why the overlay exists, whether it is blocking, and what its actions mean belong to the caller.
 */
@Composable
fun VexAnchoredOverlay(
    presentationRef: String,
    onDismissRequest: () -> Unit,
    preferredPlacements: List<VexOverlayPlacement> =
        listOf(
            VexOverlayPlacement.BELOW,
            VexOverlayPlacement.ABOVE,
            VexOverlayPlacement.END,
            VexOverlayPlacement.START,
        ),
    content: @Composable ColumnScope.() -> Unit,
) {
    require(presentationRef.isNotBlank()) {
        "overlay presentationRef must not be blank"
    }
    require(preferredPlacements.isNotEmpty()) {
        "overlay preferredPlacements must not be empty"
    }

    val density =
        LocalDensity.current
    val anchorGapPx =
        with(density) {
            4.dp.roundToPx()
        }

    var resolvedPlacement by
        remember {
            mutableStateOf<VexOverlayPlacement?>(null)
        }

    val provider =
        remember(
            anchorGapPx,
            preferredPlacements,
        ) {
            VexAnchoredOverlayPositionProvider(
                marginPx = anchorGapPx,
                preferred = preferredPlacements,
                onResolvedPlacement = { placement ->
                    if (resolvedPlacement != placement) {
                        resolvedPlacement = placement
                    }
                },
            )
        }

    val visualPlacement =
        resolvedPlacement
            ?: preferredPlacements.first()

    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismissRequest,
        properties =
            PopupProperties(
                focusable = true,
            ),
    ) {
        VexAnchoredOverlayBubble(
            presentationRef = presentationRef,
            placement = visualPlacement,
            content = content,
        )
    }
}

@Composable
private fun VexAnchoredOverlayBubble(
    presentationRef: String,
    placement: VexOverlayPlacement,
    content: @Composable ColumnScope.() -> Unit,
) {
    val layoutDirection =
        LocalLayoutDirection.current
    val caretDirection =
        vexOverlayCaretDirection(
            placement = placement,
            layoutDirection = layoutDirection,
        )

    when (caretDirection) {
        VexOverlayCaretDirection.UP ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VexAnchoredOverlayCaret(caretDirection)
                VexAnchoredOverlaySurface(
                    presentationRef = presentationRef,
                    content = content,
                )
            }

        VexOverlayCaretDirection.DOWN ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VexAnchoredOverlaySurface(
                    presentationRef = presentationRef,
                    content = content,
                )
                VexAnchoredOverlayCaret(caretDirection)
            }

        VexOverlayCaretDirection.LEFT ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VexAnchoredOverlayCaret(caretDirection)
                VexAnchoredOverlaySurface(
                    presentationRef = presentationRef,
                    content = content,
                )
            }

        VexOverlayCaretDirection.RIGHT ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VexAnchoredOverlaySurface(
                    presentationRef = presentationRef,
                    content = content,
                )
                VexAnchoredOverlayCaret(caretDirection)
            }

        VexOverlayCaretDirection.NONE ->
            VexAnchoredOverlaySurface(
                presentationRef = presentationRef,
                content = content,
            )
    }
}

@Composable
private fun VexAnchoredOverlaySurface(
    presentationRef: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .widthIn(
                    min = 260.dp,
                    max = 300.dp,
                )
                .heightIn(max = 340.dp)
                .testTag(presentationRef),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        border =
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
            ),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
private fun VexAnchoredOverlayCaret(
    direction: VexOverlayCaretDirection,
) {
    if (direction == VexOverlayCaretDirection.NONE) {
        return
    }

    val fill =
        MaterialTheme.colorScheme.inverseSurface
    val outline =
        MaterialTheme.colorScheme.outline
    val strokeWidth =
        with(LocalDensity.current) {
            1.dp.toPx()
        }

    val modifier =
        when (direction) {
            VexOverlayCaretDirection.UP,
            VexOverlayCaretDirection.DOWN,
            ->
                Modifier.size(
                    width = 18.dp,
                    height = 10.dp,
                )

            VexOverlayCaretDirection.LEFT,
            VexOverlayCaretDirection.RIGHT,
            ->
                Modifier.size(
                    width = 10.dp,
                    height = 18.dp,
                )

            VexOverlayCaretDirection.NONE ->
                error("NONE caret direction returned after early exit")
        }

    Canvas(modifier) {
        val path =
            Path().apply {
                when (direction) {
                    VexOverlayCaretDirection.UP -> {
                        moveTo(
                            x = size.width / 2f,
                            y = 0f,
                        )
                        lineTo(
                            x = size.width,
                            y = size.height,
                        )
                        lineTo(
                            x = 0f,
                            y = size.height,
                        )
                    }

                    VexOverlayCaretDirection.DOWN -> {
                        moveTo(
                            x = 0f,
                            y = 0f,
                        )
                        lineTo(
                            x = size.width,
                            y = 0f,
                        )
                        lineTo(
                            x = size.width / 2f,
                            y = size.height,
                        )
                    }

                    VexOverlayCaretDirection.LEFT -> {
                        moveTo(
                            x = 0f,
                            y = size.height / 2f,
                        )
                        lineTo(
                            x = size.width,
                            y = 0f,
                        )
                        lineTo(
                            x = size.width,
                            y = size.height,
                        )
                    }

                    VexOverlayCaretDirection.RIGHT -> {
                        moveTo(
                            x = 0f,
                            y = 0f,
                        )
                        lineTo(
                            x = size.width,
                            y = size.height / 2f,
                        )
                        lineTo(
                            x = 0f,
                            y = size.height,
                        )
                    }

                    VexOverlayCaretDirection.NONE ->
                        error("NONE caret direction returned after early exit")
                }

                close()
            }

        drawPath(
            path = path,
            color = fill,
        )
        drawPath(
            path = path,
            color = outline,
            style =
                Stroke(
                    width = strokeWidth,
                ),
        )
    }
}

// [VXG RealForever]
