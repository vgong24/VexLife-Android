package com.vextreme.vexlife.android.testing.journey

import android.util.Log
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * Focused physical-device regression surface for the two crawler debts exposed while validating
 * VexMaps PR #73.
 *
 * This intentionally bypasses the ScreenCrawler matrix and runs only the underlying semantic
 * journeys. The ordinary crawler remains the visual-evidence surface; this class exists so a host
 * proof can isolate journey synchronization and return compact logs instead of rerunning unrelated
 * shards.
 */
class VexFocusedJourneyRegressionInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun eventsExternalAttention() {
        runFocused(
            path = Journeys.events.externalAttention,
            label = "focused/primary.events/external-attention",
        )
    }

    @Test
    fun vexStreamConnectRefreshForget() {
        runFocused(
            path = Journeys.vexStream.connectRefreshForget,
            label = "focused/shared.vexstream/connect-refresh-forget",
        )
    }

    private fun runFocused(
        path: JourneyPath,
        label: String,
    ) {
        Log.i(
            FOCUSED_JOURNEY_LOG_TAG,
            "START $label",
        )

        VexTestEngine(
            compose = compose,
            observer =
                object : VexJourneyExecutionObserver {
                    override fun afterStep(
                        observation: VexJourneyStepObservation,
                    ) {
                        Log.i(
                            FOCUSED_JOURNEY_LOG_TAG,
                            "STEP label=$label " +
                                "index=${observation.stepIndex} " +
                                "kind=${observation.stepKind} " +
                                "screen=${observation.screenId} " +
                                "semantic=${observation.semanticRef}",
                        )
                    }

                    override fun afterJourney(
                        caseLabel: String,
                        finalScreenId: String,
                    ) {
                        Log.i(
                            FOCUSED_JOURNEY_LOG_TAG,
                            "COMPLETE label=$caseLabel " +
                                "finalScreen=$finalScreenId",
                        )
                    }
                },
        ).run(
            journey = journeyFor(path),
            environment = VexJourneyEnvironments.Baseline,
            caseLabel = label,
        )
    }
}

private const val FOCUSED_JOURNEY_LOG_TAG =
    "VexJourneyFocused"

// [VXG RealForever][JOURNEY][FOCUSED-REGRESSION]
