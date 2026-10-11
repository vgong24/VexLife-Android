package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Test

class VexMapsDurationEstimateTest {
    @Test
    fun subMinuteDurationRoundsUpToOneMinute() {
        assertEquals(
            VexMapsDurationEstimate(
                hours = 0,
                minutes = 1,
            ),
            projectVexMapsDurationEstimate(
                1,
            ),
        )
    }

    @Test
    fun minuteAndHourProjectionIsStable() {
        assertEquals(
            VexMapsDurationEstimate(
                hours = 1,
                minutes = 5,
            ),
            projectVexMapsDurationEstimate(
                3_899,
            ),
        )
    }
}

// [VXG RealForever]
