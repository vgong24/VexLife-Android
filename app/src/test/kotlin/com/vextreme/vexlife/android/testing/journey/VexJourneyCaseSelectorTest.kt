package com.vextreme.vexlife.android.testing.journey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexJourneyCaseSelectorTest {
    @Test
    fun baselineIsTheDefaultFunctionalSuite() {
        val selected = VexJourneyCaseSelector.select(
            VexJourneyRunSelection(),
        )

        assertEquals(
            VexJourneyCatalog.all.size,
            selected.size,
        )
        assertTrue(
            selected.all { case ->
                case.environment.id ==
                    VexJourneyEnvironments.Baseline.id
            },
        )
    }

    @Test
    fun allMatrixExpandsDeclaredEnvironmentCases() {
        val selected = VexJourneyCaseSelector.select(
            VexJourneyRunSelection(
                matrixMode =
                    VexJourneyMatrixMode.ALL_DECLARED,
            ),
        )

        assertEquals(
            VexJourneyCatalog.cases,
            selected,
        )
        assertTrue(
            selected.size >
                VexJourneyCatalog.baselineCases.size,
        )
    }

    @Test
    fun oneEnvironmentCanBeRunAsABatchWithoutAllOthers() {
        val selected = VexJourneyCaseSelector.select(
            VexJourneyRunSelection(
                environmentId =
                    VexJourneyEnvironments.LargeText.id,
            ),
        )

        assertTrue(selected.isNotEmpty())
        assertTrue(
            selected.all { case ->
                case.environment.id ==
                    VexJourneyEnvironments.LargeText.id
            },
        )
    }

    @Test
    fun exactNonBaselineCaseCanRunWithoutAllMatrixMode() {
        val nonBaseline = VexJourneyCatalog.cases
            .first { case ->
                case.environment.id !=
                    VexJourneyEnvironments.Baseline.id
            }

        val selected = VexJourneyCaseSelector.select(
            VexJourneyRunSelection(
                exactCaseRef = nonBaseline.caseRef,
            ),
        )

        assertEquals(
            listOf(nonBaseline),
            selected,
        )
    }
}
