package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.execution.VexExecution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamQueueFamilyTest {
    @Test
    fun emptyQueueMakesQueueControlsUnavailable() {
        val family =
            VexStreamQueueFamily(
                VexStreamQueueState.Empty,
            )
        val projection =
            family.currentQueueProjection

        assertFalse(
            projection[
                VexStreamQueueControl.Previous
            ].isAvailable,
        )
        assertFalse(
            projection[
                VexStreamQueueControl.Next
            ].isAvailable,
        )
        assertFalse(
            projection[
                VexStreamQueueControl.Shuffle
            ].isAvailable,
        )
        assertFalse(
            projection[
                VexStreamQueueControl.Clear
            ].isAvailable,
        )
    }

    @Test
    fun nonEmptyQueueProjectsControlsIndependently() {
        val family =
            VexStreamQueueFamily(
                VexStreamQueueState.NonEmpty(
                    previousAvailable = false,
                    nextAvailable = true,
                    shuffleAvailable = true,
                    clearAvailable = true,
                ),
            )
        val projection =
            family.currentQueueProjection

        assertFalse(
            projection[
                VexStreamQueueControl.Previous
            ].isAvailable,
        )
        assertTrue(
            projection[
                VexStreamQueueControl.Next
            ].isAvailable,
        )
        assertTrue(
            projection[
                VexStreamQueueControl.Shuffle
            ].isAvailable,
        )
        assertTrue(
            projection[
                VexStreamQueueControl.Clear
            ].isAvailable,
        )
    }

    @Test
    fun queueControlsRequestProviderEffectsWithoutClaimingOutcome() {
        val state =
            VexStreamQueueState.NonEmpty(
                previousAvailable = true,
                nextAvailable = true,
                shuffleAvailable = true,
                clearAvailable = true,
            )
        val family =
            VexStreamQueueFamily(
                state,
            )

        val previous =
            family.activate(
                VexStreamQueueControl.Previous,
            )
        assertSame(
            VexStreamQueueEffect.SeekPrevious,
            previous?.effect,
        )
        assertEquals(
            VexExecution.Approach.Independent,
            previous?.approach,
        )
        assertSame(
            state,
            family.currentState,
        )

        val clear =
            family.activate(
                VexStreamQueueControl.Clear,
            )
        assertSame(
            VexStreamQueueEffect.ClearQueue,
            clear?.effect,
        )
        assertSame(
            state,
            family.currentState,
        )
    }

    @Test
    fun shuffleAvailabilityRequiresTwoFutureItemsAndPlaylistMutationAuthority() {
        assertFalse(
            vexStreamQueueShuffleAvailable(
                mediaItemCount = 3,
                currentIndex = 0,
                canChangeMediaItems = false,
            ),
        )
        assertFalse(
            vexStreamQueueShuffleAvailable(
                mediaItemCount = 2,
                currentIndex = 0,
                canChangeMediaItems = true,
            ),
        )
        assertTrue(
            vexStreamQueueShuffleAvailable(
                mediaItemCount = 3,
                currentIndex = 0,
                canChangeMediaItems = true,
            ),
        )
    }


}

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY][PROOF]
