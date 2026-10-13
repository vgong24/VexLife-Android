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
}

// [VXG RealForever]
