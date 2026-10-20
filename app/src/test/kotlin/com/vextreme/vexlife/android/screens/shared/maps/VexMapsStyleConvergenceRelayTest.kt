package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VexMapsStyleConvergenceRelayTest {
    @Test
    fun selectedStyleRequiresApplyUntilExactRequestLoads() {
        val relay =
            VexMapsStyleConvergenceRelay()

        relay.styleSelected(
            styleRef = "dark",
            styleUri = "https://example.test/dark",
        )

        val selected =
            assertNotNullSelection(
                relay.currentOutput.selection,
            )

        assertEquals(
            VexMapsStyleConvergenceStatus
                .NEEDS_APPLY,
            relay.currentOutput.status,
        )

        relay.applyStarted(selected)

        assertEquals(
            VexMapsStyleConvergenceStatus
                .APPLYING,
            relay.currentOutput.status,
        )

        relay.styleLoaded(selected)

        assertEquals(
            VexMapsStyleConvergenceStatus
                .APPLIED,
            relay.currentOutput.status,
        )
    }

    @Test
    fun staleCompletionCannotValidateNewerSelection() {
        val relay =
            VexMapsStyleConvergenceRelay()

        relay.styleSelected(
            styleRef = "liberty",
            styleUri =
                "https://example.test/liberty",
        )
        val liberty =
            assertNotNullSelection(
                relay.currentOutput.selection,
            )
        relay.applyStarted(liberty)

        relay.styleSelected(
            styleRef = "dark",
            styleUri =
                "https://example.test/dark",
        )

        relay.styleLoaded(liberty)

        assertEquals(
            "dark",
            relay.currentOutput
                .selection
                ?.styleRef,
        )
        assertEquals(
            VexMapsStyleConvergenceStatus
                .NEEDS_APPLY,
            relay.currentOutput.status,
        )
    }

    @Test
    fun failureIsTerminalForCurrentRevisionUntilMeaningChanges() {
        val relay =
            VexMapsStyleConvergenceRelay()

        relay.styleSelected(
            styleRef = "dark",
            styleUri =
                "https://example.test/dark",
        )
        val selection =
            assertNotNullSelection(
                relay.currentOutput.selection,
            )
        relay.applyStarted(selection)
        relay.currentApplyFailed(
            "style source unavailable",
        )

        assertEquals(
            VexMapsStyleConvergenceStatus
                .FAILED,
            relay.currentOutput.status,
        )
        assertEquals(
            "style source unavailable",
            relay.currentOutput.failureMessage,
        )

        relay.styleSelected(
            styleRef = "liberty",
            styleUri =
                "https://example.test/liberty",
        )

        assertEquals(
            VexMapsStyleConvergenceStatus
                .NEEDS_APPLY,
            relay.currentOutput.status,
        )
    }

    private fun assertNotNullSelection(
        selection: VexMapsStyleSelection?,
    ): VexMapsStyleSelection {
        assertNotNull(selection)
        return requireNotNull(selection)
    }
}

// [VXG RealForever]
