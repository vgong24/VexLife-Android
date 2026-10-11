package com.vextreme.vexlife.android.testing.journey

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
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
        assumeTrue(
            "filtered journey case: ${case.caseRef}",
            case.matchesInstrumentationFilter(),
        )
        VexTestEngine(compose).run(case)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun journeys(): List<Array<Any>> =
            VexJourneyCatalog.cases.map { case ->
                arrayOf<Any>(case)
            }
    }
}


private fun VexJourneyCase.matchesInstrumentationFilter(): Boolean {
    val arguments = InstrumentationRegistry.getArguments()

    val exactCase = arguments
        .getString("vexJourneyCase")
        ?.takeIf { it.isNotBlank() }
    if (exactCase != null && caseRef != exactCase) {
        return false
    }

    val shard = arguments
        .getString("vexJourneyShard")
        ?.takeIf { it.isNotBlank() }
    if (shard != null && shardId != shard) {
        return false
    }

    val environment = arguments
        .getString("vexJourneyEnvironment")
        ?.takeIf { it.isNotBlank() }
    if (environment != null && this.environment.id != environment) {
        return false
    }

    return true
}
