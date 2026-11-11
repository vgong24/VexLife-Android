package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackReadState
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackTelemetry
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackTrace
import com.vextreme.vexlife.android.vexstream.playback.toVexStreamPlaybackReadState
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
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
    fun playerStoreOwnsCurrentMediaAndQueueWhileTelemetryIsFreshnessGated() {
        val snapshot =
            VexStreamPlayerStore.Snapshot.Available(
                playback =
                    VexStreamPlayerPartner.Signal.Playback(
                        hasMedia = true,
                        playRequested = true,
                        phase =
                            VexStreamPlayerPartner.PlaybackPhase.Ready,
                        playPauseAvailable = true,
                        restartAvailable = true,
                    ),
                queueItems =
                    VexStreamPlayerPartner.Signal.QueueItems(
                        items =
                            listOf(
                                VexStreamPlayerPartner.Signal.QueueItems.Item(
                                    mediaId = "store.track",
                                    title = "Store title",
                                    artist = "Store artist",
                                ),
                            ),
                    ),
                queueNavigation =
                    VexStreamPlayerPartner.Signal.QueueNavigation(
                        currentIndex = 0,
                        previousAvailable = false,
                        nextAvailable = false,
                        changeMediaItemsAvailable = true,
                    ),
                repeat =
                    VexStreamPlayerPartner.Signal.Repeat(
                        enabled = false,
                        toggleAvailable = true,
                    ),
                currentMedia =
                    VexStreamPlayerPartner.Signal.CurrentMedia(
                        mediaId = "store.track",
                        title = "Store title",
                        artist = "Store artist",
                        artworkUri = "https://example.invalid/art.jpg",
                    ),
            )

        val staleTelemetry =
            snapshot.toVexStreamPlaybackReadState(
                VexStreamPlaybackTelemetry(
                    observedMediaId = "older.track",
                    observedMediaItemIndex = 0,
                    currentPositionMs = 9_000L,
                    durationMs = 10_000L,
                    bufferedPositionMs = 9_500L,
                ),
            )

        assertEquals(
            "store.track",
            staleTelemetry.currentMediaId,
        )
        assertEquals(
            "Store title",
            staleTelemetry.currentTitle,
        )
        assertEquals(
            listOf(
                "store.track",
            ),
            staleTelemetry.queueMediaIds,
        )
        assertEquals(
            0,
            staleTelemetry.currentIndex,
        )
        assertEquals(
            0L,
            staleTelemetry.currentPositionMs,
        )

        val currentTelemetry =
            snapshot.toVexStreamPlaybackReadState(
                VexStreamPlaybackTelemetry(
                    observedMediaId = "store.track",
                    observedMediaItemIndex = 0,
                    currentPositionMs = 9_000L,
                    durationMs = 10_000L,
                    bufferedPositionMs = 9_500L,
                ),
            )

        assertEquals(
            9_000L,
            currentTelemetry.currentPositionMs,
        )
        assertEquals(
            10_000L,
            currentTelemetry.durationMs,
        )
        assertEquals(
            9_500L,
            currentTelemetry.bufferedPositionMs,
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
