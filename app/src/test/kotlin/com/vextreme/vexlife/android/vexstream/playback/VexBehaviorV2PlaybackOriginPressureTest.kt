package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily
import com.vextreme.vexlife.android.behavior.VexBehaviorV2Family
import com.vextreme.vexlife.android.behavior.VexFamilyLogger
import com.vextreme.vexlife.android.behavior.vexActs
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 *       reconciles with newer local intent before concrete Family admission
 *       without becoming a fake Control or generic public setter
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
    fun reconciledProviderTruthCanEnterFamilyWithoutBecomingFakeControl() {
        val root =
            VexExecutionRoot()

        try {
            val initialSemantic =
                VexStreamPlaybackState.Media(
                    playRequested =
                        true,
                    requestPending =
                        true,
                    phase =
                        VexStreamPlaybackPhase.Buffering,
                    playPauseAvailable =
                        true,
                )

            val family =
                ProviderObservationPressureFamily(
                    executionRoot =
                        root,
                    initialState =
                        ProviderObservationPressureFamily.State.Playback(
                            initialSemantic,
                        ),
                )

            val connection =
                ProviderObservationPressureConnection(
                    family =
                        family,
                    initialPending =
                        VexStreamPlaybackPending(
                            playRequested =
                                true,
                        ),
                )

            /*
             * Media3 reports the older false value. Existing Playback reconciliation preserves the
             * newer local play intent before the concrete Family admits semantic truth.
             */
            connection.providerObserved(
                VexStreamPlaybackState.Media(
                    playRequested =
                        false,
                    phase =
                        VexStreamPlaybackPhase.Buffering,
                    playPauseAvailable =
                        true,
                ),
            )

            assertEquals(
                ProviderObservationPressureFamily.State.Playback(
                    VexStreamPlaybackState.Media(
                        playRequested =
                            true,
                        requestPending =
                            true,
                        phase =
                            VexStreamPlaybackPhase.Buffering,
                        playPauseAvailable =
                            true,
                    ),
                ),
                family.current,
            )

            /*
             * When the provider finally acknowledges the requested value, the same reconciliation
             * clears pending and the Family admits the reconciled semantic State. No Control was
             * fabricated for either provider callback.
             */
            connection.providerObserved(
                VexStreamPlaybackState.Media(
                    playRequested =
                        true,
                    phase =
                        VexStreamPlaybackPhase.Ready,
                    playPauseAvailable =
                        true,
                ),
            )

            assertEquals(
                ProviderObservationPressureFamily.State.Playback(
                    VexStreamPlaybackState.Media(
                        playRequested =
                            true,
                        requestPending =
                            false,
                        phase =
                            VexStreamPlaybackPhase.Ready,
                        playPauseAvailable =
                            true,
                    ),
                ),
                family.current,
            )
        } finally {
            root.forceStopAll()
        }
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


            val providerUnavailable =
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
                                false,
                            requestedPositionMs =
                                null,
                        ),
                )

            assertFalse(
                providerUnavailable.isAvailable(
                    control,
                ),
            )
            assertNull(
                providerUnavailable.activate(
                    control,
                ),
            )
        } finally {
            root.forceStopAll()
        }
    }

    /**
     * Provider-currentness pressure fixture.
     *
     * The wrapper State satisfies V2 Family kinship while carrying the real current production-shaped
     * Playback semantic State as evidence. It does not replace VexStreamPlaybackFamily.
     */
    private class ProviderObservationPressureFamily(
        executionRoot:
            VexExecutionRoot,
        initialState:
            State,
    ) :
        VexBehaviorV2Family<
            ProviderObservationPressureFamily,
            ProviderObservationPressureFamily.State,
            ProviderObservationPressureFamily.Control
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
                ProviderObservationPressureFamily
            > {
            data class Playback(
                val value:
                    VexStreamPlaybackState,
            ) :
                State
        }

        sealed interface Control :
            VexBehaviorFamily.Control<
                ProviderObservationPressureFamily
            > {
            data object None :
                Control
        }

        override val acts =
            vexActs<
                ProviderObservationPressureFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "origin-pressure.playback.provider-observation",
            ) {
                state<State.Playback>()
            }

        /**
         * This is intentionally concrete-Family vocabulary rather than a generic public setter.
         * The Connection may call it only after provider truth has already been reconciled.
         */
        fun providerReconciled(
            state:
                VexStreamPlaybackState,
        ) {
            admit(
                State.Playback(
                    state,
                ),
            )
        }
    }

    /**
     * Keeps the existing Playback responsibility split visible:
     *
     * Media3 observation
     * -> Connection-owned pending reconciliation
     * -> reconciled semantic State
     * -> concrete Family admission
     */
    private class ProviderObservationPressureConnection(
        private val family:
            ProviderObservationPressureFamily,
        initialPending:
            VexStreamPlaybackPending,
    ) {
        private var pending =
            initialPending

        fun providerObserved(
            observed:
                VexStreamPlaybackState,
        ) {
            val reconciled =
                reconcileVexStreamPlaybackObservation(
                    observed =
                        observed,
                    pending =
                        pending,
                )

            pending =
                reconciled.pending

            family.providerReconciled(
                reconciled.state,
            )
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
