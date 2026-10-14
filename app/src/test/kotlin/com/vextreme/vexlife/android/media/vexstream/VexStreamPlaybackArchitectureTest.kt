package com.vextreme.vexlife.android.media.vexstream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackArchitectureTest {

    @Test
    fun architectureRootIsTheReadablePlaybackEntryPoint() {
        assertEquals(
            "vexstream.playback",
            VexStreamPlaybackArchitecture
                .architectureRef,
        )
        assertTrue(
            VexStreamPlaybackArchitecture.enabled,
        )
        assertSame(
            VexStreamPlaybackControl,
            VexStreamPlaybackArchitecture.controls,
        )
        assertSame(
            VexStreamPlaybackPlacement,
            VexStreamPlaybackArchitecture.placements,
        )

        val map =
            VexStreamPlaybackArchitecture.map
        assertEquals(
            listOf(
                VexStreamPlaybackCase.QueueEmpty::class,
                VexStreamPlaybackCase.ReadyToPlay::class,
                VexStreamPlaybackCase.PlayRequested::class,
            ),
            map.cases,
        )
        assertTrue(
            map.describe()
                .contains(
                    "ReadyToPlay",
                ),
        )
    }

    @Test
    fun emptyQueueExposesNoPlayPauseTransition() {
        val current =
            VexStreamPlaybackArchitecture
                .android
                .current(
                    VexStreamPlaybackObservation(
                        queueHasItems = false,
                        playRequested = false,
                    ),
                )

        assertEquals(
            VexStreamPlaybackCase.QueueEmpty,
            current.case,
        )
        assertNull(
            current.availableTransition(
                VexStreamPlaybackArchitecture
                    .controls
                    .PlayPause,
            ),
        )
    }

    @Test
    fun readyToPlayExposesPlay() {
        val current =
            VexStreamPlaybackArchitecture
                .android
                .current(
                    VexStreamPlaybackObservation(
                        queueHasItems = true,
                        playRequested = false,
                    ),
                )

        assertEquals(
            VexStreamPlaybackCase.ReadyToPlay,
            current.case,
        )
        assertSame(
            VexStreamPlaybackControl.Play,
            current.availableTransition(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
    }

    @Test
    fun playRequestedExposesPauseEvenBeforeAudibility() {
        val current =
            VexStreamPlaybackArchitecture
                .android
                .current(
                    VexStreamPlaybackObservation(
                        queueHasItems = true,
                        playRequested = true,
                    ),
                )

        assertEquals(
            VexStreamPlaybackCase.PlayRequested,
            current.case,
        )
        assertSame(
            VexStreamPlaybackControl.Pause,
            current.availableTransition(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
    }

    @Test
    fun controlDeclarationsTellTheStoryWithoutASecondStateMap() {
        assertEquals(
            VexStreamPlaybackCase.ReadyToPlay::class,
            VexStreamPlaybackControl.Play.before,
        )
        assertEquals(
            VexStreamPlaybackCase.PlayRequested::class,
            VexStreamPlaybackControl.Play.after,
        )
        assertEquals(
            VexStreamPlaybackCase.PlayRequested::class,
            VexStreamPlaybackControl.Pause.before,
        )
        assertEquals(
            VexStreamPlaybackCase.ReadyToPlay::class,
            VexStreamPlaybackControl.Pause.after,
        )
    }
}

// [VXG RealForever]
