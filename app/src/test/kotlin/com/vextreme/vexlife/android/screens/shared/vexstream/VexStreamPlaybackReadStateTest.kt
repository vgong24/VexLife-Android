package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import androidx.media3.common.Player
import com.vextreme.vexlife.android.vexstream.playback.VexStreamCurrentPlaybackObservation
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackReadState
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackTrace
import com.vextreme.vexlife.android.vexstream.playback.vexStreamPlaybackBehavior
import com.vextreme.vexlife.android.vexstream.playback.vexStreamPlaybackControls
import com.vextreme.vexlife.android.vexstream.playback.vexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueBehavior
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueControls
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackReadStateTest {

    @Test
    fun progressProjectionClampsToThePlayableTimeline() {
        assertEquals(
            0.25f,
            VexStreamPlaybackReadState(
                currentPositionMs = 30_000L,
                durationMs = 120_000L,
            ).progressFraction,
            0.0001f,
        )

        assertEquals(
            1.0f,
            VexStreamPlaybackReadState(
                currentPositionMs = 150_000L,
                durationMs = 120_000L,
            ).progressFraction,
            0.0001f,
        )

        assertEquals(
            0.0f,
            VexStreamPlaybackReadState(
                currentPositionMs = 30_000L,
                durationMs = 0L,
            ).progressFraction,
            0.0001f,
        )
    }

    @Test
    fun playbackTraceMessageKeepsProjectionBreadcrumbsCompactAndSingleLine() {
        assertEquals(
            "seq=7 stage=widget.state.persisted " +
                "mediaId=track.2 title=Little~Bit " +
                "artist=The~Main~Squeeze glanceId=AppWidgetId(4) " +
                "desired=9 rendered=8 detail=shouldRender=true~next~line",
            VexStreamPlaybackTrace.message(
                sequence = 7L,
                stage =
                    "widget.state.persisted",
                mediaId =
                    "track.2",
                title =
                    "Little Bit",
                artist =
                    "The Main Squeeze",
                glanceId =
                    "AppWidgetId(4)",
                desiredRevision =
                    9L,
                renderedRevision =
                    8L,
                detail =
                    "shouldRender=true\nnext line",
            ),
        )
    }

    @Test
    fun currentPlaybackObservationCarriesProviderReadAndExistingSemanticProjectionsTogether() {
        val readState =
            VexStreamPlaybackReadState(
                currentMediaId =
                    "track.current",
                currentTitle =
                    "Current",
                currentArtist =
                    "Artist",
                currentIndex = 0,
            )
        val observation =
            VexStreamCurrentPlaybackObservation(
                readState =
                    readState,
                playbackProjection =
                    vexStreamPlaybackBehavior()
                        .vexStreamPlaybackControls()
                        .projection(
                            vexStreamPlaybackState(
                                mediaItemCount = 1,
                                playWhenReady = false,
                                playbackState =
                                    Player.STATE_READY,
                                playPauseAvailable = true,
                                restartAvailable = true,
                            ),
                        ),
                queueProjection =
                    vexStreamQueueBehavior()
                        .vexStreamQueueControls()
                        .projection(
                            vexStreamQueueState(
                                mediaItemCount = 1,
                                previousAvailable = false,
                                nextAvailable = false,
                                shuffleAvailable = false,
                                clearAvailable = true,
                            ),
                        ),
            )

        assertSame(
            readState,
            observation.readState,
        )
        assertTrue(
            observation
                .playbackProjection[
                    com.vextreme.vexlife.android.vexstream.playback
                        .VexStreamPlaybackControl.PlayPause
                ].isAvailable,
        )
        assertEquals(
            false,
            observation
                .queueProjection[
                    VexStreamQueueControl.Next
                ].isAvailable,
        )
    }

    @Test
    fun fullPlayerSwipeMapsOnlyBroadHorizontalGesturesToQueueControls() {
        assertEquals(
            VexStreamQueueControl.Previous,
            vexStreamFullPlayerSwipeControl(
                dragDistancePx = 120f,
                thresholdPx = 72f,
            ),
        )
        assertEquals(
            VexStreamQueueControl.Next,
            vexStreamFullPlayerSwipeControl(
                dragDistancePx = -120f,
                thresholdPx = 72f,
            ),
        )
        assertEquals(
            null,
            vexStreamFullPlayerSwipeControl(
                dragDistancePx = 40f,
                thresholdPx = 72f,
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun fullPlayerSwipeRejectsNonPositiveThreshold() {
        vexStreamFullPlayerSwipeControl(
            dragDistancePx = 10f,
            thresholdPx = 0f,
        )
    }

    @Test
    fun fullPlayerControlAddressDistinguishesExpandedSurfaceFromMiniPlayer() {
        val ref =
            VexStreamPresentationRefs.fullPlayerAction(
                VexStreamMusicScreen.ShowQueue,
            )

        assertTrue(
            ref.startsWith(
                "element.vexstream-music.full-player.action.dynamic.utf8hex.",
            ),
        )
        assertNotEquals(
            VexStreamMusicScreen.ShowQueue.elementId,
            ref,
        )
    }

    @Test
    fun fullPlayerSwipeAddressKeepsQueueControlMeaningExplicit() {
        assertEquals(
            "presentation.vexstream.full-player.swipe.previous",
            VexStreamPresentationRefs.fullPlayerSwipe(
                VexStreamQueueControl.Previous,
            ),
        )
        assertEquals(
            "presentation.vexstream.full-player.swipe.next",
            VexStreamPresentationRefs.fullPlayerSwipe(
                VexStreamQueueControl.Next,
            ),
        )
    }

    @Test
    fun sharedPlaybackRefReusesAdmittedDownloadedTruthWhenIdentityStillMatches() {
        val state =
            playbackState(
                localMediaRef =
                    "content://media/external_primary/audio/media/42",
            )

        assertEquals(
            "content://media/external_primary/audio/media/42",
            vexStreamAdmittedSharedPlaybackRef(
                state,
            ),
        )
    }

    @Test
    fun sharedPlaybackRefRejectsMismatchedOrLegacyLocalIdentity() {
        val matched =
            playbackState(
                localMediaRef =
                    "content://media/external_primary/audio/media/42",
            )

        assertEquals(
            null,
            vexStreamAdmittedSharedPlaybackRef(
                matched.copy(
                    localCopy =
                        (
                            matched.localCopy as
                                VexStreamLocalCopyState.Downloaded
                        ).copy(
                            byteLength =
                                matched.track
                                    .mediaByteLength +
                                    1L,
                        ),
                ),
            ),
        )
        assertEquals(
            null,
            vexStreamAdmittedSharedPlaybackRef(
                playbackState(
                    localMediaRef =
                        "vexstream.local.sha256." +
                            "a".repeat(
                                64,
                            ),
                ),
            ),
        )
    }

    @Test
    fun playbackTimeFormatsCompactPhoneDurations() {
        assertEquals(
            "0:00",
            formatVexStreamPlaybackTime(0L),
        )
        assertEquals(
            "1:05",
            formatVexStreamPlaybackTime(65_000L),
        )
        assertEquals(
            "1:01:01",
            formatVexStreamPlaybackTime(3_661_000L),
        )
    }
    private fun playbackState(
        localMediaRef: String,
    ): VexStreamTrackState {
        val digest =
            VexStreamMediaSha256(
                "a".repeat(
                    64,
                ),
            )
        val track =
            VexStreamTrack(
                trackRef =
                    VexStreamTrackRef(
                        "track.playback-ref",
                    ),
                mediaSha256 =
                    digest,
                mediaByteLength =
                    4_096L,
                title =
                    "Playback ref",
                artist =
                    "Vex",
            )
        return VexStreamTrackState(
            track =
                track,
            hubAvailable =
                false,
            localCopy =
                VexStreamLocalCopyState.Downloaded(
                    mediaSha256 =
                        digest,
                    byteLength =
                        track.mediaByteLength,
                    localMediaRef =
                        localMediaRef,
                ),
        )
    }
}

// [VXG RealForever]
