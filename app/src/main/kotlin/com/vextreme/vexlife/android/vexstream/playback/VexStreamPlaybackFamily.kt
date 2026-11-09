@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore

/**
 * Process-shareable semantic owner for VexStream Playback.
 *
 * UI, Android Service, Widget-facing projection, and app-scope convergence may all receive this same
 * live Family instance when their rightful lifetime is the Android process. The Family owns Playback
 * State / Control / Effect meaning; Media3 remains provider reality and Android Service remains the
 * sustained playback realization.
 */
internal class VexStreamPlaybackFamily(
    initialState:
        VexStreamPlaybackState =
        VexStreamPlaybackState.NoMedia,
) :
    VexFamily<
        VexStreamPlaybackFamily,
        VexStreamPlaybackState,
        VexStreamPlaybackControl,
        VexStreamPlaybackEffect
    >(
        initialState =
            initialState,
    ) {
    /*
     * Playback-specific currentness fence, following the accepted ThemeModeFamily pattern.
     *
     * Provider reconciliation depends on exact CURRENT semantic intent while a Play/Pause Control
     * may be admitted concurrently. Keep that feature-specific race inside the Family that owns the
     * meaning rather than wrapping Family in another admission/coordinator object.
     */
    private val providerCurrentnessLock =
        Any()

    internal fun activatePlayback(
        control:
            VexStreamPlaybackControl,
    ):
        VexFamily.Act<
            VexStreamPlaybackFamily,
            VexStreamPlaybackState,
            VexStreamPlaybackEffect
        >? =
        synchronized(
            providerCurrentnessLock,
        ) {
            activate(
                control,
            )
        }

    internal fun observePlayer(
        observed:
            VexStreamPlayerPartner.Signal.Playback?,
    ):
        VexStreamPlaybackState =
        synchronized(
            providerCurrentnessLock,
        ) {
            val observedState =
                observed
                    ?.toVexStreamPlaybackState()
                    ?: VexStreamPlaybackState.NoMedia

            observe(
                reconcileVexStreamPlaybackObservation(
                    observed =
                        observedState,
                    current =
                        currentState,
                ),
            )
        }

    override fun act(
        currentState:
            VexStreamPlaybackState,
        control:
            VexStreamPlaybackControl,
    ):
        VexFamily.Act<
            VexStreamPlaybackFamily,
            VexStreamPlaybackState,
            VexStreamPlaybackEffect
        >? =
        when (
            control
        ) {
            VexStreamPlaybackControl.PlayPause -> {
                if (
                    !isPlayPauseAvailable(
                        currentState,
                    )
                ) {
                    null
                } else {
                    val effect =
                        when (
                            currentState
                        ) {
                            VexStreamPlaybackState.NoMedia ->
                                error(
                                    "NoMedia cannot admit PlayPause",
                                )

                            is VexStreamPlaybackState.Media ->
                                if (
                                    currentState
                                        .playRequested
                                ) {
                                    VexStreamPlaybackEffect
                                        .RequestPause
                                } else {
                                    VexStreamPlaybackEffect
                                        .RequestPlay
                                }

                            is VexStreamPlaybackState.Ended ->
                                VexStreamPlaybackEffect
                                    .RestartCurrent
                        }

                    admit(
                        state =
                            currentState
                                .afterIntent(
                                    effect,
                                ),
                        effect =
                            effect,
                        approach =
                            VexExecution.Approach.Latest,
                    )
                }
            }
        }

    /**
     * Feature projection derived from one exact Family State snapshot.
     *
     * Availability is resolved by the same semantic law as activation; callers do not duplicate the
     * Play/Pause conditions in UI or Service code.
     */
    fun playbackProjectionFor(
        state:
            VexStreamPlaybackState,
    ):
        VexStreamPlaybackProjection =
        vexStreamPlaybackProjectionFor(
            state,
        )

    val currentPlaybackProjection:
        VexStreamPlaybackProjection
        get() =
            playbackProjectionFor(
                currentState,
            )

    private fun isPlayPauseAvailable(
        state:
            VexStreamPlaybackState,
    ):
        Boolean =
        vexStreamPlaybackPlayPauseAvailable(
            state,
        )
}


internal fun observeVexStreamPlaybackPlayerStore(
    family:
        VexStreamPlaybackFamily,
    store:
        VexStreamPlayerStore,
    executionRoot:
        VexExecutionRoot,
) {
    VexExecution.observe(
        executionRoot =
            executionRoot,
        owner =
            family,
        source =
            store::playbackSignals,
    ) { observed ->
        family.observePlayer(
            observed,
        )
    }
}

private fun VexStreamPlayerPartner.Signal.Playback.toVexStreamPlaybackState():
    VexStreamPlaybackState {
    if (
        !hasMedia
    ) {
        return VexStreamPlaybackState.NoMedia
    }

    return when (
        phase
    ) {
        VexStreamPlayerPartner.PlaybackPhase.Idle ->
            VexStreamPlaybackState.Media(
                playRequested =
                    playRequested,
                phase =
                    VexStreamPlaybackPhase.Idle,
                playPauseAvailable =
                    playPauseAvailable,
            )

        VexStreamPlayerPartner.PlaybackPhase.Buffering ->
            VexStreamPlaybackState.Media(
                playRequested =
                    playRequested,
                phase =
                    VexStreamPlaybackPhase.Buffering,
                playPauseAvailable =
                    playPauseAvailable,
            )

        VexStreamPlayerPartner.PlaybackPhase.Ready ->
            VexStreamPlaybackState.Media(
                playRequested =
                    playRequested,
                phase =
                    VexStreamPlaybackPhase.Ready,
                playPauseAvailable =
                    playPauseAvailable,
            )

        VexStreamPlayerPartner.PlaybackPhase.Ended ->
            VexStreamPlaybackState.Ended(
                restartAvailable =
                    restartAvailable,
            )
    }
}

internal fun vexStreamPlaybackProjectionFor(
    state:
        VexStreamPlaybackState,
):
    VexStreamPlaybackProjection =
    VexStreamPlaybackProjection(
        state =
            state,
        playPauseAvailable =
            vexStreamPlaybackPlayPauseAvailable(
                state,
            ),
    )

private fun vexStreamPlaybackPlayPauseAvailable(
    state:
        VexStreamPlaybackState,
):
    Boolean =
    when (
        state
    ) {
        VexStreamPlaybackState.NoMedia ->
            false

        is VexStreamPlaybackState.Media ->
            state.playPauseAvailable

        is VexStreamPlaybackState.Ended ->
            state.restartAvailable &&
                !state.restartPending
    }

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
