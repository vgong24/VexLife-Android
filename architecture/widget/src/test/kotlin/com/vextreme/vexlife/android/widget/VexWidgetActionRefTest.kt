package com.vextreme.vexlife.android.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class VexWidgetActionRefTest {
    @Test
    fun semanticActionRefRoundTripsThroughGlanceParameters() {
        val ref =
            VexWidgetActionRef(
                "vexstream.widget.play-pause",
            )
        val parameters =
            vexWidgetActionParameters(
                ref,
            )

        assertEquals(
            ref.value,
            parameters[
                VexWidgetActionRefKey
            ],
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun actionRefRejectsPresentationCopy() {
        VexWidgetActionRef(
            "Play Pause",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun actionRefRejectsPathLikeIdentity() {
        VexWidgetActionRef(
            "../play",
        )
    }
}

// [VXG RealForever]
