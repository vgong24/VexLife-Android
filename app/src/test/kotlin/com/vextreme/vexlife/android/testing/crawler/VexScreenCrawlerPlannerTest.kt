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
                case.locale == VexCrawlerLocale("en") &&
                    case.font == VexCrawlerFonts.Normal
            },
        )
    }

    @Test
    fun oneScreenCanRunAcrossAllRequestedLanguagesAndFonts() {
        val config = vexScreenCrawlerConfig {
            screens(UserProfileScreen)
            languages(
                language("en"),
                language("zh"),
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
            setOf("en", "zh"),
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
            languages(language("zh"))
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
    fun allShardsExceptAutomaticallyIncludesCanonicalShards() {
        val config = VexScreenCrawlerConfig(
            selection = VexCrawlerSelection.AllShardsExcept(
                excludedShards = setOf(DebugJourneys),
            ),
        )

        val plan = VexScreenCrawlerPlanner.plan(config)

        assertTrue(
            plan.none { case ->
                case.shardId == DebugJourneys.shardId
            },
        )
        assertTrue(
            plan.any { case ->
                case.shardId == GalleryJourneys.shardId
            },
        )
    }

    @Test
    fun unregisteredTypedShardFailsInsteadOfSilentlyProducingAnEmptyRun() {
        val unknownShard =
            object : com.vextreme.vexlife.android.testing.journey.VexJourneyShard {
                override val shardId = "shared.not-real"
                override val journeys =
                    emptyList<com.vextreme.vexlife.android.testing.journey.VexJourney>()
            }

        val failure = runCatching {
            VexScreenCrawlerPlanner.plan(
                VexScreenCrawlerConfig(
                    selection =
                        VexCrawlerSelection.Shards(
                            shards = setOf(unknownShard),
                        ),
                ),
            )
        }.exceptionOrNull()

        requireNotNull(failure)
        assertTrue(
            failure.message.orEmpty()
                .contains("unregistered shard"),
        )
    }
}
