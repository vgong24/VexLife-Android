package com.vextreme.vexlife.android.testing.journey

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class VexJourneyInstrumentedTest(
    private val case: VexJourneyCase,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun walkMatchesActualSemanticHistory() {
        VexTestEngine(compose).run(case)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun journeys(): List<Array<Any>> =
            selectedJourneyCases().map { case ->
                arrayOf<Any>(case)
            }
    }
}


private fun selectedJourneyCases(): List<VexJourneyCase> {
    val arguments = InstrumentationRegistry.getArguments()

    val matrixMode =
        when (
            arguments.getString("vexJourneyMatrix")
                ?.trim()
                ?.lowercase()
        ) {
            null, "", "baseline" ->
                VexJourneyMatrixMode.BASELINE_ONLY

            "all" ->
                VexJourneyMatrixMode.ALL_DECLARED

            else ->
                error(
                    "unknown vexJourneyMatrix; use baseline|all",
                )
        }

    return VexJourneyCaseSelector.select(
        VexJourneyRunSelection(
            matrixMode = matrixMode,
            exactCaseRef =
                arguments.getString("vexJourneyCase")
                    ?.takeIf { it.isNotBlank() },
            shardId =
                arguments.getString("vexJourneyShard")
                    ?.takeIf { it.isNotBlank() },
            environmentId =
                arguments.getString("vexJourneyEnvironment")
                    ?.takeIf { it.isNotBlank() },
        ),
    )
}
