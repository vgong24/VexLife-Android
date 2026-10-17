package com.vextreme.vexlife.android.vexstream.repeat

import androidx.media3.common.Player
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
    fun repeatOffProjectsEnableWhenProviderAllowsToggle() {
        val state =
            vexStreamRepeatState(
                repeatMode =
                    Player.REPEAT_MODE_OFF,
                toggleAvailable = true,
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
    fun providerRepeatOneAndAllBothProjectAsEnabled() {
        val repeatOne =
            vexStreamRepeatState(
                repeatMode =
                    Player.REPEAT_MODE_ONE,
                toggleAvailable = true,
            )
        val repeatAll =
            vexStreamRepeatState(
                repeatMode =
                    Player.REPEAT_MODE_ALL,
                toggleAvailable = true,
            )

        assertTrue(
            repeatOne is
                VexStreamRepeatState.Enabled,
        )
        assertTrue(
            repeatAll is
                VexStreamRepeatState.Enabled,
        )
        assertSame(
            VexStreamRepeatAct.Disable,
            controls.projection(
                repeatOne,
            )[
                VexStreamRepeatControl.Toggle
            ].act,
        )
        assertSame(
            VexStreamRepeatAct.Disable,
            controls.projection(
                repeatAll,
            )[
                VexStreamRepeatControl.Toggle
            ].act,
        )
    }

    @Test
    fun providerCommandAvailabilityOwnsToggleAvailability() {
        val state =
            vexStreamRepeatState(
                repeatMode =
                    Player.REPEAT_MODE_OFF,
                toggleAvailable = false,
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
}

// [VXG RealForever]
