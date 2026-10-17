package com.vextreme.vexlife.android.vexstream.queue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexStreamQueueReorderTest {
    @Test
    fun dragGeometryRequestsOnlyOneAdjacentMovePastHalfHeight() {
        assertEquals(
            2,
            vexStreamQueueAdjacentMoveTarget(
                currentIndex = 1,
                dragOffsetPx = 51f,
                itemHeightPx = 100,
                lastIndex = 4,
            ),
        )
        assertEquals(
            0,
            vexStreamQueueAdjacentMoveTarget(
                currentIndex = 1,
                dragOffsetPx = -51f,
                itemHeightPx = 100,
                lastIndex = 4,
            ),
        )
        assertNull(
            vexStreamQueueAdjacentMoveTarget(
                currentIndex = 1,
                dragOffsetPx = 49f,
                itemHeightPx = 100,
                lastIndex = 4,
            ),
        )
    }

    @Test
    fun exactCurrentOccurrenceAdmitsAdjacentMove() {
        val request =
            VexStreamQueueReorderRequest(
                occurrence =
                    VexStreamQueueOccurrence(
                        mediaId = "track.beta",
                        occurrence = 1,
                        index = 1,
                    ),
                direction =
                    VexStreamQueueReorderDirection.Later,
            )

        assertEquals(
            2,
            vexStreamQueueReorderTargetIndex(
                request = request,
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
    fun staleOccurrenceOrUnavailableProviderRejectsMove() {
        val request =
            VexStreamQueueReorderRequest(
                occurrence =
                    VexStreamQueueOccurrence(
                        mediaId = "track.alpha",
                        occurrence = 2,
                        index = 2,
                    ),
                direction =
                    VexStreamQueueReorderDirection.Earlier,
            )

        assertNull(
            vexStreamQueueReorderTargetIndex(
                request = request,
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
            vexStreamQueueReorderTargetIndex(
                request = request,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                        "track.beta",
                        "track.alpha",
                    ),
                changeAvailable = false,
            ),
        )
    }

    @Test
    fun queueBoundsRejectAdjacentMoveBeyondCurrentQueue() {
        val first =
            VexStreamQueueReorderRequest(
                occurrence =
                    VexStreamQueueOccurrence(
                        mediaId = "track.alpha",
                        occurrence = 1,
                        index = 0,
                    ),
                direction =
                    VexStreamQueueReorderDirection.Earlier,
            )
        val last =
            VexStreamQueueReorderRequest(
                occurrence =
                    VexStreamQueueOccurrence(
                        mediaId = "track.beta",
                        occurrence = 1,
                        index = 1,
                    ),
                direction =
                    VexStreamQueueReorderDirection.Later,
            )

        assertNull(
            vexStreamQueueReorderTargetIndex(
                request = first,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                        "track.beta",
                    ),
                changeAvailable = true,
            ),
        )
        assertNull(
            vexStreamQueueReorderTargetIndex(
                request = last,
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
    fun adjacentIdenticalMediaIdsRejectAmbiguousDuplicateCrossing() {
        val request =
            VexStreamQueueReorderRequest(
                occurrence =
                    VexStreamQueueOccurrence(
                        mediaId = "track.alpha",
                        occurrence = 1,
                        index = 0,
                    ),
                direction =
                    VexStreamQueueReorderDirection.Later,
            )

        assertNull(
            vexStreamQueueReorderTargetIndex(
                request = request,
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

// [VXG RealForever]
