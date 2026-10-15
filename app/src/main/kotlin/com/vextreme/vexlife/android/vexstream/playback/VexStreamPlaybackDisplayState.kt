package com.vextreme.vexlife.android.vexstream.playback

/**
 * View-facing classification of the already-derived Playback Projection.
 *
 * This is not another truth owner or Behavior-family member. It gives the UI one deterministic,
 * sealed answer for how the arrived semantic Projection should be treated.
 *
 * Active means "the Playback display is currently active", not "media is currently playing".
 */
internal sealed interface VexStreamPlaybackDisplayState {
    data object Unknown :
        VexStreamPlaybackDisplayState

    data class Inactive(
        val projection:
            VexStreamPlaybackProjection,
    ) :
        VexStreamPlaybackDisplayState

    data class Transitioning(
        val projection:
            VexStreamPlaybackProjection,
    ) :
        VexStreamPlaybackDisplayState

    data class Active(
        val projection:
            VexStreamPlaybackProjection,
    ) :
        VexStreamPlaybackDisplayState
}

/**
 * Classify one arrived semantic Projection for view treatment.
 *
 * The classification is derived only from truth already present in the Projection:
 *
 * null projection -> Unknown
 * NoMedia -> Inactive
 * admitted pending intent -> Transitioning
 * otherwise -> Active
 */
internal fun VexStreamPlaybackProjection?
    .toVexStreamPlaybackDisplayState():
    VexStreamPlaybackDisplayState =
    when (
        val current =
            this
    ) {
        null ->
            VexStreamPlaybackDisplayState
                .Unknown

        else ->
            when (
                val state =
                    current.state
            ) {
                VexStreamPlaybackState.NoMedia ->
                    VexStreamPlaybackDisplayState
                        .Inactive(
                            projection =
                                current,
                        )

                is VexStreamPlaybackState.Media ->
                    if (
                        state.requestPending
                    ) {
                        VexStreamPlaybackDisplayState
                            .Transitioning(
                                projection =
                                    current,
                            )
                    } else {
                        VexStreamPlaybackDisplayState
                            .Active(
                                projection =
                                    current,
                            )
                    }

                is VexStreamPlaybackState.Ended ->
                    if (
                        state.restartPending
                    ) {
                        VexStreamPlaybackDisplayState
                            .Transitioning(
                                projection =
                                    current,
                            )
                    } else {
                        VexStreamPlaybackDisplayState
                            .Active(
                                projection =
                                    current,
                            )
                    }
            }
    }

// [VXG RealForever]
