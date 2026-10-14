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
            VexStreamPlaybackArchitecture.architectureRef,
        )
        assertTrue(
            VexStreamPlaybackArchitecture.enabled,
        )

        val description =
            VexStreamPlaybackArchitecture.map.describe()

        assertTrue(
            description.contains(
                "PlaybackNotRequested",
            ),
        )
        assertTrue(
            description.contains(
                "queue-has-next",
            ),
        )
    }

    @Test
    fun emptyQueueExposesNoPlayPauseOrQueueNavigation() {
        val current =
            VexStreamPlaybackArchitecture.android.current(
                VexStreamPlaybackObservation(
                    queue = null,
                    playbackRequested = false,
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
    fun playbackNotRequestedExposesPlayAndOnlyAvailableQueueDirections() {
        val current =
            VexStreamPlaybackArchitecture.android.current(
                VexStreamPlaybackObservation(
                    queue =
                        queue(
                            hasPrevious = false,
                            hasNext = true,
                        ),
                    playbackRequested = false,
                ),
            )

        assertSame(
            VexStreamPlaybackControl.Play,
            current.availableTransition(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
        assertSame(
            VexStreamPlaybackControl.NextWhilePlaybackNotRequested,
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
    fun playbackRequestedExposesPauseAndPreservesConditionalQueueNavigation() {
        val current =
            VexStreamPlaybackArchitecture.android.current(
                VexStreamPlaybackObservation(
                    queue =
                        queue(
                            hasPrevious = true,
                            hasNext = false,
                        ),
                    playbackRequested = true,
                ),
            )

        assertSame(
            VexStreamPlaybackControl.Pause,
            current.availableTransition(
                VexStreamPlaybackControl.PlayPause,
            ),
        )
        assertSame(
            VexStreamPlaybackControl.PreviousWhilePlaybackRequested,
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

    @Test
    fun contextPlayingControlsShareOneQueueReplacementRelationshipFamily() {
        val controls =
            VexStreamPlaybackControl

        val empty =
            VexStreamPlaybackArchitecture.android.current(
                VexStreamPlaybackObservation(
                    queue = null,
                    playbackRequested = false,
                ),
            )
        val notRequested =
            VexStreamPlaybackArchitecture.android.current(
                VexStreamPlaybackObservation(
                    queue =
                        queue(
                            hasPrevious = false,
                            hasNext = true,
                        ),
                    playbackRequested = false,
                ),
            )
        val requested =
            VexStreamPlaybackArchitecture.android.current(
                VexStreamPlaybackObservation(
                    queue =
                        queue(
                            hasPrevious = true,
                            hasNext = true,
                        ),
                    playbackRequested = true,
                ),
            )

        for (control in listOf(
            controls.PlayTrack,
            controls.PlayAll,
            controls.ShuffleAll,
        )) {
            assertSame(
                controls.ReplaceQueueAndRequestPlaybackFromQueueEmpty,
                empty.availableTransition(control),
            )
            assertSame(
                controls.ReplaceQueueAndRequestPlaybackFromPlaybackNotRequested,
                notRequested.availableTransition(control),
            )
            assertSame(
                controls.ReplaceQueueAndRequestPlaybackFromPlaybackRequested,
                requested.availableTransition(control),
            )
        }
    }
}

// [VXG RealForever]
