package com.vextreme.vexlife.android.testing.crawler

import androidx.compose.ui.test.junit4.createComposeRule
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.testing.journey.DebugJourneys
import com.vextreme.vexlife.android.testing.journey.GalleryJourneys
import com.vextreme.vexlife.android.testing.journey.UserProfileJourneys
import com.vextreme.vexlife.android.testing.journey.VexTestEngine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * SCREENCRAWLER — NORMAL HUMAN ENTRY POINT
 *
 * You should only need this file for ordinary local ScreenCrawler work.
 *
 * 1. WHERE DO I RUN IT?
 *
 *    In Android Studio, open this class and right-click either:
 *
 *      VexScreenCrawlerInstrumentedTest
 *
 *    or:
 *
 *      runConfiguredScreenCrawler()
 *
 *    Then choose Run.
 *
 * 2. HOW DO I CHANGE WHAT IT CRAWLS?
 *
 *    Edit the CONFIG section in the companion object below.
 *
 *    First enable it:
 *
 *      enabled = true
 *
 *    No selection line means ALL current/future canonical journey shards.
 *
 *    EXACTLY ONE PATH:
 *
 *      journey(UserProfileJourneys.formSubmit)
 *
 *    Another exact path:
 *
 *      journey(DebugJourneys.logHistoryDismiss)
 *
 *    One Screen (runs every declared journey that touches that Screen):
 *
 *      screens(UserProfileScreen)
 *
 *    A whole feature/shard subset:
 *
 *      shards(DebugJourneys, GalleryJourneys)
 *
 *    English only:
 *
 *      language("en")
 *
 *    Chinese only:
 *
 *      // language("en")
 *      language("zh")
 *
 *    English + Chinese:
 *
 *      language("en")
 *      language("zh")
 *
 *    Large text:
 *
 *      VexCrawlerFonts.Large
 *
 *    The selector uses real Kotlin Screen/shard objects. Locale values use validated BCP-47-style
 *    codes because the language code itself is the external identity.
 *
 * 3. WHERE DOES IT RUN?
 *
 *    It runs on the Android target selected in Android Studio's device selector:
 *    - connected physical Android device; OR
 *    - running emulator.
 *
 *    Every generated crawler case gets a fresh Compose/VexTestEngine/repository environment.
 *
 * 4. WHERE ARE SCREENSHOTS AND HOW DO I PULL THEM?
 *
 *    From the REPOSITORY ROOT, use exactly one of these:
 *
 *    macOS / Linux:
 *
 *      bash tools/pull-vex-screen-crawler.sh
 *
 *    Windows PowerShell:
 *
 *      powershell -ExecutionPolicy Bypass -File .\\tools\\pull-vex-screen-crawler.ps1
 *
 *    The scripts:
 *    - derive the current applicationId from app/build.gradle.kts;
 *    - auto-select the only connected adb target;
 *    - verify crawler artifacts exist;
 *    - pull everything to ./vex-screen-crawler.
 *
 *    If multiple adb targets are connected, the script stops and prints the exact one-time
 *    ANDROID_SERIAL command instead of guessing.
 *
 *    The test also prints the actual device artifact directory after each crawler case.
 *
 *    Each screenshot is accompanied by JSONL evidence metadata including source/case/shard/journey,
 *    locale/font, Screen/step, device model/SDK, filename and screenshot SHA-256.
 *
 * IMPORTANT:
 *
 * - enabled=false is a GREEN NO-OP. It must never block ordinary Android development.
 * - ordinary VexJourney functional tests are separate and remain baseline-only by default.
 * - ScreenCrawler locale/font matrices are explicit evidence work, not automatic functional-test
 *   multiplication.
 * - crawler locale conditions do not claim that translations already exist or are accepted.
 */
@RunWith(Parameterized::class)
class VexScreenCrawlerInstrumentedTest(
    /**
     * AUTOMATIC JUNIT INPUT — DO NOT EDIT/CONSTRUCT THIS.
     *
     * JUnit's Parameterized runner calls [plannedRuns] below. Each returned PlannedRun becomes one
     * fresh test-class instance and is injected here. Change the CONFIG below; JUnit supplies this.
     */
    private val plannedRun: PlannedRun,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun runConfiguredScreenCrawler() {
        when (val selected = plannedRun) {
            is PlannedRun.Disabled -> {
                println(
                    "Vex ScreenCrawler disabled: ${selected.reason}",
                )
                return
            }

            is PlannedRun.Case -> {
                val case = selected.value
                val evidence =
                    VexScreenCrawlerEvidenceObserver(case)

                VexTestEngine(
                    compose = compose,
                    observer = evidence,
                ).run(
                    journey = case.journey,
                    environment = case.environment,
                    caseLabel = case.caseRef,
                )
            }
        }
    }

    companion object {
        // ---------------------------------------------------------------------
        // CONFIG — NORMAL HUMAN EDIT SURFACE
        // ---------------------------------------------------------------------

        /**
         * false -> this test is an explicit green no-op.
         * true  -> plan and execute the crawler cases configured below.
         */
        const val enabled: Boolean = false

        /**
         * Evidence coordinate stored in the screenshot manifest.
         *
         * "manual" is fine for exploratory local runs.
         * Use an exact commit/build/release reference for formal evidence.
         */
        const val sourceRef: String = "manual"

        val config: VexScreenCrawlerConfig =
            vexScreenCrawlerConfig {
                /*
                 * SELECTION — uncomment AT MOST ONE line.
                 *
                 * Leave all commented:
                 *   -> ALL current/future canonical journey shards.
                 *
                 * A newly registered shard automatically becomes available because ScreenCrawler
                 * reads VexJourneyCatalog; this file is not another shard registry.
                 */

                // EXACT ONE PATH:
                // journey(UserProfileJourneys.formSubmit)
                // journey(DebugJourneys.logHistoryDismiss)
                // journey(DebugJourneys.resetFreshHistory)

                // ALL PATHS THAT TOUCH ONE SCREEN:
                // screens(UserProfileScreen)

                // WHOLE FEATURE/SHARD PARTITIONS:
                // shards(DebugJourneys)
                // shards(DebugJourneys, GalleryJourneys)

                // EVERYTHING EXCEPT:
                // allShardsExcept(DebugJourneys)

                /*
                 * LANGUAGE / LOCALE — enable/disable rows.
                 *
                 * Examples:
                 *   en      English
                 *   zh      Chinese
                 *   bg      Bulgarian
                 *   lt      Lithuanian
                 *   zh-Hans Chinese, Simplified script
                 *   zh-Hant Chinese, Traditional script
                 */
                languages(
                    language("en"),
                    // language("zh"),
                    // language("bg"),
                    // language("lt"),
                    // language("zh-Hans"),
                    // language("zh-Hant"),
                )

                /*
                 * FONT / ACCESSIBILITY CONDITIONS — enable/disable rows.
                 */
                fontProfiles(
                    VexCrawlerFonts.Normal,
                    // VexCrawlerFonts.Large,
                    // VexCrawlerFonts.ExtraLarge,
                    // VexCrawlerFonts.Small,
                )

                /*
                 * SCREENSHOT DENSITY — keep exactly ONE active.
                 */
                capture(
                    VexCrawlerCapturePolicy.SCREEN_ONCE,
                    // VexCrawlerCapturePolicy.EVERY_STEP,
                    // VexCrawlerCapturePolicy.FINAL_ONLY,
                )

                sourceRef(sourceRef)
            }

        /**
         * JUnit plumbing:
         *
         * CONFIG -> VexScreenCrawlerPlanner -> List<PlannedRun> -> constructor [plannedRun].
         *
         * Humans configure above. This function only adapts the plan to JUnit's parameter format.
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun plannedRuns(): List<Array<Any>> =
            buildPlannedRuns().map { run ->
                arrayOf<Any>(run)
            }

        private fun buildPlannedRuns(): List<PlannedRun> {
            if (!enabled) {
                return listOf(
                    PlannedRun.Disabled(
                        reason =
                            "set VexScreenCrawlerInstrumentedTest.enabled = true",
                    ),
                )
            }

            val cases = VexScreenCrawlerPlanner.plan(config)
            require(cases.isNotEmpty()) {
                "ScreenCrawler config selected zero journey cases"
            }

            return cases.map(PlannedRun::Case)
        }
    }

    sealed interface PlannedRun {
        data class Disabled(
            val reason: String,
) : PlannedRun {
            override fun toString(): String =
                "screen-crawler-disabled"
        }

        data class Case(
            val value: VexScreenCrawlerCase,
) : PlannedRun {
            override fun toString(): String =
                value.caseRef
        }
    }
}
