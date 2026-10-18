package com.vextreme.vexlife.android.vexstream.queue

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
                shuffleAvailable = true,
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
                VexStreamQueueControl.Shuffle
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
                mediaItemCount = 4,
                previousAvailable = false,
                nextAvailable = true,
                shuffleAvailable = true,
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
            VexStreamQueueAct.Shuffle,
            controls.projection(state)[
                VexStreamQueueControl.Shuffle
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
                shuffleAvailable = true,
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
    fun shuffleAttemptsFutureMutationWithoutClaimingAnotherStateKind() {
        val state =
            VexStreamQueueState.NonEmpty(
                previousAvailable = true,
                nextAvailable = true,
                shuffleAvailable = true,
                clearAvailable = true,
            )

        val shuffle =
            behavior.resolve(
                current = state,
                act =
                    VexStreamQueueAct.Shuffle,
            )

        assertSame(
            VexStreamQueueEffect.ShuffleFuture,
            shuffle?.effect,
        )
        assertSame(
            VexStreamQueueState.NonEmpty::class,
            shuffle?.admittedStateKind,
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
    fun shuffleTargetOrderPreservesPlayedPrefixAndChangesFutureOrder() {
        val order =
            vexStreamQueueShuffleTargetOrder(
                itemCount = 5,
                currentIndex = 1,
                random = Random(0),
            )

        assertEquals(
            listOf(
                0,
                1,
            ),
            order.take(2),
        )
        assertEquals(
            setOf(
                2,
                3,
                4,
            ),
            order
                .drop(2)
                .toSet(),
        )
        assertTrue(
            order.drop(2) !=
                listOf(
                    2,
                    3,
                    4,
                ),
        )
    }

    @Test
    fun shuffleCanReorderTheWholeQueueBeforePlaybackStarts() {
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
    fun largeQueueShuffleKeepsPrefixAndFormsOneCompleteFuturePermutation() {
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

    @Test
    fun clearAttemptsProviderMutationWithoutClaimingEmptyBeforeObservation() {
        val state =
            VexStreamQueueState.NonEmpty(
                previousAvailable = true,
                nextAvailable = true,
                shuffleAvailable = true,
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
