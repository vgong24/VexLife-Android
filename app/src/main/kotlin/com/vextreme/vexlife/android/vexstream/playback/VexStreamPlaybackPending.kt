package com.vextreme.vexlife.android.vexstream.playback

/**
 * Synchronously reflect one admitted Playback Effect in Family State before Media3 acknowledgement.
 *
 * Pending meaning now lives in the shared Family State itself rather than in one Connection-local
 * shadow. Any provider observer in the process can therefore reconcile against the same current
 * semantic truth.
 */
internal fun VexStreamPlaybackState.afterIntent(
    effect:
        VexStreamPlaybackEffect,
): VexStreamPlaybackState =
    when (
        effect
    ) {
        VexStreamPlaybackEffect.RequestPlay ->
            (this as?
                VexStreamPlaybackState.Media)
                ?.copy(
                    playRequested = true,
                    requestPending = true,
                )
                ?: error(
                    "RequestPlay requires Media state",
                )

        VexStreamPlaybackEffect.RequestPause ->
            (this as?
                VexStreamPlaybackState.Media)
                ?.copy(
                    playRequested = false,
                    requestPending = true,
                )
                ?: error(
                    "RequestPause requires Media state",
                )

        VexStreamPlaybackEffect.RestartCurrent ->
            (this as?
                VexStreamPlaybackState.Ended)
                ?.copy(
                    restartPending = true,
                )
                ?: error(
                    "RestartCurrent requires Ended state",
                )
    }

/**
 * Fold one Media3 observation through the exact current Playback Family State.
 *
 * A stale provider callback cannot erase a newer admitted Control. Pending intent clears only when
 * Media3 acknowledges that request or an incompatible provider state makes the request no longer
 * current.
 */
internal fun reconcileVexStreamPlaybackObservation(
    observed:
        VexStreamPlaybackState,
    current:
        VexStreamPlaybackState,
):
    VexStreamPlaybackState =
    when (
        current
    ) {
        is VexStreamPlaybackState.Media ->
            if (
                current.requestPending &&
                observed is
                    VexStreamPlaybackState.Media
            ) {
                if (
                    observed.playRequested ==
                    current.playRequested
                ) {
                    observed.copy(
                        requestPending = false,
                    )
                } else {
                    observed.copy(
                        playRequested =
                            current.playRequested,
                        requestPending = true,
                    )
                }
            } else {
                observed
                    .withoutPendingIntent()
            }

        is VexStreamPlaybackState.Ended ->
            if (
                current.restartPending &&
                observed is
                    VexStreamPlaybackState.Ended
            ) {
                observed.copy(
                    restartPending = true,
                )
            } else {
                observed
                    .withoutPendingIntent()
            }

        VexStreamPlaybackState.NoMedia ->
            observed
                .withoutPendingIntent()
    }

private fun VexStreamPlaybackState.withoutPendingIntent():
    VexStreamPlaybackState =
    when (
        this
    ) {
        VexStreamPlaybackState.NoMedia ->
            this

        is VexStreamPlaybackState.Media ->
            copy(
                requestPending = false,
            )

        is VexStreamPlaybackState.Ended ->
            copy(
                restartPending = false,
            )
    }

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
