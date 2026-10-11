package com.vextreme.vexlife.android.testing.crawler

import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.testing.journey.VexJourney
import com.vextreme.vexlife.android.testing.journey.VexJourneyCatalog
import com.vextreme.vexlife.android.testing.journey.VexJourneyCoverageRecorder
import com.vextreme.vexlife.android.testing.journey.VexJourneyEnvironment
import com.vextreme.vexlife.android.testing.journey.VexJourneyShard

/**
 * Test/evidence locale condition addressed directly by a BCP-47 language/locale code.
 *
 * Examples:
 *   en
 *   zh
 *   bg
 *   lt
 *   zh-Hans
 *   pt-BR
 *
 * This is NOT the product's supported-language authority. A future localization foundation may own
 * that registry; ScreenCrawler should consume its codes rather than invent another enum/constant per
 * language.
 */
data class VexCrawlerLocale(
    val code: String,
) {
    val localeTag: String =
        code.trim().replace('_', '-')

    val id: String =
        localeTag.lowercase()

    init {
        require(localeTag.isNotBlank()) {
            "crawler locale code must not be blank"
        }
        require(
            localeTag.matches(
                Regex("[A-Za-z0-9]{1,8}(?:-[A-Za-z0-9]{1,8})*"),
            ),
        ) {
            "crawler locale must be a BCP-47-style code: $code"
        }
    }
}

/**
 * Exploratory font-scale profile. Values are test conditions, not regulatory thresholds.
 */
data class VexCrawlerFontProfile(
    val id: String,
    val scale: Float,
) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9-]*"))) {
            "crawler font id must be lowercase kebab-case: $id"
        }
        require(scale > 0f) { "crawler font scale must be positive" }
    }
}

object VexCrawlerFonts {
    val Small = VexCrawlerFontProfile("small-0-85x", 0.85f)
    val Normal = VexCrawlerFontProfile("normal-1x", 1.0f)
    val Large = VexCrawlerFontProfile("large-1-45x", 1.45f)
    val ExtraLarge = VexCrawlerFontProfile("extra-large-2x", 2.0f)
}

enum class VexCrawlerCapturePolicy {
    /** Capture the first stable appearance of each selected/visited Screen in one case. */
    SCREEN_ONCE,

    /** Capture after every journey step, including local text entry. Useful for translation review. */
    EVERY_STEP,

    /** Capture only the final stable state of the selected journey case. */
    FINAL_ONLY,
}

sealed interface VexCrawlerSelection {
    data object AllShards : VexCrawlerSelection

    /**
     * Canonical all-shards selection with optional exclusions.
     *
     * New shards are automatically included because the planner reads VexJourneyCatalog.shards.
     */
    data class AllShardsExcept(
        val excludedShardIds: Set<String> = emptySet(),
    ) : VexCrawlerSelection

    /** Explicit "only these shards" partition. */
    data class Shards(
        val shardIds: Set<String>,
    ) : VexCrawlerSelection

    /** Select every journey whose declared semantic walk touches any selected Screen. */
    data class Screens(
        val screenIds: Set<String>,
    ) : VexCrawlerSelection

    /** Stable source refs shaped as <shardId>/<journeyId>. */
    data class Journeys(
        val journeyRefs: Set<String>,
    ) : VexCrawlerSelection
}

data class VexScreenCrawlerConfig(
    val selection: VexCrawlerSelection = VexCrawlerSelection.AllShards,
    val locales: List<VexCrawlerLocale> = listOf(VexCrawlerLocale("en")),
    val fonts: List<VexCrawlerFontProfile> = listOf(VexCrawlerFonts.Normal),
    val capturePolicy: VexCrawlerCapturePolicy = VexCrawlerCapturePolicy.SCREEN_ONCE,
    val sourceRef: String = "unknown",
) {
    init {
        require(locales.isNotEmpty()) { "crawler must declare at least one locale" }
        require(fonts.isNotEmpty()) { "crawler must declare at least one font profile" }
        require(locales.map { it.id }.distinct().size == locales.size) {
            "crawler locale IDs must be unique"
        }
        require(fonts.map { it.id }.distinct().size == fonts.size) {
            "crawler font IDs must be unique"
        }
    }
}

class VexScreenCrawlerConfigBuilder {
    private var selection: VexCrawlerSelection = VexCrawlerSelection.AllShards
    private val locales = mutableListOf<VexCrawlerLocale>()
    private val fonts = mutableListOf<VexCrawlerFontProfile>()
    private var capturePolicy = VexCrawlerCapturePolicy.SCREEN_ONCE
    private var sourceRef = "unknown"

    fun allShards() {
        selection = VexCrawlerSelection.AllShards
    }

    fun allShardsExcept(vararg shards: VexJourneyShard) {
        selection = VexCrawlerSelection.AllShardsExcept(
            excludedShardIds = shards.map { it.shardId }.toSet(),
        )
    }

    fun shards(vararg shards: VexJourneyShard) {
        selection = VexCrawlerSelection.Shards(
            shards.map { it.shardId }.toSet(),
        )
    }

    fun screens(vararg screens: ScreenElement) {
        selection = VexCrawlerSelection.Screens(
            screens.map { it.screenId }.toSet(),
        )
    }

    fun journeys(vararg journeyRefs: String) {
        selection = VexCrawlerSelection.Journeys(
            journeyRefs.toSet(),
        )
    }

    fun languages(vararg codes: String) {
        locales += codes.map(::VexCrawlerLocale)
    }

    fun fontProfiles(vararg values: VexCrawlerFontProfile) {
        fonts += values
    }

    fun capture(policy: VexCrawlerCapturePolicy) {
        capturePolicy = policy
    }

    fun sourceRef(value: String) {
        sourceRef = value
    }

    fun build(): VexScreenCrawlerConfig =
        VexScreenCrawlerConfig(
            selection = selection,
            locales = locales.ifEmpty {
                listOf(VexCrawlerLocale("en"))
            },
            fonts = fonts.ifEmpty {
                listOf(VexCrawlerFonts.Normal)
            },
            capturePolicy = capturePolicy,
            sourceRef = sourceRef,
        )
}

fun vexScreenCrawlerConfig(
    block: VexScreenCrawlerConfigBuilder.() -> Unit,
): VexScreenCrawlerConfig =
    VexScreenCrawlerConfigBuilder()
        .apply(block)
        .build()

data class VexScreenCrawlerCase(
    val shardId: String,
    val journey: VexJourney,
    val locale: VexCrawlerLocale,
    val font: VexCrawlerFontProfile,
    val capturePolicy: VexCrawlerCapturePolicy,
    val selectedScreenIds: Set<String>,
    val sourceRef: String,
) {
    val environment: VexJourneyEnvironment =
        VexJourneyEnvironment(
            id = "${locale.id}-${font.id}",
            fontScale = font.scale,
            localeTag = locale.localeTag,
        )

    val caseRef: String
        get() =
            "crawler/$shardId/${journey.id}@${locale.id}+${font.id}"

    override fun toString(): String =
        "$caseRef :: ${journey.title}"
}

object VexScreenCrawlerPlanner {
    fun plan(config: VexScreenCrawlerConfig): List<VexScreenCrawlerCase> {
        val selected = selectJourneys(config.selection)
        val selectedScreenIds =
            (config.selection as? VexCrawlerSelection.Screens)
                ?.screenIds
                .orEmpty()

        return selected.flatMap { (shard, journey) ->
            config.locales.flatMap { locale ->
                config.fonts.map { font ->
                    VexScreenCrawlerCase(
                        shardId = shard.shardId,
                        journey = journey,
                        locale = locale,
                        font = font,
                        capturePolicy = config.capturePolicy,
                        selectedScreenIds = selectedScreenIds,
                        sourceRef = config.sourceRef,
                    )
                }
            }
        }
    }

    private fun selectJourneys(
        selection: VexCrawlerSelection,
    ): List<Pair<VexJourneyShard, VexJourney>> {
        val all = VexJourneyCatalog.shards.flatMap { shard ->
            shard.journeys.map { journey -> shard to journey }
        }

        return when (selection) {
            VexCrawlerSelection.AllShards ->
                all

            is VexCrawlerSelection.AllShardsExcept -> {
                val known = VexJourneyCatalog.shardsById.keys
                val unknown = selection.excludedShardIds - known
                require(unknown.isEmpty()) {
                    "unknown crawler shard IDs: ${unknown.sorted()}"
                }

                all.filterNot { (shard, _) ->
                    shard.shardId in selection.excludedShardIds
                }
            }

            is VexCrawlerSelection.Shards -> {
                val known = VexJourneyCatalog.shardsById.keys
                val unknown = selection.shardIds - known
                require(unknown.isEmpty()) {
                    "unknown crawler shard IDs: ${unknown.sorted()}"
                }
                all.filter { (shard, _) ->
                    shard.shardId in selection.shardIds
                }
            }

            is VexCrawlerSelection.Journeys -> {
                val byRef = all.associateBy { (shard, journey) ->
                    "${shard.shardId}/${journey.id}"
                }
                val unknown = selection.journeyRefs - byRef.keys
                require(unknown.isEmpty()) {
                    "unknown crawler journey refs: ${unknown.sorted()}"
                }
                selection.journeyRefs
                    .sorted()
                    .map { ref -> requireNotNull(byRef[ref]) }
            }

            is VexCrawlerSelection.Screens -> {
                val knownScreens = com.vextreme.vexlife.android.screens.AppScreens
                    .all
                    .map { it.screenId }
                    .toSet()
                val unknown = selection.screenIds - knownScreens
                require(unknown.isEmpty()) {
                    "unknown crawler Screen IDs: ${unknown.sorted()}"
                }

                all.filter { (_, journey) ->
                    val recorder = VexJourneyCoverageRecorder()
                    journey.walk(recorder)
                    recorder.snapshot().screenIds
                        .any(selection.screenIds::contains)
                }
            }
        }
    }
}
