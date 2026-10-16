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
    fun emptyQueueMakesQueueControlsUnavailable() {
        val state =
            vexStreamQueueState(
                mediaItemCount = 0,
                previousAvailable = true,
                nextAvailable = true,
                clearAvailable = true,
            )

        assertSame(
            VexStreamQueueState.Empty,
            state,
        )
        assertFalse(
            controls.projection(state)[
                VexStreamQueueControl.Previous
            ].isAvailable,
        )
        assertFalse(
            controls.projection(state)[
                VexStreamQueueControl.Next
            ].isAvailable,
        )
        assertFalse(
            controls.projection(state)[
                VexStreamQueueControl.Clear
            ].isAvailable,
        )
    }

    @Test
    fun nonEmptyQueueProjectsControlsIndependently() {
        val state =
            vexStreamQueueState(
                mediaItemCount = 3,
                previousAvailable = false,
                nextAvailable = true,
                clearAvailable = true,
            )

        assertTrue(
            state is
                VexStreamQueueState.NonEmpty,
        )
        assertFalse(
            controls.projection(state)[
                VexStreamQueueControl.Previous
            ].isAvailable,
        )
        assertSame(
            VexStreamQueueAct.Next,
            controls.projection(state)[
                VexStreamQueueControl.Next
            ].act,
        )
        assertSame(
            VexStreamQueueAct.Clear,
            controls.projection(state)[
                VexStreamQueueControl.Clear
            ].act,
        )
    }

    @Test
    fun neighborActsStillResolveToNeighborEffectsWithoutChangingStateKind() {
        val state =
            VexStreamQueueState.NonEmpty(
                previousAvailable = true,
                nextAvailable = true,
                clearAvailable = true,
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

    @Test
    fun clearAttemptsProviderMutationWithoutClaimingEmptyBeforeObservation() {
        val state =
            VexStreamQueueState.NonEmpty(
                previousAvailable = true,
                nextAvailable = true,
                clearAvailable = true,
            )

        val clear =
            behavior.resolve(
                current = state,
                act =
                    VexStreamQueueAct.Clear,
            )

        assertSame(
            VexStreamQueueEffect.ClearQueue,
            clear?.effect,
        )
        assertSame(
            VexStreamQueueState.NonEmpty::class,
            clear?.admittedStateKind,
        )
    }
}

// [VXG RealForever]
