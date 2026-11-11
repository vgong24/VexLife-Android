package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
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
            family.currentProjection
        assertFalse(
            projection.displayState.previousAvailable,
        )
        assertFalse(
            projection.displayState.nextAvailable,
        )
        assertFalse(
            projection.displayState.shuffleAvailable,
        )
        assertFalse(
            projection.displayState.clearAvailable,
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

        val previousBegin =
            requireNotNull(
                previous.realizationBegin,
            )
        val shuffleBegin =
            requireNotNull(
                shuffle.realizationBegin,
            )

        assertSame(
            VexExecution.Approach.Queued,
            previousBegin.approach,
        )
        assertSame(
            VexExecution.Approach.Queued,
            shuffleBegin.approach,
        )

        val executionRoot =
            VexExecutionRoot()

        try {
            val outOfOrderFailure =
                runCatching {
                    shuffle.launch(
                        executionRoot =
                            executionRoot,
                        method =
                            VexStreamQueueFamily.Act.Method(
                                connection =
                                    AcceptingRequestConnection,
                                effect =
                                    shuffleBegin.effect,
                            ),
                    )
                }
                    .exceptionOrNull()

            assertTrue(
                outOfOrderFailure is
                    IllegalStateException,
            )
        } finally {
            executionRoot.forceStopAll(
                "mixed Queue relationship proof complete",
            )
        }
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
            family.currentProjection

        assertFalse(
            projection.displayState.previousAvailable,
        )
        assertTrue(
            projection.displayState.nextAvailable,
        )
        assertFalse(
            projection.displayState.shuffleAvailable,
        )
        assertTrue(
            projection.displayState.clearAvailable,
        )
    }

    private object AcceptingRequestConnection :
        VexStreamPlayerPartner.Connection.Request {
        override val id =
            VexStreamPlayerPartner.Connection.Id(
                value =
                    "queue-family-test",
                occurrence =
                    1L,
            )

        override suspend fun play():
            Boolean =
            true

        override suspend fun pause():
            Boolean =
            true

        override suspend fun restartCurrent():
            Boolean =
            true

        override suspend fun previous():
            Boolean =
            true

        override suspend fun next():
            Boolean =
            true

        override suspend fun shuffleFuture():
            Boolean =
            true

        override suspend fun clearQueue():
            Boolean =
            true

        override suspend fun setRepeatEnabled(
            enabled:
                Boolean,
        ):
            Boolean =
            true
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
