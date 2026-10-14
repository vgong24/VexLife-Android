package com.vextreme.vexlife.android.media.vexstream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class VexStreamPlaybackArchitectureTest {

    @Test
    fun emptyQueueExposesNoPlayPauseTransition() {
        val current =
            VexStreamPlaybackArchitecture.currentControl(
                VexStreamPlaybackObservation(
                    queueHasItems = false,
                    playRequested = false,
                ),
            )

        assertEquals(
            VexStreamPlaybackControlCase.QueueEmpty,
            current.case,
        )
        assertNull(
            current.availableTransition(
                VexStreamPlaybackArchitecture.PlayPauseControl,
            ),
        )
    }

    @Test
    fun readyToPlayExposesPlay() {
        val current =
            VexStreamPlaybackArchitecture.currentControl(
                VexStreamPlaybackObservation(
                    queueHasItems = true,
                    playRequested = false,
                ),
            )

        assertEquals(
            VexStreamPlaybackControlCase.ReadyToPlay,
            current.case,
        )
        assertSame(
            VexStreamPlaybackArchitecture.Play,
            current.availableTransition(
                VexStreamPlaybackArchitecture.PlayPauseControl,
            ),
        )
    }

    @Test
    fun playRequestedExposesPauseEvenBeforeAudibility() {
        val current =
            VexStreamPlaybackArchitecture.currentControl(
                VexStreamPlaybackObservation(
                    queueHasItems = true,
                    playRequested = true,
                ),
            )

        assertEquals(
            VexStreamPlaybackControlCase.PlayRequested,
            current.case,
        )
        assertSame(
            VexStreamPlaybackArchitecture.Pause,
            current.availableTransition(
                VexStreamPlaybackArchitecture.PlayPauseControl,
            ),
        )
    }

    @Test
    fun playPauseRelationshipsTellTheStoryWithoutASeparateMap() {
        assertEquals(
            VexStreamPlaybackControlCase.ReadyToPlay::class,
            VexStreamPlaybackArchitecture.Play.before,
        )
        assertEquals(
            VexStreamPlaybackControlCase.PlayRequested::class,
            VexStreamPlaybackArchitecture.Play.after,
        )
        assertEquals(
            VexStreamPlaybackControlCase.PlayRequested::class,
            VexStreamPlaybackArchitecture.Pause.before,
        )
        assertEquals(
            VexStreamPlaybackControlCase.ReadyToPlay::class,
            VexStreamPlaybackArchitecture.Pause.after,
        )
    }
}

// [VXG RealForever]
