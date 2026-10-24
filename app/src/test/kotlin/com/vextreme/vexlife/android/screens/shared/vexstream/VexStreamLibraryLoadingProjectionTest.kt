package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamLibraryLoadState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamLibraryLoadingProjectionTest {
    @Test
    fun restoringLocalTruthProjectsAsLoading() {
        val projection =
            VexStreamMusicState(
                libraryLoadState =
                    VexStreamLibraryLoadState.RESTORING_LOCAL,
            ).toVexStreamLibraryProjection(
                searchQuery = "",
            )

        assertEquals(
            VexStreamLibraryLoadState.RESTORING_LOCAL,
            projection.loadState,
        )
        assertTrue(
            projection.isLoading,
        )
        assertFalse(
            projection.canShowTerminalEmptyState,
        )
    }

    @Test
    fun readyTruthProjectsAsTerminalReady() {
        val projection =
            VexStreamMusicState(
                libraryLoadState =
                    VexStreamLibraryLoadState.READY,
            ).toVexStreamLibraryProjection(
                searchQuery = "",
            )

        assertEquals(
            VexStreamLibraryLoadState.READY,
            projection.loadState,
        )
        assertFalse(
            projection.isLoading,
        )
        assertTrue(
            projection.canShowTerminalEmptyState,
        )
    }
}

// [VXG RealForever]
