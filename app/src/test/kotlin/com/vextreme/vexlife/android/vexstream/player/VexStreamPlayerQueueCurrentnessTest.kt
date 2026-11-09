package com.vextreme.vexlife.android.vexstream.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexStreamPlayerQueueCurrentnessTest {
    @Test
    fun duplicateMediaIdsReceiveDistinctCurrentOccurrences() {
        assertEquals(
            listOf(
                VexStreamPlayerPartner.QueueOccurrence(
                    mediaId = "track.alpha",
                    occurrence = 1,
                    index = 0,
                ),
                VexStreamPlayerPartner.QueueOccurrence(
                    mediaId = "track.beta",
                    occurrence = 1,
                    index = 1,
                ),
                VexStreamPlayerPartner.QueueOccurrence(
                    mediaId = "track.alpha",
                    occurrence = 2,
                    index = 2,
                ),
            ),
            vexStreamPlayerQueueOccurrences(
                listOf(
                    "track.alpha",
                    "track.beta",
                    "track.alpha",
                ),
            ),
        )
    }

    @Test
    fun exactCurrentOccurrenceAdmitsQueuePlay() {
        assertEquals(
            2,
            queuePlayTargetIndex(
                occurrence =
                    VexStreamPlayerPartner.QueueOccurrence(
                        mediaId = "track.alpha",
                        occurrence = 2,
                        index = 2,
                    ),
                currentMediaIds =
                    listOf(
                        "track.alpha",
                        "track.beta",
                        "track.alpha",
                    ),
                seekAvailable = true,
                playAvailable = true,
            ),
        )
    }

    @Test
    fun staleOccurrenceOrProviderCapabilityRejectsQueuePlay() {
        val occurrence =
            VexStreamPlayerPartner.QueueOccurrence(
                mediaId = "track.alpha",
                occurrence = 1,
                index = 0,
            )

        assertNull(
            queuePlayTargetIndex(
                occurrence =
                    occurrence,
                currentMediaIds =
                    listOf(
                        "track.beta",
                        "track.alpha",
                    ),
                seekAvailable = true,
                playAvailable = true,
            ),
        )
        assertNull(
            queuePlayTargetIndex(
                occurrence =
                    occurrence,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                    ),
                seekAvailable = false,
                playAvailable = true,
            ),
        )
        assertNull(
            queuePlayTargetIndex(
                occurrence =
                    occurrence,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                    ),
                seekAvailable = true,
                playAvailable = false,
            ),
        )
    }

    @Test
    fun exactCurrentOccurrenceAdmitsAdjacentMove() {
        assertEquals(
            2,
            queueMoveTargetIndex(
                occurrence =
                    VexStreamPlayerPartner.QueueOccurrence(
                        mediaId = "track.beta",
                        occurrence = 1,
                        index = 1,
                    ),
                move =
                    VexStreamPlayerPartner.QueueMove.Later,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                        "track.beta",
                        "track.gamma",
                    ),
                changeAvailable = true,
            ),
        )
    }

    @Test
    fun staleOccurrenceUnavailableProviderOrQueueBoundsRejectMove() {
        val occurrence =
            VexStreamPlayerPartner.QueueOccurrence(
                mediaId = "track.alpha",
                occurrence = 2,
                index = 2,
            )

        assertNull(
            queueMoveTargetIndex(
                occurrence =
                    occurrence,
                move =
                    VexStreamPlayerPartner.QueueMove.Earlier,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                        "track.beta",
                        "track.gamma",
                    ),
                changeAvailable = true,
            ),
        )
        assertNull(
            queueMoveTargetIndex(
                occurrence =
                    occurrence,
                move =
                    VexStreamPlayerPartner.QueueMove.Earlier,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                        "track.beta",
                        "track.alpha",
                    ),
                changeAvailable = false,
            ),
        )
        assertNull(
            queueMoveTargetIndex(
                occurrence =
                    VexStreamPlayerPartner.QueueOccurrence(
                        mediaId = "track.alpha",
                        occurrence = 1,
                        index = 0,
                    ),
                move =
                    VexStreamPlayerPartner.QueueMove.Earlier,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                        "track.beta",
                    ),
                changeAvailable = true,
            ),
        )
    }

    @Test
    fun chainedMoveWaitsForProviderToObserveExpectedOccurrencePosition() {
        val occurrence =
            VexStreamPlayerPartner.QueueOccurrence(
                mediaId = "track.beta",
                occurrence = 1,
                index = 2,
            )

        assertNull(
            queueMoveTargetIndex(
                occurrence =
                    occurrence,
                move =
                    VexStreamPlayerPartner.QueueMove.Later,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                        "track.beta",
                        "track.gamma",
                        "track.delta",
                    ),
                changeAvailable = true,
            ),
        )
        assertEquals(
            3,
            queueMoveTargetIndex(
                occurrence =
                    occurrence,
                move =
                    VexStreamPlayerPartner.QueueMove.Later,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                        "track.gamma",
                        "track.beta",
                        "track.delta",
                    ),
                changeAvailable = true,
            ),
        )
    }

    @Test
    fun adjacentIdenticalMediaIdsRejectAmbiguousDuplicateCrossing() {
        assertNull(
            queueMoveTargetIndex(
                occurrence =
                    VexStreamPlayerPartner.QueueOccurrence(
                        mediaId = "track.alpha",
                        occurrence = 1,
                        index = 0,
                    ),
                move =
                    VexStreamPlayerPartner.QueueMove.Later,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                        "track.alpha",
                        "track.beta",
                    ),
                changeAvailable = true,
            ),
        )
    }
}

// [VXG RealForever][VEXSTREAM][PLAYER-PARTNER][QUEUE-CURRENTNESS]
