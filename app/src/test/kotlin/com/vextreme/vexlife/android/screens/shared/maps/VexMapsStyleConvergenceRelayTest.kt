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
            VexMapsStyleConvergenceAvailability
                .NEEDS_APPLY,
            relay.currentOutput.availability,
        )

        relay.applyStarted(selected)

        assertEquals(
            VexMapsStyleConvergenceAvailability
                .APPLYING,
            relay.currentOutput.availability,
        )

        relay.styleLoaded(selected)

        assertEquals(
            VexMapsStyleConvergenceAvailability
                .APPLIED,
            relay.currentOutput.availability,
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
            VexMapsStyleConvergenceAvailability
                .NEEDS_APPLY,
            relay.currentOutput.availability,
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
            VexMapsStyleConvergenceAvailability
                .FAILED,
            relay.currentOutput.availability,
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
            VexMapsStyleConvergenceAvailability
                .NEEDS_APPLY,
            relay.currentOutput.availability,
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
