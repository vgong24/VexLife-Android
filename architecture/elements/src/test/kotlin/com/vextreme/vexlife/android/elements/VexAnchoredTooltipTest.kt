package com.vextreme.vexlife.android.elements

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexAnchoredTooltipTest {
    @Test
    fun choosesTheFirstPreferredSideThatSafelyFits() {
        val resolved =
            resolveVexAnchoredTooltip(
                anchorBounds =
                    IntRect(
                        left = 100,
                        top = 100,
                        right = 200,
                        bottom = 150,
                    ),
                windowSize = IntSize(400, 800),
                popupSize = IntSize(180, 120),
                layoutDirection = LayoutDirection.Ltr,
                marginPx = 12,
            )

        assertEquals(
            VexTooltipPlacement.BELOW,
            resolved.placement,
        )
        assertFalse(resolved.clipped)
    }

    @Test
    fun preferenceOrderWinsWhenSeveralSidesFit() {
        val resolved =
            resolveVexAnchoredTooltip(
                anchorBounds =
                    IntRect(
                        left = 150,
                        top = 300,
                        right = 250,
                        bottom = 350,
                    ),
                windowSize = IntSize(500, 800),
                popupSize = IntSize(180, 120),
                layoutDirection = LayoutDirection.Ltr,
                marginPx = 4,
                preferred =
                    listOf(
                        VexTooltipPlacement.ABOVE,
                        VexTooltipPlacement.BELOW,
                    ),
            )

        assertEquals(
            VexTooltipPlacement.ABOVE,
            resolved.placement,
        )
    }

    @Test
    fun caretPointsBackTowardTheAnchorForResolvedPlacement() {
        assertEquals(
            VexTooltipCaretDirection.DOWN,
            vexTooltipCaretDirection(
                VexTooltipPlacement.ABOVE,
                LayoutDirection.Ltr,
            ),
        )
        assertEquals(
            VexTooltipCaretDirection.UP,
            vexTooltipCaretDirection(
                VexTooltipPlacement.BELOW,
                LayoutDirection.Ltr,
            ),
        )
        assertEquals(
            VexTooltipCaretDirection.RIGHT,
            vexTooltipCaretDirection(
                VexTooltipPlacement.START,
                LayoutDirection.Ltr,
            ),
        )
        assertEquals(
            VexTooltipCaretDirection.LEFT,
            vexTooltipCaretDirection(
                VexTooltipPlacement.START,
                LayoutDirection.Rtl,
            ),
        )
    }

    @Test
    fun bottomAnchorReorientsAboveWithoutChangingTargetIdentity() {
        val resolved =
            resolveVexAnchoredTooltip(
                anchorBounds =
                    IntRect(
                        left = 100,
                        top = 700,
                        right = 220,
                        bottom = 750,
                    ),
                windowSize = IntSize(400, 800),
                popupSize = IntSize(180, 120),
                layoutDirection = LayoutDirection.Ltr,
                marginPx = 12,
            )

        assertEquals(
            VexTooltipPlacement.ABOVE,
            resolved.placement,
        )
        assertFalse(resolved.clipped)
    }

    @Test
    fun logicalStartRespectsRtl() {
        val resolved =
            resolveVexAnchoredTooltip(
                anchorBounds =
                    IntRect(
                        left = 40,
                        top = 300,
                        right = 100,
                        bottom = 360,
                    ),
                windowSize = IntSize(600, 500),
                popupSize = IntSize(180, 100),
                layoutDirection = LayoutDirection.Rtl,
                marginPx = 12,
                preferred =
                    listOf(
                        VexTooltipPlacement.START,
                    ),
            )

        assertEquals(
            VexTooltipPlacement.START,
            resolved.placement,
        )
        assertTrue(
            resolved.offset.x >
                100,
        )
    }

    @Test
    fun impossibleFitIsExplicitFallbackRatherThanUnsafePreferredClaim() {
        val resolved =
            resolveVexAnchoredTooltip(
                anchorBounds =
                    IntRect(
                        left = 20,
                        top = 20,
                        right = 40,
                        bottom = 40,
                    ),
                windowSize = IntSize(120, 120),
                popupSize = IntSize(200, 200),
                layoutDirection = LayoutDirection.Ltr,
                marginPx = 12,
            )

        assertEquals(
            VexTooltipPlacement.FALLBACK,
            resolved.placement,
        )
        assertTrue(resolved.clipped)
    }
}

// [VXG RealForever]
