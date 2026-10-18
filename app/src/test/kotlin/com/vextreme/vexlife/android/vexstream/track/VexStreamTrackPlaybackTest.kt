package com.vextreme.vexlife.android.vexstream.track

import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamTrackPlaybackTest {
    @Test
    fun playTargetFollowsResolvedContextInsteadOfRenderedIndex() {
        val alpha = track("track.alpha")
        val beta = track("track.beta")
        val gamma = track("track.gamma")
        val request =
            VexStreamTrackPlayRequest(
                selected = beta,
                contextTracks =
                    listOf(
                        alpha,
                        beta,
                        gamma,
                    ),
            )

        assertEquals(
            0,
            vexStreamTrackPlayTargetIndex(
                request = request,
                resolvedMediaIds =
                    listOf(
                        "track.beta",
                        "track.gamma",
                    ),
            ),
        )
        assertNull(
            vexStreamTrackPlayTargetIndex(
                request = request,
                resolvedMediaIds =
                    listOf(
                        "track.alpha",
                        "track.gamma",
                    ),
            ),
        )
    }

    @Test
    fun sourceAvailabilityUsesProjectedTrackTruthWithoutByteVerification() {
        assertTrue(
            vexStreamTrackSourceProjectedAvailable(
                track(
                    "track.hub",
                ),
            ),
        )
        assertFalse(
            vexStreamTrackSourceProjectedAvailable(
                track(
                    ref = "track.unavailable",
                    hubAvailable = false,
                ),
            ),
        )
    }

    @Test
    fun playAdmissionRequiresEveryProviderCommandActuallyUsed() {
        assertTrue(
            vexStreamTrackPlayCommandsAvailable(
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = false,
                setShuffleModeAvailable = false,
            ),
        )
        assertFalse(
            vexStreamTrackPlayCommandsAvailable(
                changeMediaItemsAvailable = false,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = false,
                setShuffleModeAvailable = true,
            ),
        )
        assertFalse(
            vexStreamTrackPlayCommandsAvailable(
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = true,
                setShuffleModeAvailable = false,
            ),
        )
        assertTrue(
            vexStreamTrackPlayCommandsAvailable(
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
                shuffleEnabled = true,
                setShuffleModeAvailable = true,
            ),
        )
    }

    @Test
    fun playNextStartsPlaybackOnlyWhenEmptyQueueCommandsAreAvailable() {
        assertEquals(
            VexStreamTrackQueueTarget.StartPlayback,
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.Next,
                mediaItemCount = 0,
                currentMediaItemIndex = 0,
                timelineAvailable = true,
                setMediaItemAvailable = true,
                changeMediaItemsAvailable = false,
                prepareAvailable = true,
                playAvailable = true,
            ),
        )
        assertNull(
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.Next,
                mediaItemCount = 0,
                currentMediaItemIndex = 0,
                timelineAvailable = true,
                setMediaItemAvailable = true,
                changeMediaItemsAvailable = true,
                prepareAvailable = false,
                playAvailable = true,
            ),
        )
    }

    @Test
    fun playNextNeedsTimelineAndInsertsAfterCurrentItem() {
        assertEquals(
            VexStreamTrackQueueTarget.Insert(
                index = 2,
            ),
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.Next,
                mediaItemCount = 4,
                currentMediaItemIndex = 1,
                timelineAvailable = true,
                setMediaItemAvailable = false,
                changeMediaItemsAvailable = true,
                prepareAvailable = false,
                playAvailable = false,
            ),
        )
        assertNull(
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.Next,
                mediaItemCount = null,
                currentMediaItemIndex = null,
                timelineAvailable = false,
                setMediaItemAvailable = true,
                changeMediaItemsAvailable = true,
                prepareAvailable = true,
                playAvailable = true,
            ),
        )
    }

    @Test
    fun addToQueueNeedsOnlyAppendAuthorityAfterSourceResolution() {
        assertEquals(
            VexStreamTrackQueueTarget.Append,
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.End,
                mediaItemCount = null,
                currentMediaItemIndex = null,
                timelineAvailable = false,
                setMediaItemAvailable = false,
                changeMediaItemsAvailable = true,
                prepareAvailable = false,
                playAvailable = false,
            ),
        )
        assertNull(
            vexStreamTrackQueueTarget(
                placement =
                    VexStreamTrackQueuePlacement.End,
                mediaItemCount = null,
                currentMediaItemIndex = null,
                timelineAvailable = false,
                setMediaItemAvailable = true,
                changeMediaItemsAvailable = false,
                prepareAvailable = true,
                playAvailable = true,
            ),
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
