package com.vextreme.vexlife.android.vexstream.queue

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamQueueFamilyTest {
    @Test
    fun emptyQueueRejectsFiniteQueueControls() {
        val state =
            vexStreamQueueState(
                mediaItemCount = 0,
                previousAvailable = true,
                nextAvailable = true,
                shuffleAvailable = true,
                clearAvailable = true,
            )
        val family =
            VexStreamQueueFamily(
                state,
            )

        assertSame(
            VexStreamQueueFamily.State.Empty,
            state,
        )
        VexStreamQueueFamily.Control.entries.forEach { control ->
            assertFalse(
                state.isAvailable(
                    control,
                ),
            )
            assertNull(
                family.activate(
                    control,
                ),
            )
        }
    }

    @Test
    fun nonEmptyQueueAuthorsOnlyAvailableProviderEffectsWithoutClaimingOutcome() {
        val state =
            VexStreamQueueFamily.State.NonEmpty(
                previousAvailable = false,
                nextAvailable = true,
                shuffleAvailable = true,
                clearAvailable = true,
            )
        val family =
            VexStreamQueueFamily(
                state,
            )

        assertFalse(
            state.isAvailable(
                VexStreamQueueFamily.Control.Previous,
            ),
        )
        assertNull(
            family.activate(
                VexStreamQueueFamily.Control.Previous,
            ),
        )
        assertSame(
            VexStreamQueueFamily.Effect.SeekNext,
            family.activate(
                VexStreamQueueFamily.Control.Next,
            )?.effect,
        )
        assertSame(
            VexStreamQueueFamily.Effect.ShuffleFuture,
            family.activate(
                VexStreamQueueFamily.Control.Shuffle,
            )?.effect,
        )
        assertSame(
            VexStreamQueueFamily.Effect.ClearQueue,
            family.activate(
                VexStreamQueueFamily.Control.Clear,
            )?.effect,
        )
        assertSame(
            state,
            family.currentState,
        )
    }

    @Test
    fun providerObservationOwnsResultingQueueState() {
        val family =
            VexStreamQueueFamily(
                VexStreamQueueFamily.State.NonEmpty(
                    previousAvailable = true,
                    nextAvailable = false,
                    shuffleAvailable = false,
                    clearAvailable = true,
                ),
            )

        val requested =
            family.activate(
                VexStreamQueueFamily.Control.Clear,
            )

        assertSame(
            VexStreamQueueFamily.Effect.ClearQueue,
            requested?.effect,
        )
        assertTrue(
            family.currentState is
                VexStreamQueueFamily.State.NonEmpty,
        )

        family.observe(
            VexStreamQueueFamily.State.Empty,
        )

        assertSame(
            VexStreamQueueFamily.State.Empty,
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
        assertTrue(
            vexStreamQueueShuffleAvailable(
                mediaItemCount = 2,
                currentIndex = -1,
                canChangeMediaItems = true,
            ),
        )
    }

    @Test
    fun shufflePreservesPlayedPrefixAndPermutesCompleteFuture() {
        val natural =
            (0 until 5).toList()
        val shuffled =
            vexStreamQueueShuffleTargetOrder(
                itemCount = natural.size,
                currentIndex = 1,
                random = Random(0),
            )

        assertEquals(
            natural.take(2),
            shuffled.take(2),
        )
        assertEquals(
            natural.toSet(),
            shuffled.toSet(),
        )
        assertNotEquals(
            natural.drop(2),
            shuffled.drop(2),
        )
    }

    @Test
    fun shuffleCanPermuteWholeQueueBeforePlaybackStarts() {
        val natural =
            (0 until 5).toList()
        val shuffled =
            vexStreamQueueShuffleTargetOrder(
                itemCount = natural.size,
                currentIndex = -1,
                random = Random(11),
            )

        assertEquals(
            natural.toSet(),
            shuffled.toSet(),
        )
        assertNotEquals(
            natural,
            shuffled,
        )
    }

    @Test
    fun largeQueueShuffleKeepsPrefixAndCompleteMembership() {
        val itemCount = 640
        val currentIndex = 19
        val natural =
            (0 until itemCount).toList()
        val shuffled =
            vexStreamQueueShuffleTargetOrder(
                itemCount = itemCount,
                currentIndex = currentIndex,
                random = Random(24),
            )

        assertEquals(
            natural.take(
                currentIndex + 1,
            ),
            shuffled.take(
                currentIndex + 1,
            ),
        )
        assertEquals(
            natural.toSet(),
            shuffled.toSet(),
        )
        assertNotEquals(
            natural.drop(
                currentIndex + 1,
            ),
            shuffled.drop(
                currentIndex + 1,
            ),
        )
    }
}

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY]
