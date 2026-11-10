package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.execution.VexExecution
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamQueueFamilyTest {
    @Test
    fun emptyQueueRejectsFiniteQueueControls() {
        val family =
            VexStreamQueueFamily()

        VexStreamQueueControl.entries
            .forEach { control ->
                assertSame(
                    null,
                    family.activate(
                        control,
                    ),
                )
            }

        val projection =
            family.currentQueueProjection
        assertFalse(
            projection.previousAvailable,
        )
        assertFalse(
            projection.nextAvailable,
        )
        assertFalse(
            projection.shuffleAvailable,
        )
        assertFalse(
            projection.clearAvailable,
        )
    }

    @Test
    fun nonEmptyQueueAuthorsExactQueuedEffectsWithoutClaimingOutcome() {
        val state =
            VexStreamQueueState.NonEmpty(
                previousAvailable =
                    true,
                nextAvailable =
                    true,
                shuffleAvailable =
                    true,
                clearAvailable =
                    true,
            )
        val cases =
            listOf(
                VexStreamQueueControl.Previous to
                    VexStreamQueueEffect.SeekPrevious,
                VexStreamQueueControl.Next to
                    VexStreamQueueEffect.SeekNext,
                VexStreamQueueControl.Shuffle to
                    VexStreamQueueEffect.ShuffleFuture,
                VexStreamQueueControl.Clear to
                    VexStreamQueueEffect.ClearQueue,
            )

        cases.forEach { (
            control,
            effect,
        ) ->
            val family =
                VexStreamQueueFamily(
                    initialState =
                        state,
                )
            val accepted =
                requireNotNull(
                    family.activate(
                        control,
                    ),
                )
            val begin =
                requireNotNull(
                    accepted.realizationBegin,
                )

            assertSame(
                effect,
                begin.effect,
            )
            assertSame(
                VexExecution.Approach.Queued,
                begin.approach,
            )
            assertEquals(
                state,
                family.currentState,
            )
        }
    }

    @Test
    fun mixedQueueControlsShareOneOrderedRelationship() {
        val family =
            VexStreamQueueFamily(
                initialState =
                    VexStreamQueueState.NonEmpty(
                        previousAvailable =
                            true,
                        nextAvailable =
                            true,
                        shuffleAvailable =
                            true,
                        clearAvailable =
                            true,
                    ),
            )

        val previous =
            requireNotNull(
                family.activate(
                    VexStreamQueueControl.Previous,
                ),
            )
        val shuffle =
            requireNotNull(
                family.activate(
                    VexStreamQueueControl.Shuffle,
                ),
            )

        assertEquals(
            previous.relationshipId,
            shuffle.relationshipId,
        )
        assertSame(
            VexExecution.Approach.Queued,
            requireNotNull(
                previous.realizationBegin,
            ).approach,
        )
        assertSame(
            VexExecution.Approach.Queued,
            requireNotNull(
                shuffle.realizationBegin,
            ).approach,
        )
    }

    @Test
    fun commandAvailabilityProjectsIndependently() {
        val family =
            VexStreamQueueFamily(
                initialState =
                    VexStreamQueueState.NonEmpty(
                        previousAvailable =
                            false,
                        nextAvailable =
                            true,
                        shuffleAvailable =
                            false,
                        clearAvailable =
                            true,
                    ),
            )
        val projection =
            family.currentQueueProjection

        assertFalse(
            projection.previousAvailable,
        )
        assertTrue(
            projection.nextAvailable,
        )
        assertFalse(
            projection.shuffleAvailable,
        )
        assertTrue(
            projection.clearAvailable,
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
            order.take(
                2,
            ),
        )
        assertEquals(
            setOf(
                2,
                3,
                4,
            ),
            order
                .drop(
                    2,
                )
                .toSet(),
        )
        assertNotEquals(
            listOf(
                2,
                3,
                4,
            ),
            order.drop(
                2,
            ),
        )
    }
}

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY][PROOF]
