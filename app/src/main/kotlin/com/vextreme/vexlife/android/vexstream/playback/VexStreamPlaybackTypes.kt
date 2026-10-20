package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.behavior.VexPort
import com.vextreme.vexlife.android.behavior.VexPortFlow

/**
 * Feature-local names hide foundation generic weight from ordinary VexStream callers while keeping
 * the full family-bound compiler contract underneath.
 */
internal typealias VexStreamPlaybackConnection =
    VexPort<
        VexStreamPlaybackFamily,
        VexStreamPlaybackState,
        VexStreamPlaybackControl,
        VexStreamPlaybackAct
    >

internal typealias VexStreamPlaybackConnectionFlow =
    VexPortFlow<
        VexStreamPlaybackFamily,
        VexStreamPlaybackState,
        VexStreamPlaybackControl,
        VexStreamPlaybackAct
    >

internal typealias VexStreamPlaybackProjection =
    VexBehaviorProjection<
        VexStreamPlaybackFamily,
        VexStreamPlaybackState,
        VexStreamPlaybackControl,
        VexStreamPlaybackAct
    >

// [VXG RealForever]
