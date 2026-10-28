package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily
import com.vextreme.vexlife.android.behavior.VexBehaviorV2Family
import com.vextreme.vexlife.android.behavior.VexFamilyLogger
import com.vextreme.vexlife.android.behavior.vexActs
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Third V2 Origin-Walk reference: Playback pressure, NOT a production V2 Playback template.
 *
 * The first V2 reference classes prove the locally-owned Family circuit and stale-render activation.
 * This class deliberately starts from richer Playback facts that already exist in production-shaped
 * source and asks where the current V2 runtime stops being general.
 *
 * A passing test here means the PRESSURE remains visible:
 *
 *     provider observation
 *       must reconcile with newer local intent before becoming semantic truth
 *
 *     payload request
 *       must retain exact identity/currentness at activation
 *
 *     Control availability
 *       must derive from current semantic/provider capability
 *
 *     current V2 exact-value Control matching
 *       does NOT yet author arbitrary runtime payload Controls
 *
 * Do not "fix" the final test by registering representative payload values or by turning provider
 * observation into a fake Control. The point of this class is to preserve the unresolved boundary
 * until a later Origin Walk earns one reusable relationship. Production Playback stays on its current
 * accepted architecture until this pressure can be expressed without losing provider ownership,
 * currentness, payload identity or UI availability.
 */
class VexBehaviorV2PlaybackOriginPressureTest {
    @Test
    fun providerObservationMustReconcileWithNewerIntentBeforeBecomingSemanticState() {
        val pending =
            VexStreamPlaybackPending(
                playRequested = true,
            )

        val staleProviderObservation =
            VexStreamPlaybackState.Media(
                playRequested = false,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable = true,
            )

        val reconciled =
            reconcileVexStreamPlaybackObservation(
                observed =
                    staleProviderObservation,
                pending =
                    pending,
            )

        val semanticState =
            reconciled.state as
                VexStreamPlaybackState.Media

        /*
         * Media3 owns the observed provider fact, but that observation is not sufficient by itself to
         * replace semantic current truth. The newer admitted intent remains current until the provider
         * acknowledges it or an incompatible terminal observation invalidates it.
         */
        assertEquals(
            true,
            semanticState.playRequested,
        )
        assertTrue(
            semanticState.requestPending,
        )
        assertEquals(
            true,
            reconciled.pending.playRequested,
        )
    }

    @Test
    fun payloadSeekRequiresExactCurrentMediaCoordinateAtActivation() {
        val request =
            VexStreamPlaybackSeekRequest(
                mediaId =
                    "track.alpha",
                mediaItemIndex =
                    1,
                positionMs =
                    60_000L,
            )

        assertEquals(
            60_000L,
            vexStreamPlaybackSeekTarget(
                request =
                    request,
                currentMediaId =
                    "track.alpha",
                currentMediaItemIndex =
                    1,
                seekAvailable =
                    true,
            ),
        )

        /*
         * Payload identity is semantic currentness, not UI decoration. The same track in a different
         * queue occurrence is not the same activation coordinate.
         */
        assertNull(
            vexStreamPlaybackSeekTarget(
                request =
                    request,
                currentMediaId =
                    "track.alpha",
                currentMediaItemIndex =
                    2,
                seekAvailable =
                    true,
            ),
        )

        assertNull(
            vexStreamPlaybackSeekTarget(
                request =
                    request,
                currentMediaId =
                    "track.alpha",
                currentMediaItemIndex =
                    1,
                seekAvailable =
                    false,
            ),
        )
    }

    @Test
    fun controlAvailabilityComesFromCurrentSemanticAndProviderCapability() {
        val behavior =
            vexStreamPlaybackBehavior()
        val controls =
            behavior
                .vexStreamPlaybackControls()

        val unavailable =
            VexStreamPlaybackState.Media(
                playRequested = false,
                phase =
                    VexStreamPlaybackPhase.Ready,
                playPauseAvailable = false,
            )

        val available =
            unavailable.copy(
                playPauseAvailable = true,
            )

        val unavailableProjection =
            controls.projection(
                unavailable,
            )
        val availableProjection =
            controls.projection(
                available,
            )

        assertFalse(
            unavailableProjection[
                VexStreamPlaybackControl.PlayPause
            ].isAvailable,
        )

        assertTrue(
            availableProjection[
                VexStreamPlaybackControl.PlayPause
            ].isAvailable,
        )
        assertSame(
            VexStreamPlaybackAct.Play,
            availableProjection[
                VexStreamPlaybackControl.PlayPause
            ].act,
        )

        /*
         * A future V2 DisplayState/availability design must not duplicate this law in UI code merely
         * because activate(Control) can return null. Availability is part of current semantic meaning.
         */
    }

    @Test
    fun currentV2ExactControlMatchingKeepsArbitraryPayloadAuthoringOpen() {
        val root =
            VexExecutionRoot()

        try {
            val family =
                PayloadPressureFamily(
                    executionRoot =
                        root,
                )

            val exactlyAuthoredRequest =
                VexStreamPlaybackSeekRequest(
                    mediaId =
                        "track.alpha",
                    mediaItemIndex =
                        1,
                    positionMs =
                        30_000L,
                )

            val runtimeRequestWithDifferentPayload =
                exactlyAuthoredRequest.copy(
                    positionMs =
                        60_000L,
                )

            assertNotNull(
                family.activate(
                    PayloadPressureFamily.Control.Seek(
                        exactlyAuthoredRequest,
                    ),
                ),
            )

            /*
             * This null is the executable hold.
             *
             * V2 currently pre-authors a concrete Control value into the Act graph. Data-class
             * equality therefore matches the exact authored Seek payload, but it cannot express:
             *
             *     on<Control.Seek> { seek ->
             *         begin { Seek(seek.request) }
             *     }
             *
             * for arbitrary runtime requests. Do not interpret the sealed Control type alone as
             * proof that payload-bearing Control authoring is solved.
             */
            assertNull(
                family.activate(
                    PayloadPressureFamily.Control.Seek(
                        runtimeRequestWithDifferentPayload,
                    ),
                ),
            )
        } finally {
            root.forceStopAll()
        }
    }

    /**
     * Minimal V2 comparison fixture used only to make the exact-value payload boundary executable.
     *
     * It intentionally does not model Media3, provider observation, pending reconciliation or Seek
     * execution. Those already have rightful production-shaped owners above; copying them into this
     * fixture would make the test look more complete than the current V2 architecture really is.
     */
    private class PayloadPressureFamily(
        executionRoot:
            VexExecutionRoot,
    ) :
        VexBehaviorV2Family<
            PayloadPressureFamily,
            PayloadPressureFamily.State,
            PayloadPressureFamily.Control
        >(
            initialState =
                State.Ready(
                    mediaId =
                        "track.alpha",
                    mediaItemIndex =
                        1,
                ),
            executionRoot =
                executionRoot,
            logger =
                VexFamilyLogger.None,
        ) {
        sealed interface State :
            VexBehaviorFamily.State<
                PayloadPressureFamily
            > {
            data class Ready(
                val mediaId: String,
                val mediaItemIndex: Int,
            ) :
                State
        }

        sealed interface Control :
            VexBehaviorFamily.Control<
                PayloadPressureFamily
            > {
            data class Seek(
                val request:
                    VexStreamPlaybackSeekRequest,
            ) :
                Control
        }

        override val acts =
            vexActs<
                PayloadPressureFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "origin-pressure.playback.payload-control",
            ) {
                state<State.Ready> {
                    on(
                        Control.Seek(
                            VexStreamPlaybackSeekRequest(
                                mediaId =
                                    "track.alpha",
                                mediaItemIndex =
                                    1,
                                positionMs =
                                    30_000L,
                            ),
                        ),
                    ) {
                        stay()
                    }
                }
            }
    }
}

// [VXG RealForever]
