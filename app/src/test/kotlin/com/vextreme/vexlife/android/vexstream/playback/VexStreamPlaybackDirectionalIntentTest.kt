package com.vextreme.vexlife.android.vexstream.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Directional Play / Pause intent for callers that may have formed the request against older
 * perception (Vex voice/text, future system surfaces).
 *
 * The human PlayPause toggle keeps its existing meaning; these tests prove the directional commands
 * never invert already-satisfied intent and stay inside the one Playback Latest relationship.
 */
class VexStreamPlaybackDirectionalIntentTest {
    @Test
    fun pauseWhilePlayRequestedRequestsPause() {
        val family =
            familyIn(
                media(
                    playRequested =
                        true,
                ),
            )

        val accepted =
            requireNotNull(
                family.activatePlayback(
                    VexStreamPlaybackControl.Pause,
                ),
            )
        val current =
            family.currentState as
                VexStreamPlaybackState.Media

        assertSame(
            VexStreamPlaybackEffect.RequestPause,
            requireNotNull(
                accepted.realizationBegin,
            ).effect,
        )
        assertFalse(
            current.playRequested,
        )
        assertTrue(
            current.requestPending,
        )
    }

    @Test
    fun pauseWhenAlreadyPausedAuthorsNoActAndKeepsState() {
        val initial =
            media(
                playRequested =
                    false,
            )
        val family =
            familyIn(
                initial,
            )

        assertNull(
            family.activatePlayback(
                VexStreamPlaybackControl.Pause,
            ),
        )
        assertEquals(
            initial,
            family.currentState,
        )
    }

    @Test
    fun playWhenAlreadyPlayRequestedAuthorsNoActAndKeepsState() {
        val initial =
            media(
                playRequested =
                    true,
            )
        val family =
            familyIn(
                initial,
            )

        assertNull(
            family.activatePlayback(
                VexStreamPlaybackControl.Play,
            ),
        )
        assertEquals(
            initial,
            family.currentState,
        )
    }

    @Test
    fun playWhilePausedRequestsPlay() {
        val family =
            familyIn(
                media(
                    playRequested =
                        false,
                ),
            )

        val accepted =
            requireNotNull(
                family.activatePlayback(
                    VexStreamPlaybackControl.Play,
                ),
            )

        assertSame(
            VexStreamPlaybackEffect.RequestPlay,
            requireNotNull(
                accepted.realizationBegin,
            ).effect,
        )
        assertTrue(
            (family.currentState as VexStreamPlaybackState.Media)
                .playRequested,
        )
    }

    @Test
    fun directionalIntentReadsAdmittedPendingIntentNotOnlyProviderTruth() {
        // Provider still reports paused, but a newer Play is already admitted and pending.
        val family =
            familyIn(
                media(
                    playRequested =
                        false,
                ),
            )
        requireNotNull(
            family.activatePlayback(
                VexStreamPlaybackControl.Play,
            ),
        )

        assertNull(
            "Play is already the current admitted intent",
            family.activatePlayback(
                VexStreamPlaybackControl.Play,
            ),
        )
        assertSame(
            VexStreamPlaybackEffect.RequestPause,
            requireNotNull(
                requireNotNull(
                    family.activatePlayback(
                        VexStreamPlaybackControl.Pause,
                    ),
                ).realizationBegin,
            ).effect,
        )
    }

    @Test
    fun directionalIntentRespectsProviderCapability() {
        val family =
            familyIn(
                media(
                    playRequested =
                        true,
                    playPauseAvailable =
                        false,
                ),
            )

        assertNull(
            family.activatePlayback(
                VexStreamPlaybackControl.Pause,
            ),
        )
        assertNull(
            VexStreamPlaybackFamily().activatePlayback(
                VexStreamPlaybackControl.Play,
            ),
        )
    }

    @Test
    fun endedMediaRestartsOnPlayAndTreatsPauseAsSatisfied() {
        val ended =
            VexStreamPlaybackState.Ended(
                restartAvailable =
                    true,
            )

        assertNull(
            familyIn(
                ended,
            ).activatePlayback(
                VexStreamPlaybackControl.Pause,
            ),
        )
        assertSame(
            VexStreamPlaybackEffect.RestartCurrent,
            requireNotNull(
                requireNotNull(
                    familyIn(
                        ended,
                    ).activatePlayback(
                        VexStreamPlaybackControl.Play,
                    ),
                ).realizationBegin,
            ).effect,
        )
    }

    @Test
    fun directionalAndToggleShareOneLatestRelationship() {
        val family =
            familyIn(
                media(
                    playRequested =
                        true,
                ),
            )

        // A Vex "pause" is accepted, then the human taps the toggle before realization launches.
        val vexPause =
            requireNotNull(
                family.activatePlayback(
                    VexStreamPlaybackControl.Pause,
                ),
            )
        val humanToggle =
            requireNotNull(
                family.activatePlayback(
                    VexStreamPlaybackControl.PlayPause,
                ),
            )

        // Latest supersession only happens inside one maintained relationship, so this proves the
        // directional command and the toggle share it without reaching the foundation-only id.
        assertNull(
            "Latest: the newer human intent supersedes the older Vex request",
            vexPause.realizationBegin,
        )
        assertSame(
            VexStreamPlaybackEffect.RequestPlay,
            requireNotNull(
                humanToggle.realizationBegin,
            ).effect,
        )
    }

    private fun familyIn(
        state:
            VexStreamPlaybackState,
    ):
        VexStreamPlaybackFamily =
        VexStreamPlaybackFamily(
            initialState =
                state,
        )

    private fun media(
        playRequested:
            Boolean,
        playPauseAvailable:
            Boolean =
            true,
    ):
        VexStreamPlaybackState.Media =
        VexStreamPlaybackState.Media(
            playRequested =
                playRequested,
            phase =
                VexStreamPlaybackPhase.Ready,
            playPauseAvailable =
                playPauseAvailable,
        )
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY][DIRECTIONAL-INTENT][PROOF]
