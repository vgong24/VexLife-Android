package com.vextreme.vexlife.android.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexWidgetSurfaceStateTest {
    private data class ExampleState(
        val title: String,
        val selected: Boolean,
    ) : VexWidgetSurfaceState

    @Test
    fun structuralStateEqualitySuppressesExactDuplicatePublication() {
        assertFalse(
            vexWidgetSurfaceStateChanged(
                current =
                    ExampleState(
                        title = "Same",
                        selected = true,
                    ),
                next =
                    ExampleState(
                        title = "Same",
                        selected = true,
                    ),
            ),
        )
    }

    @Test
    fun returningToAnEarlierValueIsStillARealStateChangeFromCurrent() {
        val a =
            ExampleState(
                title = "A",
                selected = false,
            )
        val b =
            ExampleState(
                title = "B",
                selected = false,
            )

        assertTrue(
            vexWidgetSurfaceStateChanged(
                current = a,
                next = b,
            ),
        )
        assertTrue(
            vexWidgetSurfaceStateChanged(
                current = b,
                next = a,
            ),
        )
    }

    @Test
    fun missingDurableStateAlwaysAcceptsTheFirstSurfaceSnapshot() {
        assertTrue(
            vexWidgetSurfaceStateChanged(
                current = null,
                next =
                    ExampleState(
                        title = "First",
                        selected = false,
                    ),
            ),
        )
    }
}

// [VXG RealForever]
