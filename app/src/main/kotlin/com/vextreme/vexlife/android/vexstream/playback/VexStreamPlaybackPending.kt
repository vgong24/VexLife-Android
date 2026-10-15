package com.vextreme.vexlife.android.vexstream.playback

/**
 * Connection-local shock absorber between synchronous semantic intent and delayed Media3 observation.
 *
 * This is implementation state, not a fifth Behavior concept. Public semantic truth is still
 * VexStreamPlaybackState.
 */
internal data class VexStreamPlaybackPending(
    val playRequested: Boolean? = null,
    val restartPending: Boolean = false,
)

internal data class VexStreamPlaybackReconciliation(
    val state: VexStreamPlaybackState,
    val pending: VexStreamPlaybackPending,
)

/**
 * Record the newest admitted Effect before dispatching it to Media3.
 */
internal fun VexStreamPlaybackPending.after(
    effect: VexStreamPlaybackEffect,
): VexStreamPlaybackPending =
    when (effect) {
        VexStreamPlaybackEffect.RequestPlay ->
            copy(
                playRequested = true,
                restartPending = false,
            )

        VexStreamPlaybackEffect.RequestPause ->
            copy(
                playRequested = false,
                restartPending = false,
            )

        VexStreamPlaybackEffect.RestartCurrent ->
            copy(
                playRequested = true,
                restartPending = true,
            )
    }

/**
 * Synchronously reflect an admitted Effect in semantic State before provider dispatch.
 *
 * This does not claim provider convergence. The pending flags explicitly say the connection is waiting for
 * Media3 acknowledgement.
 */
internal fun VexStreamPlaybackState.afterIntent(
    effect: VexStreamPlaybackEffect,
): VexStreamPlaybackState =
    when (effect) {
        VexStreamPlaybackEffect.RequestPlay ->
            (this as? VexStreamPlaybackState.Media)
                ?.copy(
                    playRequested = true,
                    requestPending = true,
                )
                ?: error(
                    "RequestPlay requires Media state",
                )

        VexStreamPlaybackEffect.RequestPause ->
            (this as? VexStreamPlaybackState.Media)
                ?.copy(
                    playRequested = false,
                    requestPending = true,
                )
                ?: error(
                    "RequestPause requires Media state",
                )

        VexStreamPlaybackEffect.RestartCurrent ->
            (this as? VexStreamPlaybackState.Ended)
                ?.copy(
                    restartPending = true,
                )
                ?: error(
                    "RestartCurrent requires Ended state",
                )
    }

/**
 * Fold provider observation through any newer local intent.
 *
 * A stale Media3 callback cannot erase a newer admitted Control activation. Pending intent clears
 * only when Media3 acknowledges the requested value or an incompatible terminal observation
 * invalidates it.
 */
internal fun reconcileVexStreamPlaybackObservation(
    observed: VexStreamPlaybackState,
    pending: VexStreamPlaybackPending,
): VexStreamPlaybackReconciliation =
    when (observed) {
        VexStreamPlaybackState.NoMedia ->
            VexStreamPlaybackReconciliation(
                state =
                    VexStreamPlaybackState.NoMedia,
                pending =
                    VexStreamPlaybackPending(),
            )

        is VexStreamPlaybackState.Media -> {
            val expected =
                pending.playRequested

            when {
                expected == null ->
                    VexStreamPlaybackReconciliation(
                        state =
                            observed.copy(
                                requestPending = false,
                            ),
                        pending =
                            pending.copy(
                                restartPending = false,
                            ),
                    )

                observed.playRequested == expected ->
                    VexStreamPlaybackReconciliation(
                        state =
                            observed.copy(
                                requestPending = false,
                            ),
                        pending =
                            VexStreamPlaybackPending(),
                    )

                else ->
                    VexStreamPlaybackReconciliation(
                        state =
                            observed.copy(
                                playRequested = expected,
                                requestPending = true,
                            ),
                        pending =
                            pending.copy(
                                restartPending = false,
                            ),
                    )
            }
        }

        is VexStreamPlaybackState.Ended ->
            if (pending.restartPending) {
                VexStreamPlaybackReconciliation(
                    state =
                        observed.copy(
                            restartPending = true,
                        ),
                    pending = pending,
                )
            } else {
                VexStreamPlaybackReconciliation(
                    state =
                        observed.copy(
                            restartPending = false,
                        ),
                    pending =
                        VexStreamPlaybackPending(),
                )
            }
    }

// [VXG RealForever]
