package com.vextreme.vexlife.android.vexstream.queue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexStreamQueueReorderTest {
    @Test
    fun dragGeometryRequestsOnlyOneAdjacentMovePastHalfHeight() {
        assertEquals(
            2,
            vexStreamQueueAdjacentMoveTarget(
                currentIndex = 1,
                dragOffsetPx = 51f,
                itemHeightPx = 100,
                lastIndex = 4,
            ),
        )
        assertEquals(
            0,
            vexStreamQueueAdjacentMoveTarget(
                currentIndex = 1,
                dragOffsetPx = -51f,
                itemHeightPx = 100,
                lastIndex = 4,
            ),
        )
        assertNull(
            vexStreamQueueAdjacentMoveTarget(
                currentIndex = 1,
                dragOffsetPx = 49f,
                itemHeightPx = 100,
                lastIndex = 4,
            ),
        )
    }
}

// [VXG RealForever][VEXSTREAM][QUEUE][PRESENTATION-GEOMETRY]
