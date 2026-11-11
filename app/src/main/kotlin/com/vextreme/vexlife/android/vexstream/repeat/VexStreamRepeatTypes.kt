package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Feature-facing name for the canonical Family projection.
 *
 * Projection identity stays with VexFamily; Repeat contributes only its concrete DisplayState.
 */
internal typealias VexStreamRepeatProjection =
    VexFamily.Projection<
        VexStreamRepeatFamily,
        VexStreamRepeatState,
        VexStreamRepeatFamily.DisplayState
    >

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
