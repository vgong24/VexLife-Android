package com.vextreme.vexlife.android.testing.crawler

import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.testing.journey.JourneyDomain
import com.vextreme.vexlife.android.testing.journey.Journeys
import com.vextreme.vexlife.android.testing.journey.VexJourneyCatalog
import com.vextreme.vexlife.android.testing.journey.journeyFor
import com.vextreme.vexlife.android.testing.journey.shardFor
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
    fun oneNamedJourneySelectsExactlyOnePath() {
        val config = vexScreenCrawlerConfig {
            journey(Journeys.userProfile.formSubmit)
        }

        val plan = VexScreenCrawlerPlanner.plan(config)

        assertEquals(1, plan.size)
        assertEquals(
            journeyFor(Journeys.userProfile.formSubmit),
            plan.single().journey,
        )
        assertEquals(
            shardFor(Journeys.userProfile).shardId,
            plan.single().shardId,
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
    fun scenarioAndEvidenceIdentityStayStableAcrossLocaleAndFontConfigurations() {
        val config = vexScreenCrawlerConfig {
            journey(Journeys.userProfile.formSubmit)
            languages(
                language("en"),
                language("zh"),
            )
            fontProfiles(
                VexCrawlerFonts.Normal,
                VexCrawlerFonts.Large,
            )
        }

        val plan = VexScreenCrawlerPlanner.plan(config)

        assertEquals(
            setOf("shared.user-profile/form-submit"),
            plan.map { case -> case.scenarioRef }.toSet(),
        )
        assertEquals(
            setOf(
                "en/normal-1x",
                "en/large-1-45x",
                "zh/normal-1x",
                "zh/large-1-45x",
            ),
            plan.map { case -> case.configurationRef }.toSet(),
        )

        val evidenceRefs =
            plan.map { case ->
                case.evidenceIdentity(
                    screenId = UserProfileScreen.screenId,
                    stepIndex = 2,
                    stepKind = "tap",
                    semanticRef = UserProfileScreen.Save.elementId,
                ).evidenceRef
            }.toSet()

        assertEquals(1, evidenceRefs.size)
        assertEquals(
            "shared.user-profile/form-submit::" +
                "${UserProfileScreen.screenId}::" +
                "step-002::tap::" +
                UserProfileScreen.Save.elementId,
            evidenceRefs.single(),
        )
    }

    @Test
    fun shardSubsetDoesNotRequireASecondJourneyRegistry() {
        val config = vexScreenCrawlerConfig {
            shards(
                Journeys.debug,
                Journeys.gallery,
            )
            languages(language("zh"))
            fontProfiles(VexCrawlerFonts.Large)
        }

        val plan = VexScreenCrawlerPlanner.plan(config)

        assertEquals(
            shardFor(Journeys.debug).journeys.size +
                shardFor(Journeys.gallery).journeys.size,
            plan.size,
        )
        assertEquals(
            setOf(
                shardFor(Journeys.debug).shardId,
                shardFor(Journeys.gallery).shardId,
            ),
            plan.map { it.shardId }.toSet(),
        )
    }

    @Test
    fun allShardsExceptAutomaticallyIncludesCanonicalShards() {
        val config = VexScreenCrawlerConfig(
            selection = VexCrawlerSelection.AllShardsExcept(
                excludedShards = setOf(Journeys.debug),
            ),
        )

        val plan = VexScreenCrawlerPlanner.plan(config)

        assertTrue(
            plan.none { case ->
                case.shardId == shardFor(Journeys.debug).shardId
            },
        )
        assertTrue(
            plan.any { case ->
                case.shardId == shardFor(Journeys.gallery).shardId
            },
        )
    }

    @Test
    fun unregisteredTypedShardFailsInsteadOfSilentlyProducingAnEmptyRun() {
        val unknownDomain =
            object : JourneyDomain() {}

        val failure = runCatching {
            VexScreenCrawlerPlanner.plan(
                VexScreenCrawlerConfig(
                    selection =
                        VexCrawlerSelection.Shards(
                            shards = setOf(unknownDomain),
                        ),
                ),
            )
        }.exceptionOrNull()

        requireNotNull(failure)
        assertTrue(
            failure.message.orEmpty()
                .contains("journey domain is not registered"),
        )
    }
}
