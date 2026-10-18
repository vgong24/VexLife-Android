package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

/**
 * Compile-time family identity for VexStream Repeat.
 *
 * Repeat is independent from Queue ordering and Playback play/pause truth. It answers whether the
 * current Media3 playback context is repeating and whether that repeat state can be toggled.
 */
sealed interface VexStreamRepeatFamily :
    VexBehaviorFamily<
        VexStreamRepeatFamily
    >

// [VXG RealForever]
