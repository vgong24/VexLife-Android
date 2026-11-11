package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamRepeatFamilyTest {
    @Test
    fun disabledRepeatAuthorsLatestEnableWithoutClaimingProviderOutcome() {
        val initial =
            VexStreamRepeatState.Disabled(
                toggleAvailable =
                    true,
            )
        val family =
            VexStreamRepeatFamily(
                initialState =
                    initial,
            )
        val accepted =
            requireNotNull(
                family.activate(
                    VexStreamRepeatControl.Toggle,
                ),
            )
        val begin =
            requireNotNull(
                accepted.realizationBegin,
            )

        assertSame(
            VexStreamRepeatEffect.RequestEnableAll,
            begin.effect,
        )
        assertSame(
            VexExecution.Approach.Latest,
            begin.approach,
        )
        assertSame(
            initial,
            family.currentState,
        )
    }

    @Test
    fun enabledRepeatAuthorsDisable() {
        val family =
            VexStreamRepeatFamily(
                initialState =
                    VexStreamRepeatState.Enabled(
                        toggleAvailable =
                            true,
                    ),
            )
        val accepted =
            requireNotNull(
                family.activate(
                    VexStreamRepeatControl.Toggle,
                ),
            )

        assertSame(
            VexStreamRepeatEffect.RequestDisable,
            requireNotNull(
                accepted.realizationBegin,
            ).effect,
        )
    }

    @Test
    fun unavailableRepeatRejectsToggle() {
        val family =
            VexStreamRepeatFamily(
                initialState =
                    VexStreamRepeatState.Disabled(
                        toggleAvailable =
                            false,
                    ),
            )

        assertSame(
            null,
            family.activate(
                VexStreamRepeatControl.Toggle,
            ),
        )
        assertFalse(
            family.currentProjection
                .toggleAvailable,
        )
    }

    @Test
    fun outsideRepeatSignalReturnsAsSemanticState() {
        val family =
            VexStreamRepeatFamily()

        val enabled =
            family.observeStore(
                VexStreamPlayerPartner.Signal.Repeat(
                    enabled =
                        true,
                    toggleAvailable =
                        true,
                ),
            )

        assertTrue(
            enabled is
                VexStreamRepeatState.Enabled,
        )
        assertTrue(
            family.currentProjection
                .toggleAvailable,
        )
    }
}

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY][PROOF]
