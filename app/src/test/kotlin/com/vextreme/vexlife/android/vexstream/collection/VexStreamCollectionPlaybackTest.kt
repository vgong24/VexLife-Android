package com.vextreme.vexlife.android.vexstream.collection

import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamCollectionPlaybackTest {
    @Test
    fun projectedAvailabilityNeedsAtLeastOneCurrentlyAvailableTrack() {
        assertFalse(
            vexStreamCollectionSourceProjectedAvailable(
                emptyList(),
            ),
        )
        assertFalse(
            vexStreamCollectionSourceProjectedAvailable(
                listOf(
                    track(
                        ref = "track.offline",
                        hubAvailable = false,
                    ),
                ),
            ),
        )
        assertTrue(
            vexStreamCollectionSourceProjectedAvailable(
                listOf(
                    track(
                        ref = "track.offline",
                        hubAvailable = false,
                    ),
                    track(
                        ref = "track.hub",
                    ),
                ),
            ),
        )
    }

    @Test
    fun playbackAdmissionRequiresEveryProviderCommandActuallyUsed() {
        assertTrue(
            vexStreamCollectionPlaybackCommandsAvailable(
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = false,
                setShuffleModeAvailable = false,
            ),
        )
        assertFalse(
            vexStreamCollectionPlaybackCommandsAvailable(
                changeMediaItemsAvailable = false,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = false,
                setShuffleModeAvailable = true,
            ),
        )
        assertFalse(
            vexStreamCollectionPlaybackCommandsAvailable(
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = true,
                setShuffleModeAvailable = false,
            ),
        )
        assertTrue(
            vexStreamCollectionPlaybackCommandsAvailable(
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = true,
                setShuffleModeAvailable = true,
            ),
        )
    }

    @Test
    fun playAllPreservesResolvedCollectionOrder() {
        assertEquals(
            listOf(
                0,
                1,
                2,
                3,
            ),
            vexStreamCollectionPlaybackTargetOrder(
                itemCount = 4,
                order =
                    VexStreamCollectionPlaybackOrder.Natural,
            ),
        )
    }

    @Test
    fun shuffleAllFormsOneCompleteChangedPermutationBeforePlaybackStarts() {
        val natural =
            (0 until 5).toList()
        val shuffled =
            vexStreamCollectionPlaybackTargetOrder(
                itemCount = natural.size,
                order =
                    VexStreamCollectionPlaybackOrder.Shuffled,
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

    private fun track(
        ref: String,
        hubAvailable: Boolean = true,
    ): VexStreamTrackState =
        VexStreamTrackState(
            track =
                VexStreamTrack(
                    trackRef =
                        VexStreamTrackRef(
                            ref,
                        ),
                    mediaSha256 =
                        VexStreamMediaSha256(
                            "a".repeat(64),
                        ),
                    mediaByteLength = 1L,
                    title = ref,
                    artist = "Vex",
                ),
            hubAvailable = hubAvailable,
        )
}

// [VXG RealForever]
