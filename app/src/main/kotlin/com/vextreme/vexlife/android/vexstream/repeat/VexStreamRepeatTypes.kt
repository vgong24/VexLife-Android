package com.vextreme.vexlife.android.vexstream.repeat

internal data class VexStreamRepeatControlProjection(
    val isAvailable:
        Boolean,
)

internal data class VexStreamRepeatProjection(
    val state:
        VexStreamRepeatState,
    val toggleAvailable:
        Boolean,
) {
    operator fun get(
        control:
            VexStreamRepeatControl,
    ):
        VexStreamRepeatControlProjection =
        when (
            control
        ) {
            VexStreamRepeatControl.Toggle ->
                VexStreamRepeatControlProjection(
                    isAvailable =
                        toggleAvailable,
                )
        }
}

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
