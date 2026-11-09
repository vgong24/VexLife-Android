package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamRepeatFamilyTest {
    @Test
    fun partnerRepeatDisabledProjectsToggleAvailability() {
        val state =
            repeatState(
                enabled = false,
                toggleAvailable = true,
            )
        val family =
            VexStreamRepeatFamily(
                state,
            )

        assertTrue(
            family.currentRepeatProjection[
                VexStreamRepeatControl.Toggle
            ],
        )
        assertSame(
            VexStreamRepeatEffect.RequestEnableAll,
            family.activate(
                VexStreamRepeatControl.Toggle,
            )?.effect,
        )
    }

    @Test
    fun partnerRepeatEnabledRequestsDisable() {
        val family =
            VexStreamRepeatFamily(
                repeatState(
                    enabled = true,
                    toggleAvailable = true,
                ),
            )

        val act =
            family.activate(
                VexStreamRepeatControl.Toggle,
            )

        assertSame(
            VexStreamRepeatEffect.RequestDisable,
            act?.effect,
        )
        assertEquals(
            VexExecution.Approach.Latest,
            act?.approach,
        )
    }

    @Test
    fun unavailableProviderToggleRejectsControl() {
        val family =
            VexStreamRepeatFamily(
                repeatState(
                    enabled = false,
                    toggleAvailable = false,
                ),
            )

        assertFalse(
            family.currentRepeatProjection[
                VexStreamRepeatControl.Toggle
            ],
        )
        assertSame(
            null,
            family.activate(
                VexStreamRepeatControl.Toggle,
            ),
        )
    }

    private fun repeatState(
        enabled:
            Boolean,
        toggleAvailable:
            Boolean,
    ):
        VexStreamRepeatState =
        VexStreamPlayerPartner.Signal.Repeat(
            enabled =
                enabled,
            toggleAvailable =
                toggleAvailable,
        )
            .toVexStreamRepeatState()
}

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY][PROOF]
