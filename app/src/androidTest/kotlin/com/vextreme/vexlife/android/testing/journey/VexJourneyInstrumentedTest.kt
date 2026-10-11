package com.vextreme.vexlife.android.testing.journey

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class VexJourneyInstrumentedTest(
    private val journey: VexJourney,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun walkMatchesActualSemanticHistory() {
        VexTestEngine(compose).run(journey)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun journeys(): List<Array<Any>> =
            VexJourneyCatalog.all.map { journey ->
                arrayOf(journey)
            }
    }
}
