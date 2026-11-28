package com.vextreme.vexlife.android.elements.contextdock

import org.junit.Assert.assertEquals
import org.junit.Test

class VexContextDockTest {
    @Test
    fun preferredSemanticPageSurvivesOrderChanges() {
        val maps =
            VexContextDockPageRef(
                "vexmaps.map-style",
            )
        val music =
            VexContextDockPageRef(
                "vexstream.now-playing",
            )

        assertEquals(
            0,
            resolveVexContextDockPageIndex(
                pageRefs = listOf(maps),
                preferredPageRef = maps,
            ),
        )
        assertEquals(
            1,
            resolveVexContextDockPageIndex(
                pageRefs =
                    listOf(
                        music,
                        maps,
                    ),
                preferredPageRef = maps,
            ),
        )
    }

    @Test
    fun missingPreferredPageFallsBackWithoutReusingItsOldIndex() {
        val maps =
            VexContextDockPageRef(
                "vexmaps.map-style",
            )
        val music =
            VexContextDockPageRef(
                "vexstream.now-playing",
            )

        assertEquals(
            0,
            resolveVexContextDockPageIndex(
                pageRefs = listOf(maps),
                preferredPageRef = music,
            ),
        )
    }

    @Test
    fun emptyPageSetHasSafeZeroIndex() {
        assertEquals(
            0,
            resolveVexContextDockPageIndex(
                pageRefs = emptyList(),
                preferredPageRef = null,
            ),
        )
    }
    @Test
    fun layeredDragRaisesAndLowersOneLevelAtATime() {
        val levels = VexContextDockLevels.Layered
        val threshold = 28f

        assertEquals(VexContextDockReveal.Peek, levels.afterDrag(VexContextDockReveal.Tucked, -40f, threshold))
        assertEquals(VexContextDockReveal.Open, levels.afterDrag(VexContextDockReveal.Peek, -40f, threshold))
        assertEquals(VexContextDockReveal.Full, levels.afterDrag(VexContextDockReveal.Open, -400f, threshold))
        assertEquals(VexContextDockReveal.Full, levels.afterDrag(VexContextDockReveal.Full, -400f, threshold))
        assertEquals(VexContextDockReveal.Open, levels.afterDrag(VexContextDockReveal.Full, 40f, threshold))
        assertEquals(VexContextDockReveal.Tucked, levels.afterDrag(VexContextDockReveal.Peek, 40f, threshold))
        assertEquals(VexContextDockReveal.Tucked, levels.afterDrag(VexContextDockReveal.Tucked, 400f, threshold))
    }

    @Test
    fun smallDragNeverChangesReveal() {
        val levels = VexContextDockLevels.Layered

        VexContextDockReveal.entries.forEach { from ->
            assertEquals(from, levels.afterDrag(from, -27f, 28f))
            assertEquals(from, levels.afterDrag(from, 27f, 28f))
        }
    }

    @Test
    fun compactDockKeepsTheOriginalTwoLevels() {
        val levels = VexContextDockLevels.Compact

        assertEquals(
            listOf(VexContextDockReveal.Peek, VexContextDockReveal.Open),
            levels.admitted,
        )
        assertEquals(VexContextDockReveal.Peek, levels.afterDrag(VexContextDockReveal.Peek, 400f, 28f))
        assertEquals(VexContextDockReveal.Open, levels.afterDrag(VexContextDockReveal.Open, -400f, 28f))
        // A hoisted state from a layered host is coerced, never rendered at an unadmitted level.
        assertEquals(VexContextDockReveal.Peek, levels.coerce(VexContextDockReveal.Tucked))
        assertEquals(VexContextDockReveal.Open, levels.coerce(VexContextDockReveal.Full))
    }

    @Test
    fun handleTapIsAQuickLookNeverForegroundOrDismiss() {
        val levels = VexContextDockLevels.Layered

        assertEquals(VexContextDockReveal.Peek, levels.afterHandleTap(VexContextDockReveal.Tucked))
        assertEquals(VexContextDockReveal.Open, levels.afterHandleTap(VexContextDockReveal.Peek))
        assertEquals(VexContextDockReveal.Peek, levels.afterHandleTap(VexContextDockReveal.Open))
        assertEquals(VexContextDockReveal.Open, levels.afterHandleTap(VexContextDockReveal.Full))
    }

    @Test
    fun expandedCompatibilityReadingMatchesTheOriginalDock() {
        assertEquals(
            listOf(false, false, true, true),
            VexContextDockReveal.entries.map(VexContextDockReveal::isExpanded),
        )
    }
}
