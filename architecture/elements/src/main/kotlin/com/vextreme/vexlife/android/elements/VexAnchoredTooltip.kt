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
        preferred.firstOrNull { placement ->
            room(placement) >= required(placement)
        }

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

    return VexResolvedTooltipPlacement(
        placement = placement,
        offset = offset,
        rect = rect,
        clipped = clipped,
    )
}

private class VexAnchoredTooltipPositionProvider(
    private val marginPx: Int,
    private val preferred: List<VexTooltipPlacement>,
    private val onResolvedPlacement: (VexTooltipPlacement) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val resolved =
            resolveVexAnchoredTooltip(
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

internal enum class VexTooltipCaretDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    NONE,
}

internal fun vexTooltipCaretDirection(
    placement: VexTooltipPlacement,
    layoutDirection: LayoutDirection,
): VexTooltipCaretDirection =
    when (placement) {
        VexTooltipPlacement.ABOVE ->
            VexTooltipCaretDirection.DOWN

        VexTooltipPlacement.BELOW ->
            VexTooltipCaretDirection.UP

        VexTooltipPlacement.START ->
            if (layoutDirection == LayoutDirection.Ltr) {
                VexTooltipCaretDirection.RIGHT
            } else {
                VexTooltipCaretDirection.LEFT
            }

        VexTooltipPlacement.END ->
            if (layoutDirection == LayoutDirection.Ltr) {
                VexTooltipCaretDirection.LEFT
            } else {
                VexTooltipCaretDirection.RIGHT
            }

        VexTooltipPlacement.FALLBACK ->
            VexTooltipCaretDirection.NONE
    }

@Composable
fun VexAnchoredTooltip(
    presentationRef: String,
    onDismissRequest: () -> Unit,
    preferredPlacements: List<VexTooltipPlacement> =
        listOf(
            VexTooltipPlacement.BELOW,
            VexTooltipPlacement.ABOVE,
            VexTooltipPlacement.END,
            VexTooltipPlacement.START,
        ),
    content: @Composable ColumnScope.() -> Unit,
) {
    require(presentationRef.isNotBlank()) {
        "tooltip presentationRef must not be blank"
    }
    require(preferredPlacements.isNotEmpty()) {
        "tooltip preferredPlacements must not be empty"
    }

    val density =
        LocalDensity.current
    val anchorGapPx =
        with(density) {
            4.dp.roundToPx()
        }

    var resolvedPlacement by
        remember {
            mutableStateOf<VexTooltipPlacement?>(null)
        }

    val provider =
        remember(
            anchorGapPx,
            preferredPlacements,
        ) {
            VexAnchoredTooltipPositionProvider(
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
        VexTooltipBubble(
            presentationRef = presentationRef,
            placement = visualPlacement,
            content = content,
        )
    }
}

@Composable
private fun VexTooltipBubble(
    presentationRef: String,
    placement: VexTooltipPlacement,
    content: @Composable ColumnScope.() -> Unit,
) {
    val layoutDirection =
        LocalLayoutDirection.current
    val caretDirection =
        vexTooltipCaretDirection(
            placement = placement,
            layoutDirection = layoutDirection,
        )

    when (caretDirection) {
        VexTooltipCaretDirection.UP ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VexTooltipCaret(caretDirection)
                VexTooltipSurface(
                    presentationRef = presentationRef,
                    content = content,
                )
            }

        VexTooltipCaretDirection.DOWN ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VexTooltipSurface(
                    presentationRef = presentationRef,
                    content = content,
                )
                VexTooltipCaret(caretDirection)
            }

        VexTooltipCaretDirection.LEFT ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VexTooltipCaret(caretDirection)
                VexTooltipSurface(
                    presentationRef = presentationRef,
                    content = content,
                )
            }

        VexTooltipCaretDirection.RIGHT ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VexTooltipSurface(
                    presentationRef = presentationRef,
                    content = content,
                )
                VexTooltipCaret(caretDirection)
            }

        VexTooltipCaretDirection.NONE ->
            VexTooltipSurface(
                presentationRef = presentationRef,
                content = content,
            )
    }
}

@Composable
private fun VexTooltipSurface(
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
private fun VexTooltipCaret(
    direction: VexTooltipCaretDirection,
) {
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
            VexTooltipCaretDirection.UP,
            VexTooltipCaretDirection.DOWN,
            ->
                Modifier.size(
                    width = 18.dp,
                    height = 10.dp,
                )

            VexTooltipCaretDirection.LEFT,
            VexTooltipCaretDirection.RIGHT,
            ->
                Modifier.size(
                    width = 10.dp,
                    height = 18.dp,
                )

            VexTooltipCaretDirection.NONE ->
                Modifier.size(0.dp)
        }

    Canvas(modifier) {
        val path =
            Path().apply {
                when (direction) {
                    VexTooltipCaretDirection.UP -> {
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

                    VexTooltipCaretDirection.DOWN -> {
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

                    VexTooltipCaretDirection.LEFT -> {
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

                    VexTooltipCaretDirection.RIGHT -> {
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

                    VexTooltipCaretDirection.NONE ->
                        return@Canvas
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
