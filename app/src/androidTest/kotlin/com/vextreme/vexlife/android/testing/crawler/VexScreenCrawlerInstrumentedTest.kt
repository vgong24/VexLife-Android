package com.vextreme.vexlife.android.testing.crawler

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import android.util.Log
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.testing.journey.Journeys
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
 *    No selection line means ALL current/future canonical journey shards.
 *
 *    EXACTLY ONE PATH:
 *
 *      journey(Journeys.userProfile.formSubmit)
 *
 *    Another exact path:
 *
 *      journey(Journeys.debug.logHistoryDismiss)
 *
 *    One Screen (runs every declared journey that touches that Screen):
 *
 *      screens(UserProfileScreen)
 *
 *    A whole feature/shard subset:
 *
 *      shards(Journeys.debug, Journeys.gallery)
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
 *    The scripts collect the ScreenCrawler artifacts that AndroidX TestStorage/Gradle returned
 *    from the instrumentation run into:
 *
 *      ./vex-screen-crawler
 *
 *    This is intentionally NOT a raw adb filesystem pull. Modern Android instrumentation runs in a
 *    storage sandbox, so a path visible inside the test process may not exist in adb shell's mount
 *    namespace. PlatformTestStorage is the transfer boundary.
 *
 *    For live debugging, filter Logcat with:
 *
 *      tag:VexScreenCrawler
 *
 *    Each screenshot has live `tag:VexScreenCrawler` CAPTURE diagnostics containing its stable
 *    semantic/configuration identity, returned path, byte count and screenshot SHA-256. The
 *    delivery tree intentionally remains screenshot-only; there is no per-case JSONL manifest.
 *
 * IMPORTANT:
 *
 * - right-clicking THIS crawler class/method means "run the configured crawler".
 * - ordinary full-suite Android runs do not expand crawler screenshot work.
 * - ordinary VexJourney functional tests are separate and remain baseline-only by default.
 * - ScreenCrawler locale/font matrices are explicit evidence work, not automatic functional-test
 *   multiplication.
 * - crawler locale conditions do not claim that translations already exist or are accepted.
 * - adding a normal Screen or journey should NOT require edits to VexScreenCrawlerPlanner or
 *   VexScreenCrawlerEvidenceObserver; change those only when crawler selection/evidence mechanics
 *   themselves change.
 */
@RunWith(Parameterized::class)
class VexScreenCrawlerInstrumentedTest(
    /**
     * AUTOMATIC JUNIT INPUT — DO NOT EDIT/CONSTRUCT THIS.
     *
     * JUnit's Parameterized runner calls [plannedRuns] below. If this crawler test was explicitly
     * selected in Android Studio, the CONFIG is planned into real crawler cases. During an ordinary
     * full-suite run, one green NotSelected placeholder is injected instead so screenshot work does
     * not multiply normal functional testing.
     */
    private val plannedRun: PlannedRun,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun runConfiguredScreenCrawler() {
        when (val selected = plannedRun) {
            is PlannedRun.NotSelected -> {
                Log.i(
                    SCREEN_CRAWLER_LOG_TAG,
                    "NOT_SELECTED reason=${selected.reason}",
                )
                println(
                    "Vex ScreenCrawler not selected for this run: ${selected.reason}",
                )
                return
            }

            is PlannedRun.Case -> {
                val case = selected.value
                Log.i(
                    SCREEN_CRAWLER_LOG_TAG,
                    "RUN case=${case.caseRef}",
                )
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
                // journey(Journeys.userProfile.formSubmit)
                // journey(Journeys.debug.logHistoryDismiss)
                // journey(Journeys.debug.resetFreshHistory)

                // ALL PATHS THAT TOUCH ONE SCREEN:
                // screens(UserProfileScreen)

                // WHOLE FEATURE/SHARD PARTITIONS:
                // shards(Journeys.debug)
                // shards(Journeys.debug, Journeys.gallery)

                // EVERYTHING EXCEPT:
                // allShardsExcept(Journeys.debug)

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
            if (!isCrawlerExplicitlySelected()) {
                return listOf(
                    PlannedRun.NotSelected(
                        reason =
                            "ordinary suite run; right-click " +
                                "VexScreenCrawlerInstrumentedTest or " +
                                "runConfiguredScreenCrawler() to execute crawler evidence",
                    ),
                )
            }

            val cases = VexScreenCrawlerPlanner.plan(config)
            require(cases.isNotEmpty()) {
                "ScreenCrawler config selected zero journey cases"
            }

            return cases.map(PlannedRun::Case)
        }

        /**
         * AndroidJUnitRunner receives the standard "class" filter when Android Studio runs a
         * specific class/method. Package selection is also treated as explicit crawler intent.
         *
         * A plain connectedDebugAndroidTest/full-module run has neither crawler-specific filter, so
         * the optional evidence matrix stays out of the normal production loop.
         */
        private fun isCrawlerExplicitlySelected(): Boolean {
            val arguments = InstrumentationRegistry.getArguments()
            val className =
                VexScreenCrawlerInstrumentedTest::class.java.name
            val packageName =
                VexScreenCrawlerInstrumentedTest::class.java.packageName

            val classFilter = arguments
                .getString("class")
                .orEmpty()
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)

            if (
                classFilter.any { candidate ->
                    candidate.substringBefore('#') == className
                }
            ) {
                return true
            }

            val packageFilter = arguments
                .getString("package")
                .orEmpty()
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)

            return packageName in packageFilter
        }
    }

    sealed interface PlannedRun {
        data class NotSelected(
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


private const val SCREEN_CRAWLER_LOG_TAG = "VexScreenCrawler"
