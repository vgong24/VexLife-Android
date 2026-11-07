package com.vextreme.vexlife.android.vexstream.repeat

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamRepeatFamilyTest {
    @Test
    fun providerRepeatOffMapsToDisabledAndToggleRequestsEnableAll() {
        val state =
            vexStreamRepeatState(
                repeatMode =
                    Player.REPEAT_MODE_OFF,
                toggleAvailable =
                    true,
            )
        val family =
            VexStreamRepeatFamily(
                initialState =
                    state,
            )

        assertTrue(
            state is
                VexStreamRepeatFamily.State.Disabled,
        )
        assertTrue(
            family.isAvailable(
                VexStreamRepeatFamily.Control.Toggle,
            ),
        )

        val act =
            family.activate(
                VexStreamRepeatFamily.Control.Toggle,
            )

        assertSame(
            VexStreamRepeatFamily.Effect.RequestEnableAll,
            act?.effect,
        )
        assertNull(
            act?.relationshipId,
        )
        assertSame(
            state,
            family.currentState,
        )
    }

    @Test
    fun providerRepeatOneAndAllBothMapToEnabled() {
        val repeatOne =
            vexStreamRepeatState(
                repeatMode =
                    Player.REPEAT_MODE_ONE,
                toggleAvailable =
                    true,
            )
        val repeatAll =
            vexStreamRepeatState(
                repeatMode =
                    Player.REPEAT_MODE_ALL,
                toggleAvailable =
                    true,
            )

        assertTrue(
            repeatOne is
                VexStreamRepeatFamily.State.Enabled,
        )
        assertTrue(
            repeatAll is
                VexStreamRepeatFamily.State.Enabled,
        )

        val family =
            VexStreamRepeatFamily(
                initialState =
                    repeatOne,
            )
        val act =
            family.activate(
                VexStreamRepeatFamily.Control.Toggle,
            )

        assertSame(
            VexStreamRepeatFamily.Effect.RequestDisable,
            act?.effect,
        )
        assertSame(
            repeatOne,
            family.currentState,
        )
    }

    @Test
    fun providerCommandAvailabilityOwnsToggleAvailability() {
        val family =
            VexStreamRepeatFamily(
                initialState =
                    VexStreamRepeatFamily.State.Disabled(
                        toggleAvailable =
                            false,
                    ),
            )

        assertFalse(
            family.isAvailable(
                VexStreamRepeatFamily.Control.Toggle,
            ),
        )
        assertNull(
            family.activate(
                VexStreamRepeatFamily.Control.Toggle,
            ),
        )
    }

    @Test
    fun providerObservationChangesStateSeparatelyFromRequestedEffect() {
        val family =
            VexStreamRepeatFamily(
                initialState =
                    VexStreamRepeatFamily.State.Disabled(
                        toggleAvailable =
                            true,
                    ),
            )

        val requested =
            family.activate(
                VexStreamRepeatFamily.Control.Toggle,
            )

        assertSame(
            VexStreamRepeatFamily.Effect.RequestEnableAll,
            requested?.effect,
        )
        assertTrue(
            family.currentState is
                VexStreamRepeatFamily.State.Disabled,
        )

        family.observe(
            VexStreamRepeatFamily.State.Enabled(
                toggleAvailable =
                    true,
            ),
        )

        assertTrue(
            family.currentState is
                VexStreamRepeatFamily.State.Enabled,
        )
    }
}

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
