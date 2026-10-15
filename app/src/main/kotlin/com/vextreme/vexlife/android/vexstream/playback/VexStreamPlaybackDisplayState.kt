package com.vextreme.vexlife.android.vexstream.playback

/**
 * View-facing classification of the already-derived Playback output.
 *
 * This is not another truth owner or Behavior-family member. It gives the UI one deterministic,
 * sealed answer for how the arrived semantic Projection + provider read data should be treated.
 *
 * Active means "the Playback display is currently active", not "media is currently playing".
 */
internal sealed interface VexStreamPlaybackDisplayState {
    val projection:
        VexStreamPlaybackProjection?

    val playback:
        VexStreamPlaybackReadState

    data class Unknown(
        override val playback:
            VexStreamPlaybackReadState =
            VexStreamPlaybackReadState.Empty,
    ) :
        VexStreamPlaybackDisplayState {
        override val projection:
            VexStreamPlaybackProjection? =
            null
    }

    data class Inactive(
        override val projection:
            VexStreamPlaybackProjection,
        override val playback:
            VexStreamPlaybackReadState,
    ) :
        VexStreamPlaybackDisplayState

    data class Transitioning(
        override val projection:
            VexStreamPlaybackProjection,
        override val playback:
            VexStreamPlaybackReadState,
    ) :
        VexStreamPlaybackDisplayState

    data class Active(
        override val projection:
            VexStreamPlaybackProjection,
        override val playback:
            VexStreamPlaybackReadState,
    ) :
        VexStreamPlaybackDisplayState
}

/**
 * Classify one arrived Playback output for view treatment.
 *
 * The classification is derived only from current truth already available to the Experience:
 *
 * null projection -> Unknown
 * NoMedia -> Inactive
 * admitted pending intent -> Transitioning
 * otherwise -> Active
 */
internal fun VexStreamPlaybackProjection?
    .toVexStreamPlaybackDisplayState(
        playback:
            VexStreamPlaybackReadState,
    ):
    VexStreamPlaybackDisplayState =
    when (
        val current =
            this
    ) {
        null ->
            VexStreamPlaybackDisplayState
                .Unknown(
                    playback =
                        playback,
                )

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
                            playback =
                                playback,
                        )

                is VexStreamPlaybackState.Media ->
                    if (
                        state.requestPending
                    ) {
                        VexStreamPlaybackDisplayState
                            .Transitioning(
                                projection =
                                    current,
                                playback =
                                    playback,
                            )
                    } else {
                        VexStreamPlaybackDisplayState
                            .Active(
                                projection =
                                    current,
                                playback =
                                    playback,
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
                                playback =
                                    playback,
                            )
                    } else {
                        VexStreamPlaybackDisplayState
                            .Active(
                                projection =
                                    current,
                                playback =
                                    playback,
                            )
                    }
            }
    }

// [VXG RealForever]
