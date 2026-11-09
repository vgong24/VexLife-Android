package com.vextreme.vexlife.android.vexstream.repeat

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
        Boolean =
        when (
            control
        ) {
            VexStreamRepeatControl.Toggle ->
                toggleAvailable
        }
}

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
