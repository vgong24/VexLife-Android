package com.vextreme.vexlife.android.testing.crawler

import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.testing.journey.DebugJourneys
import com.vextreme.vexlife.android.testing.journey.GalleryJourneys
import com.vextreme.vexlife.android.testing.journey.VexJourneyCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexScreenCrawlerPlannerTest {
    @Test
    fun fullBaselinePlanCreatesOneCasePerJourney() {
        val plan = VexScreenCrawlerPlanner.plan(
            VexScreenCrawlerConfig(),
        )

        assertEquals(
            VexJourneyCatalog.all.size,
            plan.size,
        )
        assertTrue(
            plan.all { case ->
                case.locale == VexCrawlerLocales.EnglishUs &&
                    case.font == VexCrawlerFonts.Normal
            },
        )
    }

    @Test
    fun oneScreenCanRunAcrossAllRequestedLanguagesAndFonts() {
        val config = vexScreenCrawlerConfig {
            screens(UserProfileScreen)
            languages(
                VexCrawlerLocales.EnglishUs,
                VexCrawlerLocales.ChineseSimplified,
            )
            fontProfiles(
                VexCrawlerFonts.Normal,
                VexCrawlerFonts.Large,
            )
            capture(VexCrawlerCapturePolicy.EVERY_STEP)
        }

        val plan = VexScreenCrawlerPlanner.plan(config)

        // Current catalog has one Profile-owned journey that touches UserProfileScreen.
        assertEquals(4, plan.size)
        assertEquals(
            setOf("en-us", "zh-cn"),
            plan.map { it.locale.id }.toSet(),
        )
        assertEquals(
            setOf("normal-1x", "large-1-45x"),
            plan.map { it.font.id }.toSet(),
        )
        assertTrue(
            plan.all { case ->
                UserProfileScreen.screenId in case.selectedScreenIds
            },
        )
    }

    @Test
    fun shardSubsetDoesNotRequireASecondJourneyRegistry() {
        val config = vexScreenCrawlerConfig {
            shards(
                DebugJourneys,
                GalleryJourneys,
            )
            languages(VexCrawlerLocales.ChineseSimplified)
            fontProfiles(VexCrawlerFonts.Large)
        }

        val plan = VexScreenCrawlerPlanner.plan(config)

        assertEquals(
            DebugJourneys.journeys.size +
                GalleryJourneys.journeys.size,
            plan.size,
        )
        assertEquals(
            setOf(
                DebugJourneys.shardId,
                GalleryJourneys.shardId,
            ),
            plan.map { it.shardId }.toSet(),
        )
    }

    @Test
    fun unknownShardFailsInsteadOfSilentlyProducingAnEmptyRun() {
        val failure = runCatching {
            VexScreenCrawlerPlanner.plan(
                VexScreenCrawlerConfig(
                    selection =
                        VexCrawlerSelection.Shards(
                            setOf("shared.not-real"),
                        ),
                ),
            )
        }.exceptionOrNull()

        requireNotNull(failure)
        assertTrue(
            failure.message.orEmpty()
                .contains("unknown crawler shard IDs"),
        )
    }
}
