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
    fun pendingUpdateRequestSurvivesAnExactDuplicateSurfaceState() {
        assertTrue(
            vexWidgetUpdateRequestRequired(
                stateChanged = false,
                updatePending = true,
            ),
        )
    }

    @Test
    fun exactDuplicateWithoutPendingUpdateSuppressesAnotherRequest() {
        assertFalse(
            vexWidgetUpdateRequestRequired(
                stateChanged = false,
                updatePending = false,
            ),
        )
    }

    @Test
    fun changedSurfaceStateAlwaysRequiresAnUpdateRequest() {
        assertTrue(
            vexWidgetUpdateRequestRequired(
                stateChanged = true,
                updatePending = false,
            ),
        )
    }

    @Test
    fun retryMayRequestUpdateWithoutAnotherSurfaceStateWrite() {
        val retry =
            VexWidgetPublishResult(
                instanceCount = 1,
                changedCount = 0,
                updateRequestedCount = 1,
                failedCount = 0,
            )

        assertTrue(
            retry.allUpdatesRequested,
        )
    }


    @Test
    fun updateRequestGenerationsAdvanceEvenAfterPriorDebtSettles() {
        val first =
            vexWidgetNextUpdateRequestGeneration(
                0L,
            )
        val second =
            vexWidgetNextUpdateRequestGeneration(
                first,
            )

        assertTrue(
            second !=
                first,
        )
    }

    @Test
    fun onlyTheCurrentPendingRequestGenerationMaySettleDebt() {
        assertTrue(
            vexWidgetUpdateRequestCanSettle(
                currentGeneration = 7L,
                updatePending = true,
                expectedGeneration = 7L,
            ),
        )
        assertFalse(
            vexWidgetUpdateRequestCanSettle(
                currentGeneration = 8L,
                updatePending = true,
                expectedGeneration = 7L,
            ),
        )
        assertFalse(
            vexWidgetUpdateRequestCanSettle(
                currentGeneration = 7L,
                updatePending = false,
                expectedGeneration = 7L,
            ),
        )
    }

    @Test
    fun olderSameValueCompletionCannotSettleLaterSameValueRequest() {
        val firstA =
            vexWidgetNextUpdateRequestGeneration(
                0L,
            )
        val b =
            vexWidgetNextUpdateRequestGeneration(
                firstA,
            )
        val secondA =
            vexWidgetNextUpdateRequestGeneration(
                b,
            )

        assertFalse(
            vexWidgetUpdateRequestCanSettle(
                currentGeneration = secondA,
                updatePending = true,
                expectedGeneration = firstA,
            ),
        )
        assertTrue(
            vexWidgetUpdateRequestCanSettle(
                currentGeneration = secondA,
                updatePending = true,
                expectedGeneration = secondA,
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
