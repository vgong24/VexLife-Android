package com.vextreme.vexlife.android.vexstream.playback

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/**
 * Compile-time family identity for the VexStream Playback behavior.
 *
 * This has no runtime instance or service role. Its only job is to make Playback State / Control /
 * Act / Effect impossible to cross-wire with another Behavior family.
 */
sealed interface VexStreamPlaybackFamily :
    VexBehaviorFamily<
        VexStreamPlaybackFamily
    >

// [VXG RealForever]
