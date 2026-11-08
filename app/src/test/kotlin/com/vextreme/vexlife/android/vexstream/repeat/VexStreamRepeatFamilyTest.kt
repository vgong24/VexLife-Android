package com.vextreme.vexlife.android.vexstream.repeat

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamRepeatFamilyTest {
    @Test
    fun providerModesMapToCurrentRepeatTruthAndAvailability() {
        assertEquals(
            VexStreamRepeatFamily.State.Disabled(
                toggleAvailable = true,
            ),
            vexStreamRepeatState(
                Player.REPEAT_MODE_OFF,
                toggleAvailable = true,
            ),
        )
        assertEquals(
            VexStreamRepeatFamily.State.Enabled(
                toggleAvailable = true,
            ),
            vexStreamRepeatState(
                Player.REPEAT_MODE_ONE,
                toggleAvailable = true,
            ),
        )
        assertEquals(
            VexStreamRepeatFamily.State.Enabled(
                toggleAvailable = true,
            ),
            vexStreamRepeatState(
                Player.REPEAT_MODE_ALL,
                toggleAvailable = true,
            ),
        )

        val blocked =
            VexStreamRepeatFamily(
                VexStreamRepeatFamily.State.Disabled(
                    toggleAvailable = false,
                ),
            )

        assertFalse(
            blocked.isAvailable(
                VexStreamRepeatFamily.Control.Toggle,
            ),
        )
        assertNull(
            blocked.activate(
                VexStreamRepeatFamily.Control.Toggle,
            ),
        )
    }

    @Test
    fun toggleAuthorsProviderEffectWhileObservationOwnsResultingState() {
        val family =
            VexStreamRepeatFamily(
                VexStreamRepeatFamily.State.Disabled(
                    toggleAvailable = true,
                ),
            )

        val enable =
            family.activate(
                VexStreamRepeatFamily.Control.Toggle,
            )

        assertSame(
            VexStreamRepeatFamily.Effect.RequestEnableAll,
            enable?.effect,
        )
        assertNull(
            enable?.relationshipId,
        )
        assertTrue(
            family.currentState is
                VexStreamRepeatFamily.State.Disabled,
        )

        family.observe(
            VexStreamRepeatFamily.State.Enabled(
                toggleAvailable = true,
            ),
        )

        val disable =
            family.activate(
                VexStreamRepeatFamily.Control.Toggle,
            )

        assertSame(
            VexStreamRepeatFamily.Effect.RequestDisable,
            disable?.effect,
        )
        assertTrue(
            family.currentState is
                VexStreamRepeatFamily.State.Enabled,
        )
    }
}

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
