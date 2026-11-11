package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexFamily

internal data class VexStreamRepeatControlProjection(
    val isAvailable:
        Boolean,
)

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

internal val VexStreamRepeatProjection.toggleAvailable:
    Boolean
    get() =
        displayState
            .toggleAvailable

internal operator fun VexStreamRepeatProjection.get(
    control:
        VexStreamRepeatControl,
):
    VexStreamRepeatControlProjection =
    VexStreamRepeatControlProjection(
        isAvailable =
            displayState
                .isAvailable(
                    control,
                ),
    )

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
