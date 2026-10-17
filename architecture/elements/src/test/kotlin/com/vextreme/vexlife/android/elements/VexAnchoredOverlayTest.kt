package com.vextreme.vexlife.android.elements

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexAnchoredOverlayTest {
    @Test
    fun choosesTheFirstPreferredSideThatSafelyFits() {
        val resolved =
            resolveVexAnchoredOverlay(
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
            VexOverlayPlacement.BELOW,
            resolved.placement,
        )
        assertFalse(resolved.clipped)
    }

    @Test
    fun preferenceOrderWinsWhenSeveralSidesFit() {
        val resolved =
            resolveVexAnchoredOverlay(
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
                        VexOverlayPlacement.ABOVE,
                        VexOverlayPlacement.BELOW,
                    ),
            )

        assertEquals(
            VexOverlayPlacement.ABOVE,
            resolved.placement,
        )
    }

    @Test
    fun caretPointsBackTowardTheAnchorForResolvedPlacement() {
        assertEquals(
            VexOverlayCaretDirection.DOWN,
            vexOverlayCaretDirection(
                VexOverlayPlacement.ABOVE,
                LayoutDirection.Ltr,
            ),
        )
        assertEquals(
            VexOverlayCaretDirection.UP,
            vexOverlayCaretDirection(
                VexOverlayPlacement.BELOW,
                LayoutDirection.Ltr,
            ),
        )
        assertEquals(
            VexOverlayCaretDirection.RIGHT,
            vexOverlayCaretDirection(
                VexOverlayPlacement.START,
                LayoutDirection.Ltr,
            ),
        )
        assertEquals(
            VexOverlayCaretDirection.LEFT,
            vexOverlayCaretDirection(
                VexOverlayPlacement.START,
                LayoutDirection.Rtl,
            ),
        )
    }

    @Test
    fun bottomAnchorReorientsAboveWithoutChangingTargetIdentity() {
        val resolved =
            resolveVexAnchoredOverlay(
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
            VexOverlayPlacement.ABOVE,
            resolved.placement,
        )
        assertFalse(resolved.clipped)
    }

    @Test
    fun logicalStartRespectsRtl() {
        val resolved =
            resolveVexAnchoredOverlay(
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
                        VexOverlayPlacement.START,
                    ),
            )

        assertEquals(
            VexOverlayPlacement.START,
            resolved.placement,
        )
        assertTrue(
            resolved.offset.x >
                100,
        )
    }


    @Test
    fun centeredPopupKeepsCaretAtItsOwnCenter() {
        val resolved =
            resolveVexAnchoredOverlay(
                anchorBounds =
                    IntRect(
                        left = 150,
                        top = 100,
                        right = 190,
                        bottom = 140,
                    ),
                windowSize = IntSize(400, 800),
                popupSize = IntSize(180, 120),
                layoutDirection = LayoutDirection.Ltr,
                marginPx = 12,
                caretEdgeInsetPx = 10,
            )

        assertEquals(
            0,
            resolved.caretCrossAxisOffsetPx,
        )
    }

    @Test
    fun clampedPopupMovesCaretBackToAnchorCenter() {
        val anchor =
            IntRect(
                left = 15,
                top = 100,
                right = 35,
                bottom = 140,
            )
        val resolved =
            resolveVexAnchoredOverlay(
                anchorBounds = anchor,
                windowSize = IntSize(300, 800),
                popupSize = IntSize(180, 120),
                layoutDirection = LayoutDirection.Ltr,
                marginPx = 12,
                caretEdgeInsetPx = 10,
            )

        assertEquals(
            VexOverlayPlacement.BELOW,
            resolved.placement,
        )
        assertEquals(
            anchor.left +
                anchor.width / 2,
            resolved.offset.x +
                180 / 2 +
                resolved.caretCrossAxisOffsetPx,
        )
    }

    @Test
    fun impossibleFitIsExplicitFallbackRatherThanUnsafePreferredClaim() {
        val resolved =
            resolveVexAnchoredOverlay(
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
            VexOverlayPlacement.FALLBACK,
            resolved.placement,
        )
        assertTrue(resolved.clipped)
    }
}

// [VXG RealForever]
