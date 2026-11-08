package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamRepeatBehaviorTest {
    private val behavior =
        vexStreamRepeatBehavior()

    private val controls =
        behavior.vexStreamRepeatControls()

    @Test
    fun partnerRepeatDisabledProjectsEnableWhenToggleIsAvailable() {
        val state =
            repeatState(
                enabled =
                    false,
                toggleAvailable =
                    true,
            )

        assertTrue(
            state is
                VexStreamRepeatState.Disabled,
        )
        assertSame(
            VexStreamRepeatAct.Enable,
            controls.projection(
                state,
            )[
                VexStreamRepeatControl.Toggle
            ].act,
        )
    }

    @Test
    fun partnerRepeatEnabledProjectsDisable() {
        val state =
            repeatState(
                enabled =
                    true,
                toggleAvailable =
                    true,
            )

        assertTrue(
            state is
                VexStreamRepeatState.Enabled,
        )
        assertSame(
            VexStreamRepeatAct.Disable,
            controls.projection(
                state,
            )[
                VexStreamRepeatControl.Toggle
            ].act,
        )
    }

    @Test
    fun partnerCapabilityOwnsToggleAvailability() {
        val state =
            repeatState(
                enabled =
                    false,
                toggleAvailable =
                    false,
            )

        assertFalse(
            controls.projection(
                state,
            )[
                VexStreamRepeatControl.Toggle
            ].isAvailable,
        )
    }

    @Test
    fun enableAndDisableRequestProviderEffectsWithoutClaimingOutcome() {
        val disabled =
            VexStreamRepeatState.Disabled(
                toggleAvailable = true,
            )
        val enabled =
            VexStreamRepeatState.Enabled(
                toggleAvailable = true,
            )

        val enable =
            behavior.resolve(
                current = disabled,
                act =
                    VexStreamRepeatAct.Enable,
            )
        val disable =
            behavior.resolve(
                current = enabled,
                act =
                    VexStreamRepeatAct.Disable,
            )

        assertSame(
            VexStreamRepeatEffect.RequestEnableAll,
            enable?.effect,
        )
        assertSame(
            VexStreamRepeatState.Disabled::class,
            enable?.admittedStateKind,
        )
        assertSame(
            VexStreamRepeatEffect.RequestDisable,
            disable?.effect,
        )
        assertSame(
            VexStreamRepeatState.Enabled::class,
            disable?.admittedStateKind,
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

// [VXG RealForever][VEXSTREAM][REPEAT][PLAYER-PARTNER]
