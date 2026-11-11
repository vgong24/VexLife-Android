package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Feature-facing name for the canonical Family projection.
 *
 * This is a type alias, not another projection owner. VexFamily owns the projection relationship;
 * VexStreamPlaybackFamily owns the concrete DisplayState.
 */
internal typealias VexStreamPlaybackProjection =
    VexFamily.Projection<
        VexStreamPlaybackFamily,
        VexStreamPlaybackState,
        VexStreamPlaybackFamily.DisplayState
    >

internal val VexStreamPlaybackProjection.playPauseAvailable:
    Boolean
    get() =
        displayState
            .playPauseAvailable

// [VXG RealForever][VEXSTREAM][PLAYBACK][DIRECT-FAMILY]
