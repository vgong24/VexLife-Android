package com.vextreme.vexlife.android.behavior

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proof that relational source declarations preserve the behavior geometry they describe.
 */
class VexBehaviorBlueprintTest {
    private sealed interface PlaybackState {
        data object NoMedia :
            PlaybackState

        data object NotRequested :
            PlaybackState

        data object Requested :
            PlaybackState
    }

    private sealed interface PlaybackIntention {
        data object PlayPause :
            PlaybackIntention
    }

    private sealed interface PlaybackEffect {
        data object Play :
            PlaybackEffect

        data object Pause :
            PlaybackEffect
    }

    private fun playbackBlueprint() =
        vexBehaviorBlueprint<
            PlaybackState,
            PlaybackIntention,
            PlaybackEffect
        >(
            behaviorRef = "example.playback",
        ) {
            state<PlaybackState.NoMedia>()
            state<PlaybackState.NotRequested>()
            state<PlaybackState.Requested>()

            intention(
                PlaybackIntention.PlayPause,
            ) {
                cycle<
                    PlaybackState.NotRequested,
                    PlaybackState.Requested
                >(
                    forward =
                        PlaybackEffect.Play,
                    backward =
                        PlaybackEffect.Pause,
                )
            }
        }

    @Test
    fun playAndPauseAreOneVisibleAndResolvableCycle() {
        val blueprint =
            playbackBlueprint()

        val play =
            blueprint.resolve(
                current =
                    PlaybackState.NotRequested,
                intention =
                    PlaybackIntention.PlayPause,
            )

        assertSame(
            PlaybackEffect.Play,
            play?.effect,
        )
        assertEquals(
            PlaybackState.Requested::class,
            play?.destination,
        )

        val pause =
            blueprint.resolve(
                current =
                    PlaybackState.Requested,
                intention =
                    PlaybackIntention.PlayPause,
            )

        assertSame(
            PlaybackEffect.Pause,
            pause?.effect,
        )
        assertEquals(
            PlaybackState.NotRequested::class,
            pause?.destination,
        )
    }

    @Test
    fun incomingAndOutgoingNeighborhoodsAreDerivedFromTheBond() {
        val blueprint =
            playbackBlueprint()

        val requestedIncoming =
            blueprint.incoming(
                PlaybackState.Requested::class,
            )
        val requestedOutgoing =
            blueprint.outgoing(
                PlaybackState.Requested::class,
            )

        assertEquals(
            1,
            requestedIncoming.size,
        )
        assertSame(
            PlaybackEffect.Play,
            requestedIncoming.single().effect,
        )
        assertEquals(
            1,
            requestedOutgoing.size,
        )
        assertSame(
            PlaybackEffect.Pause,
            requestedOutgoing.single().effect,
        )
    }

    @Test
    fun noMediaDoesNotInventAPlayPauseRelationship() {
        assertNull(
            playbackBlueprint().resolve(
                current =
                    PlaybackState.NoMedia,
                intention =
                    PlaybackIntention.PlayPause,
            ),
        )
    }

    private sealed interface QueueState {
        data object Empty :
            QueueState

        data class Positioned(
            val hasNext: Boolean,
        ) : QueueState
    }

    private sealed interface QueueIntention {
        data object Next :
            QueueIntention
    }

    private sealed interface QueueEffect {
        data object Next :
            QueueEffect
    }

    @Test
    fun conditionalSelfRelationshipKeepsQueuePositioned() {
        val blueprint =
            vexBehaviorBlueprint<
                QueueState,
                QueueIntention,
                QueueEffect
            >(
                behaviorRef = "example.queue",
            ) {
                state<QueueState.Empty>()
                state<QueueState.Positioned>()

                intention(
                    QueueIntention.Next,
                ) {
                    stay<QueueState.Positioned>(
                        effect =
                            QueueEffect.Next,
                        conditionName =
                            "has-next",
                    ) { current ->
                        current.hasNext
                    }
                }
            }

        assertNull(
            blueprint.resolve(
                current =
                    QueueState.Positioned(
                        hasNext = false,
                    ),
                intention =
                    QueueIntention.Next,
            ),
        )

        val next =
            blueprint.resolve(
                current =
                    QueueState.Positioned(
                        hasNext = true,
                    ),
                intention =
                    QueueIntention.Next,
            )

        assertSame(
            QueueEffect.Next,
            next?.effect,
        )
        assertEquals(
            QueueState.Positioned::class,
            next?.destination,
        )
        assertTrue(
            blueprint.describe()
                .contains(
                    "when has-next",
                ),
        )
    }
}

// [VXG RealForever]
