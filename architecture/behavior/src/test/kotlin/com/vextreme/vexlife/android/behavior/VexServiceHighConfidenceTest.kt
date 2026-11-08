package com.vextreme.vexlife.android.behavior

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexServiceHighConfidenceTest {
    private data class NavigationCurrent(
        val active:
            Boolean,
    )

    private class NavigationServiceTemplate(
        private val source:
            () -> NavigationCurrent,
    ) :
        VexService<
            NavigationServiceTemplate,
            NavigationCurrent
        > {
        override val current:
            NavigationCurrent
            get() =
                source()
    }

    private data class PlaybackCurrent(
        val playing:
            Boolean,
    )

    private class PlaybackServiceTemplate(
        private val source:
            () -> PlaybackCurrent,
    ) :
        VexService<
            PlaybackServiceTemplate,
            PlaybackCurrent
        > {
        override val current:
            PlaybackCurrent
            get() =
                source()
    }

    @Test
    fun distinctServiceRootsBindDistinctExistingCurrentVocabularies() {
        var navigationTruth =
            NavigationCurrent(
                active =
                    true,
            )
        var playbackTruth =
            PlaybackCurrent(
                playing =
                    false,
            )

        val navigation =
            NavigationServiceTemplate {
                navigationTruth
            }
        val playback =
            PlaybackServiceTemplate {
                playbackTruth
            }

        assertTrue(
            navigation.current.active,
        )
        assertFalse(
            playback.current.playing,
        )

        navigationTruth =
            NavigationCurrent(
                active =
                    false,
            )
        playbackTruth =
            PlaybackCurrent(
                playing =
                    true,
            )

        assertFalse(
            navigation.current.active,
        )
        assertTrue(
            playback.current.playing,
        )
    }
}

// [VXG RealForever][VEX-SERVICE][STANDARD-TEMPLATE]
