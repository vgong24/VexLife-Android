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

fun language(code: String): VexCrawlerLocale =
    VexCrawlerLocale(code)

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
     * Canonical all-shards selection with optional typed exclusions.
     *
     * Future shards are included automatically because planning reads VexJourneyCatalog.shards.
     */
    data class AllShardsExcept(
        val excludedShards: Set<VexJourneyShard> = emptySet(),
    ) : VexCrawlerSelection

    /** Explicit "only these shards" partition using canonical shard objects. */
    data class Shards(
        val shards: Set<VexJourneyShard>,
    ) : VexCrawlerSelection

    /** Select every journey whose declared semantic walk touches any selected Screen. */
    data class Screens(
        val screens: Set<ScreenElement>,
    ) : VexCrawlerSelection

    /** Select exactly these canonical journey objects. */
    data class Journeys(
        val journeys: Set<VexJourney>,
    ) : VexCrawlerSelection
}

data class VexScreenCrawlerConfig(
    val selection: VexCrawlerSelection = VexCrawlerSelection.AllShards,
    val locales: List<VexCrawlerLocale> = listOf(language("en")),
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
    private var selectionWasExplicitlySet = false
    private val locales = mutableListOf<VexCrawlerLocale>()
    private val fonts = mutableListOf<VexCrawlerFontProfile>()
    private var capturePolicy = VexCrawlerCapturePolicy.SCREEN_ONCE
    private var sourceRef = "unknown"

    fun allShards() {
        setSelection(VexCrawlerSelection.AllShards)
    }

    fun allShardsExcept(vararg shards: VexJourneyShard) {
        setSelection(
            VexCrawlerSelection.AllShardsExcept(
                excludedShards = shards.toSet(),
            ),
        )
    }

    fun shards(vararg shards: VexJourneyShard) {
        setSelection(
            VexCrawlerSelection.Shards(
                shards = shards.toSet(),
            ),
        )
    }

    fun screens(vararg screens: ScreenElement) {
        setSelection(
            VexCrawlerSelection.Screens(
                screens = screens.toSet(),
            ),
        )
    }

    /** Run exactly one named journey/path. */
    fun journey(journey: VexJourney) {
        setSelection(
            VexCrawlerSelection.Journeys(
                journeys = setOf(journey),
            ),
        )
    }

    /** Run an explicit typed set of named journeys/paths. */
    fun journeys(vararg journeys: VexJourney) {
        setSelection(
            VexCrawlerSelection.Journeys(
                journeys = journeys.toSet(),
            ),
        )
    }

    private fun setSelection(value: VexCrawlerSelection) {
        check(!selectionWasExplicitlySet) {
            "ScreenCrawler config declares more than one selection; " +
                "keep only one of journey(s)/screens/shards/allShardsExcept/allShards"
        }
        selection = value
        selectionWasExplicitlySet = true
    }

    fun languages(vararg values: VexCrawlerLocale) {
        locales += values
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
                listOf(language("en"))
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
                ?.screens
                ?.map { screen -> screen.screenId }
                ?.toSet()
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
                val known = VexJourneyCatalog.shards.toSet()
                val unknown = selection.excludedShards - known
                require(unknown.isEmpty()) {
                    "crawler exclusion contains unregistered shard(s): " +
                        unknown.joinToString { shard -> shard.shardId }
                }

                all.filterNot { (shard, _) ->
                    shard in selection.excludedShards
                }
            }

            is VexCrawlerSelection.Shards -> {
                val known = VexJourneyCatalog.shards.toSet()
                val unknown = selection.shards - known
                require(unknown.isEmpty()) {
                    "crawler selection contains unregistered shard(s): " +
                        unknown.joinToString { shard -> shard.shardId }
                }

                all.filter { (shard, _) ->
                    shard in selection.shards
                }
            }

            is VexCrawlerSelection.Screens -> {
                val knownScreens =
                    com.vextreme.vexlife.android.screens.AppScreens
                        .all
                        .toSet()
                val unknown = selection.screens - knownScreens
                require(unknown.isEmpty()) {
                    "crawler selection contains unregistered Screen(s): " +
                        unknown.joinToString { screen -> screen.screenId }
                }

                val selectedIds =
                    selection.screens
                        .map { screen -> screen.screenId }
                        .toSet()

                all.filter { (_, journey) ->
                    val recorder = VexJourneyCoverageRecorder()
                    journey.walk(recorder)
                    recorder.snapshot().screenIds
                        .any(selectedIds::contains)
                }
            }

            is VexCrawlerSelection.Journeys -> {
                val registered =
                    all.map { (_, journey) -> journey }.toSet()
                val unknown = selection.journeys - registered
                require(unknown.isEmpty()) {
                    "crawler selection contains unregistered journey(s): " +
                        unknown.joinToString { journey -> journey.id }
                }

                all.filter { (_, journey) ->
                    journey in selection.journeys
                }
            }
        }
    }
}
