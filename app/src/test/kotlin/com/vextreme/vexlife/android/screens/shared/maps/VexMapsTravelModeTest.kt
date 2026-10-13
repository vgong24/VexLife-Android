package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Test

class VexMapsTravelModeTest {
    @Test
    fun travelModesMapToValhallaCostingModels() {
        assertEquals(
            "auto",
            VexMapsTravelMode.DRIVE
                .valhallaCosting,
        )
        assertEquals(
            "pedestrian",
            VexMapsTravelMode.WALK
                .valhallaCosting,
        )
        assertEquals(
            "bicycle",
            VexMapsTravelMode.BICYCLE
                .valhallaCosting,
        )
    }
}

// [VXG RealForever]
