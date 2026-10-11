package com.vextreme.vexlife.android.testing.crawler

import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.testing.journey.DebugJourneys
import com.vextreme.vexlife.android.testing.journey.GalleryJourneys
import com.vextreme.vexlife.android.testing.journey.VexJourneyCatalog

/**
 * The ONE normal human-editable ScreenCrawler control surface.
 *
 * New-hire workflow:
 * 1. open this file;
 * 2. set [enabled] true;
 * 3. optionally uncomment ONE typed selection line;
 * 4. uncomment the locale/font rows you want;
 * 5. run VexScreenCrawlerInstrumentedTest.
 *
 * No Screen/shard IDs are copied here as strings. AppScreens/VexJourneyCatalog remain authoritative.
 */
object VexScreenCrawlerRunConfig {
    /**
     * ScreenCrawler does not join ordinary connectedAndroidTest work unless explicitly enabled here.
     */
    const val enabled: Boolean = false

    /**
     * Source/evidence coordinate written into screenshot manifests.
     *
     * For a formal evidence batch, replace "manual" with the exact commit/build/release coordinate.
     */
    const val sourceRef: String = "manual"

    val config: VexScreenCrawlerConfig =
        vexScreenCrawlerConfig {
            /*
             * SELECTION — choose at most one line.
             *
             * Leave all lines commented to include ALL current/future canonical shards.
             * Because the default comes from VexJourneyCatalog, a newly registered shard is
             * automatically available without adding it again here.
             */

            // screens(UserProfileScreen)
            // shards(DebugJourneys)
            // shards(DebugJourneys, GalleryJourneys)
            // allShardsExcept(DebugJourneys)

            /*
             * LANGUAGE / LOCALE CONDITIONS — BCP-47-style codes.
             *
             * These are evidence conditions, not a claim that product translations already exist.
             * Enable/disable a row. No new Kotlin property/parameter is needed for another code.
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
             * FONT / ACCESSIBILITY CONDITIONS.
             *
             * These remain test profiles until an authoritative accessibility/regulatory requirement
             * owns exact acceptance thresholds.
             */
            fontProfiles(
                VexCrawlerFonts.Normal,
                // VexCrawlerFonts.Large,
                // VexCrawlerFonts.ExtraLarge,
                // VexCrawlerFonts.Small,
            )

            /*
             * EVIDENCE DENSITY.
             */
            capture(
                VexCrawlerCapturePolicy.SCREEN_ONCE,
                // VexCrawlerCapturePolicy.EVERY_STEP,
                // VexCrawlerCapturePolicy.FINAL_ONLY,
            )

            sourceRef(sourceRef)
        }

    /**
     * Developer orientation only. Do not copy this into another list.
     */
    val availableShardIds: Set<String>
        get() = VexJourneyCatalog.shardsById.keys

    internal fun parameters(): List<VexScreenCrawlerParameter> {
        if (!enabled) {
            return listOf(
                VexScreenCrawlerParameter(
                    enabled = false,
                    case = null,
                    disabledReason =
                        "set VexScreenCrawlerRunConfig.enabled = true",
                ),
            )
        }

        val cases = VexScreenCrawlerPlanner.plan(config)
        require(cases.isNotEmpty()) {
            "ScreenCrawler config selected zero journey cases"
        }

        return cases.map { case ->
            VexScreenCrawlerParameter(
                enabled = true,
                case = case,
            )
        }
    }
}

internal data class VexScreenCrawlerParameter(
    val enabled: Boolean,
    val case: VexScreenCrawlerCase?,
    val disabledReason: String? = null,
) {
    override fun toString(): String =
        case?.caseRef ?: "screen-crawler-disabled"
}
