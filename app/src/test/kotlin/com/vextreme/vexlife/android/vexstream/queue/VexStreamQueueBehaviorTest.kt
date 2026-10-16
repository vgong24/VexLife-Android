package com.vextreme.vexlife.android.vexstream.queue

import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamQueueBehaviorTest {
    private val behavior =
        vexStreamQueueBehavior()

    private val controls =
        behavior.vexStreamQueueControls()

    @Test
    fun emptyQueueMakesNeighborControlsUnavailable() {
        val state =
            vexStreamQueueState(
                mediaItemCount = 0,
                previousAvailable = true,
                nextAvailable = true,
            )

        assertSame(
            VexStreamQueueState.Empty,
            state,
        )
        assertFalse(
            controls.projection(
                state,
            )[
                VexStreamQueueControl.Previous
            ].isAvailable,
        )
        assertFalse(
            controls.projection(
                state,
            )[
                VexStreamQueueControl.Next
            ].isAvailable,
        )
    }

    @Test
    fun nonEmptyQueueProjectsPreviousAndNextIndependently() {
        val state =
            vexStreamQueueState(
                mediaItemCount = 3,
                previousAvailable = false,
                nextAvailable = true,
            )

        assertTrue(
            state is
                VexStreamQueueState.NonEmpty,
        )
        assertFalse(
            controls.projection(
                state,
            )[
                VexStreamQueueControl.Previous
            ].isAvailable,
        )
        assertSame(
            VexStreamQueueAct.Next,
            controls.projection(
                state,
            )[
                VexStreamQueueControl.Next
            ].act,
        )
    }

    @Test
    fun queueActsResolveToNeighborEffectsWithoutChangingStateKind() {
        val state =
            VexStreamQueueState.NonEmpty(
                previousAvailable = true,
                nextAvailable = true,
            )

        val previous =
            behavior.resolve(
                current = state,
                act =
                    VexStreamQueueAct.Previous,
            )
        val next =
            behavior.resolve(
                current = state,
                act =
                    VexStreamQueueAct.Next,
            )

        assertSame(
            VexStreamQueueEffect.SeekPrevious,
            previous?.effect,
        )
        assertSame(
            VexStreamQueueEffect.SeekNext,
            next?.effect,
        )
        assertSame(
            VexStreamQueueState.NonEmpty::class,
            previous?.admittedStateKind,
        )
        assertSame(
            VexStreamQueueState.NonEmpty::class,
            next?.admittedStateKind,
        )
    }
}

// [VXG RealForever]
