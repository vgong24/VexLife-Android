package com.vextreme.vexlife.android.vexstream.architecture.v2

import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.vexstream.architecture.v2.music.VexStreamMusicPath
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.VexStreamPlayer
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.VexStreamPlayerPlaybackSnapshot
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.VexStreamPlayerQueueSnapshot
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.playback.VexStreamPlaybackArchitecture
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.playback.VexStreamPlaybackCase
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.queue.VexStreamQueueArchitecture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * VexStream V2 architecture proof that stays independent from Media3 and Compose.
 *
 * The fake player proves semantic routing/translation only. Real Media3/device behavior remains a
 * separate proof surface through the experiment and repository host/device verification.
 */
class VexStreamArchitectureV2Test {

    /**
     * Small truthful player double.
     *
     * It owns exactly the snapshots/effects the V2 port declares so tests do not duplicate Media3 or
     * VexStream repository behavior.
     */
    private class FakePlayer(
        /** Mutable playback observation controlled by the fake player owner. */
        var playback:
            VexStreamPlayerPlaybackSnapshot =
            VexStreamPlayerPlaybackSnapshot(
                hasMedia = true,
                playbackRequested = false,
            ),
        /** Mutable queue observation controlled by the fake player owner. */
        var queue:
            VexStreamPlayerQueueSnapshot =
            VexStreamPlayerQueueSnapshot(
                itemCount = 2,
                currentIndex = 0,
                hasPrevious = false,
                hasNext = true,
            ),
    ) : VexStreamPlayer {

        /** Count lets tests prove a Player effect occurred once. */
        var playCalls: Int = 0

        /** Count lets tests prove Pause realization independently from Play. */
        var pauseCalls: Int = 0

        /** Count lets tests prove Queue.Next realization without Playback branching. */
        var nextCalls: Int = 0

        /** Count lets tests prove Queue.Previous realization without Playback branching. */
        var previousCalls: Int = 0

        /** Last selected queue index proves request data stays outside the Queue Case type. */
        var selectedIndex: Int? = null

        override fun observePlayback():
            VexStreamPlayerPlaybackSnapshot =
            playback

        override fun observeQueue():
            VexStreamPlayerQueueSnapshot =
            queue

        override fun play() {
            playCalls += 1
            playback =
                playback.copy(
                    playbackRequested = true,
                )
        }

        override fun pause() {
            pauseCalls += 1
            playback =
                playback.copy(
                    playbackRequested = false,
                )
        }

        override fun next() {
            nextCalls += 1
        }

        override fun previous() {
            previousCalls += 1
        }

        override fun playQueueIndex(
            index: Int,
        ) {
            selectedIndex = index
            playback =
                playback.copy(
                    playbackRequested = true,
                )
        }
    }

    @Test
    fun rootDirectoryWalkExposesPlayerPlaybackAndQueueAsNestedConcerns() {
        val refs =
            VexStreamArchitecture
                .playerWalk
                .map {
                    it.architectureRef
                }

        assertEquals(
            listOf(
                "vexstream",
                "vexstream.music",
                "vexstream.music.player",
                "vexstream.music.player.playback",
                "vexstream.music.player.queue",
            ),
            refs,
        )
    }

    @Test
    fun playPauseDependsOnlyOnPlaybackTruth() {
        val player =
            FakePlayer(
                playback =
                    VexStreamPlayerPlaybackSnapshot(
                        hasMedia = true,
                        playbackRequested = false,
                    ),
                queue =
                    VexStreamPlayerQueueSnapshot(
                        itemCount = 40,
                        currentIndex = 17,
                        hasPrevious = true,
                        hasNext = true,
                    ),
            )

        val before =
            VexStreamPlaybackArchitecture
                .android
                .current(
                    player,
                )

        assertSame(
            VexStreamPlaybackCase
                .PlaybackNotRequested,
            before.case,
        )

        assertTrue(
            VexStreamPlaybackArchitecture
                .android
                .request(
                    control =
                        VexStreamPlaybackArchitecture
                            .controls
                            .PlayPause,
                    player = player,
                ),
        )
        assertEquals(
            1,
            player.playCalls,
        )

        val after =
            VexStreamPlaybackArchitecture
                .android
                .current(
                    player,
                )
        assertSame(
            VexStreamPlaybackCase
                .PlaybackRequested,
            after.case,
        )
    }

    @Test
    fun nextAvailabilityBelongsToQueueWithoutPlaybackCaseVariants() {
        val player =
            FakePlayer(
                playback =
                    VexStreamPlayerPlaybackSnapshot(
                        hasMedia = true,
                        playbackRequested = false,
                    ),
                queue =
                    VexStreamPlayerQueueSnapshot(
                        itemCount = 3,
                        currentIndex = 1,
                        hasPrevious = true,
                        hasNext = true,
                    ),
            )

        assertTrue(
            VexStreamQueueArchitecture
                .android
                .request(
                    control =
                        VexStreamQueueArchitecture
                            .controls
                            .NextControl,
                    player = player,
                ),
        )
        assertEquals(
            1,
            player.nextCalls,
        )

        player.queue =
            player.queue.copy(
                hasNext = false,
            )

        assertFalse(
            VexStreamQueueArchitecture
                .android
                .request(
                    control =
                        VexStreamQueueArchitecture
                            .controls
                            .NextControl,
                    player = player,
                ),
        )
        assertEquals(
            1,
            player.nextCalls,
        )
    }

    @Test
    fun playSongPathPreservesHumanIntentAbovePlaybackAndQueue() {
        val selected =
            VexStreamTrackRef(
                "track.selected",
            )
        val path =
            VexStreamMusicPath.PlaySong(
                selectedTrackRef =
                    selected,
                contextTrackRefs =
                    listOf(
                        VexStreamTrackRef(
                            "track.before",
                        ),
                        selected,
                        VexStreamTrackRef(
                            "track.after",
                        ),
                    ),
            )

        assertEquals(
            "How do I play this song?",
            path.humanQuestion,
        )
        assertEquals(
            listOf(
                VexStreamQueueArchitecture,
                VexStreamPlaybackArchitecture,
            ),
            path.concerns,
        )
    }
}

// [VXG RealForever]
