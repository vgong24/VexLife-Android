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
 *     runtime payload Control
 *       now materializes an Act from actual CURRENT State + actual Control payload
 *
 * This class still does NOT solve provider observation/reconciliation or production Seek execution.
 * Production Playback stays on its current accepted architecture until those remaining pressures can
 * be expressed without losing provider ownership, currentness, payload identity or UI availability.
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
    fun runtimePayloadControlMaterializesActFromActualCurrentStateAndRequest() {
        val root =
            VexExecutionRoot()

        try {
            val family =
                PayloadPressureFamily(
                    executionRoot =
                        root,
                    initialState =
                        PayloadPressureFamily.State.Ready(
                            mediaId =
                                "track.alpha",
                            mediaItemIndex =
                                1,
                            seekAvailable =
                                true,
                            requestedPositionMs =
                                null,
                        ),
                )

            val runtimeRequest =
                VexStreamPlaybackSeekRequest(
                    mediaId =
                        "track.alpha",
                    mediaItemIndex =
                        1,
                    positionMs =
                        60_000L,
                )
            val control =
                PayloadPressureFamily.Control.Seek(
                    runtimeRequest,
                )

            /*
             * Availability and activation resolve from the same semantic rule. The actual payload
             * value does not need to exist when the Family graph is constructed.
             */
            assertTrue(
                family.isAvailable(
                    control,
                ),
            )

            val act =
                family.activate(
                    control,
                )

            assertEquals(
                control,
                act?.control,
            )
            assertEquals(
                PayloadPressureFamily.State.Ready(
                    mediaId =
                        "track.alpha",
                    mediaItemIndex =
                        1,
                    seekAvailable =
                        true,
                    requestedPositionMs =
                        60_000L,
                ),
                family.current,
            )

            /*
             * The same Control shape with a stale occurrence does not become available merely because
             * the type matches. Runtime State + runtime Control payload participate in the Act law.
             */
            val staleOccurrence =
                PayloadPressureFamily.Control.Seek(
                    runtimeRequest.copy(
                        mediaItemIndex =
                            2,
                    ),
                )

            assertFalse(
                family.isAvailable(
                    staleOccurrence,
                ),
            )
            assertNull(
                family.activate(
                    staleOccurrence,
                ),
            )
        } finally {
            root.forceStopAll()
        }
    }

    /**
     * Minimal V2 comparison fixture proving runtime payload materialization only.
     *
     * requestedPositionMs is deliberately fixture-local pressure data. This does NOT claim production
     * Playback should store seek position as Behavior State; real Media3 Seek remains provider-owned.
     * The fixture only proves the reusable relationship:
     *
     *     actual CURRENT State value
     *     + actual incoming payload Control
     *     -> materialized Act
     *     -> value-dependent immediate consequence
     */
    private class PayloadPressureFamily(
        executionRoot:
            VexExecutionRoot,
        initialState:
            State,
    ) :
        VexBehaviorV2Family<
            PayloadPressureFamily,
            PayloadPressureFamily.State,
            PayloadPressureFamily.Control
        >(
            initialState =
                initialState,
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
                val seekAvailable: Boolean,
                val requestedPositionMs: Long?,
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
                    on<Control.Seek>(
                        conditionName =
                            "seek-matches-current-media-and-provider-is-available",
                        whenCurrent = { current, seek ->
                            current.seekAvailable &&
                                current.mediaId ==
                                    seek.request.mediaId &&
                                current.mediaItemIndex ==
                                    seek.request.mediaItemIndex
                        },
                    ) { current, seek ->
                        admit(
                            current.copy(
                                requestedPositionMs =
                                    seek.request.positionMs,
                            ),
                        )
                    }
                }
            }
    }
}


// [VXG RealForever]
