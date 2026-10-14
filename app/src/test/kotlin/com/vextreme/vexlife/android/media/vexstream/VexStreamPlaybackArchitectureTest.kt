package com.vextreme.vexlife.android.media.vexstream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPlaybackArchitectureTest {

    private fun queue(
        hasPrevious: Boolean,
        hasNext: Boolean,
    ) =
        VexStreamPlaybackQueuePosition(
            itemCount = 3,
            currentIndex = 1,
            hasPrevious = hasPrevious,
            hasNext = hasNext,
        )

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

        val description =
            VexStreamPlaybackArchitecture
                .map
                .describe()

        assertTrue(
            description.contains(
                "ReadyToPlay",
            ),
        )
        assertTrue(
            description.contains(
                "queue-has-next",
            ),
        )
    }

    @Test
    fun emptyQueueExposesNoPlaybackNavigationControl() {
        val current =
            VexStreamPlaybackArchitecture
                .android
                .current(
                    VexStreamPlaybackObservation(
                        queue = null,
                        playRequested = false,
                    ),
                )

        assertEquals(
            VexStreamPlaybackCase.QueueEmpty,
            current.case,
        )
        assertNull(
            current.availableTransition(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
        assertNull(
            current.availableTransition(
                VexStreamPlaybackControl.Next,
            ),
        )
    }

    @Test
    fun readyToPlayExposesPlayAndOnlyAvailableQueueDirections() {
        val current =
            VexStreamPlaybackArchitecture
                .android
                .current(
                    VexStreamPlaybackObservation(
                        queue =
                            queue(
                                hasPrevious = false,
                                hasNext = true,
                            ),
                        playRequested = false,
                    ),
                )

        assertSame(
            VexStreamPlaybackControl.Play,
            current.availableTransition(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
        assertSame(
            VexStreamPlaybackControl.NextWhileReady,
            current.availableTransition(
                VexStreamPlaybackControl.Next,
            ),
        )
        assertNull(
            current.availableTransition(
                VexStreamPlaybackControl.Previous,
            ),
        )
    }

    @Test
    fun playRequestedKeepsQueueNavigationWithinTheSameCaseType() {
        val current =
            VexStreamPlaybackArchitecture
                .android
                .current(
                    VexStreamPlaybackObservation(
                        queue =
                            queue(
                                hasPrevious = true,
                                hasNext = false,
                            ),
                        playRequested = true,
                    ),
                )

        assertSame(
            VexStreamPlaybackControl.Pause,
            current.availableTransition(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
        assertSame(
            VexStreamPlaybackControl.PreviousWhilePlayRequested,
            current.availableTransition(
                VexStreamPlaybackControl.Previous,
            ),
        )
        assertNull(
            current.availableTransition(
                VexStreamPlaybackControl.Next,
            ),
        )
    }
}

// [VXG RealForever]
