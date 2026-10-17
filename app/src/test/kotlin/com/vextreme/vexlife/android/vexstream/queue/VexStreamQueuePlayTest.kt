package com.vextreme.vexlife.android.vexstream.queue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexStreamQueuePlayTest {
    @Test
    fun duplicateMediaIdsReceiveDistinctCurrentOccurrences() {
        assertEquals(
            listOf(
                VexStreamQueueOccurrence(
                    mediaId = "track.alpha",
                    occurrence = 1,
                    index = 0,
                ),
                VexStreamQueueOccurrence(
                    mediaId = "track.beta",
                    occurrence = 1,
                    index = 1,
                ),
                VexStreamQueueOccurrence(
                    mediaId = "track.alpha",
                    occurrence = 2,
                    index = 2,
                ),
            ),
            vexStreamQueueOccurrences(
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
        val request =
            VexStreamQueuePlayRequest(
                occurrence =
                    VexStreamQueueOccurrence(
                        mediaId = "track.alpha",
                        occurrence = 2,
                        index = 2,
                    ),
            )

        assertEquals(
            2,
            vexStreamQueuePlayTargetIndex(
                request = request,
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
    fun sameMediaAndIndexWithDifferentOccurrenceIsRejected() {
        val request =
            VexStreamQueuePlayRequest(
                occurrence =
                    VexStreamQueueOccurrence(
                        mediaId = "track.alpha",
                        occurrence = 2,
                        index = 2,
                    ),
            )

        assertNull(
            vexStreamQueuePlayTargetIndex(
                request = request,
                currentMediaIds =
                    listOf(
                        "track.beta",
                        "track.beta",
                        "track.alpha",
                    ),
                seekAvailable = true,
                playAvailable = true,
            ),
        )
    }

    @Test
    fun staleIndexOrProviderCapabilityRejectsQueuePlay() {
        val request =
            VexStreamQueuePlayRequest(
                occurrence =
                    VexStreamQueueOccurrence(
                        mediaId = "track.alpha",
                        occurrence = 1,
                        index = 0,
                    ),
            )

        assertNull(
            vexStreamQueuePlayTargetIndex(
                request = request,
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
            vexStreamQueuePlayTargetIndex(
                request = request,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                    ),
                seekAvailable = false,
                playAvailable = true,
            ),
        )
        assertNull(
            vexStreamQueuePlayTargetIndex(
                request = request,
                currentMediaIds =
                    listOf(
                        "track.alpha",
                    ),
                seekAvailable = true,
                playAvailable = false,
            ),
        )
    }
}

// [VXG RealForever]
