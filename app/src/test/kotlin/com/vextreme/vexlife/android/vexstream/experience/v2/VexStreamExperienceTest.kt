package com.vextreme.vexlife.android.vexstream.experience.v2

import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.VexMediaPlaybackSnapshot
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.VexMediaPlayerArchitecture
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.VexMediaQueueSnapshot
import com.vextreme.vexlife.android.vexstream.experience.v2.player.VexStreamPlayerArchitecture
import com.vextreme.vexlife.android.vexstream.experience.v2.player.playback.VexStreamPlayback
import com.vextreme.vexlife.android.vexstream.experience.v2.player.playback.VexStreamPlaybackCase
import com.vextreme.vexlife.android.vexstream.experience.v2.player.provider.VexStreamPlayerProvider
import com.vextreme.vexlife.android.vexstream.experience.v2.player.queue.VexStreamQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * VexStream Experience / Player Architecture proof independent from Media3 and Compose.
 */
class VexStreamExperienceTest {

    /** Narrow provider double implementing only the capabilities the VexStream Player requires. */
    private class FakePlayerProvider(
        /** Provider-owned Playback observation. */
        var playback:
            VexMediaPlaybackSnapshot =
            VexMediaPlaybackSnapshot(
                hasMedia = true,
                playbackRequested = false,
            ),
        /** Provider-owned Queue observation. */
        var queue:
            VexMediaQueueSnapshot =
            VexMediaQueueSnapshot(
                itemCount = 2,
                currentIndex = 0,
                hasPrevious = false,
                hasNext = true,
            ),
    ) : VexStreamPlayerProvider {

        /** Effect count proving semantic Play crossed the provider boundary once. */
        var playCalls: Int = 0

        /** Effect count proving Queue.Next is independent from Playback. */
        var nextCalls: Int = 0

        override fun observePlayback():
            VexMediaPlaybackSnapshot =
            playback

        override fun observeQueue():
            VexMediaQueueSnapshot =
            queue

        override fun play() {
            playCalls += 1
            playback =
                playback.copy(
                    playbackRequested = true,
                )
        }

        override fun pause() {
            playback =
                playback.copy(
                    playbackRequested = false,
                )
        }

        override fun next() {
            nextCalls += 1
        }

        override fun previous() = Unit

        override fun playQueueIndex(
            index: Int,
        ) = Unit
    }

    @Test
    fun experienceKeepsLibrarySourcesAndPlayerAsPeerMeanings() {
        assertSame(
            VexStreamLibrary,
            VexStreamExperience.library,
        )
        assertSame(
            VexStreamSources,
            VexStreamExperience.sources,
        )
        assertSame(
            VexStreamPlayerArchitecture,
            VexStreamExperience.player,
        )
    }

    @Test
    fun playerArchitectureUsesReusableMediaPlayerContractsWithoutNestedArchitectureNames() {
        assertTrue(
            VexStreamPlayerArchitecture is
                VexMediaPlayerArchitecture.Contract,
        )
        assertTrue(
            VexStreamPlayback is
                VexMediaPlayerArchitecture.PlaybackContract,
        )
        assertTrue(
            VexStreamQueue is
                VexMediaPlayerArchitecture.QueueContract,
        )
        assertSame(
            VexStreamPlayback,
            VexStreamPlayerArchitecture.playback,
        )
        assertSame(
            VexStreamQueue,
            VexStreamPlayerArchitecture.queue,
        )
    }

    @Test
    fun playPauseDependsOnlyOnPlaybackTruth() {
        val provider =
            FakePlayerProvider(
                playback =
                    VexMediaPlaybackSnapshot(
                        hasMedia = true,
                        playbackRequested = false,
                    ),
                queue =
                    VexMediaQueueSnapshot(
                        itemCount = 40,
                        currentIndex = 17,
                        hasPrevious = true,
                        hasNext = true,
                    ),
            )

        val before =
            VexStreamPlayback
                .android
                .current(
                    provider,
                )

        assertSame(
            VexStreamPlaybackCase
                .PlaybackNotRequested,
            before.case,
        )

        assertTrue(
            VexStreamPlayback
                .android
                .request(
                    control =
                        VexStreamPlayback
                            .controls
                            .PlayPause,
                    provider = provider,
                ),
        )
        assertEquals(
            1,
            provider.playCalls,
        )
    }

    @Test
    fun nextAvailabilityBelongsToQueueWithoutPlaybackVariants() {
        val provider =
            FakePlayerProvider(
                playback =
                    VexMediaPlaybackSnapshot(
                        hasMedia = true,
                        playbackRequested = false,
                    ),
                queue =
                    VexMediaQueueSnapshot(
                        itemCount = 3,
                        currentIndex = 1,
                        hasPrevious = true,
                        hasNext = true,
                    ),
            )

        assertTrue(
            VexStreamQueue
                .android
                .request(
                    control =
                        VexStreamQueue
                            .controls
                            .NextControl,
                    provider = provider,
                ),
        )
        assertEquals(
            1,
            provider.nextCalls,
        )

        provider.queue =
            provider.queue.copy(
                hasNext = false,
            )

        assertFalse(
            VexStreamQueue
                .android
                .request(
                    control =
                        VexStreamQueue
                            .controls
                            .NextControl,
                    provider = provider,
                ),
        )
        assertEquals(
            1,
            provider.nextCalls,
        )
    }
}

// [VXG RealForever]
