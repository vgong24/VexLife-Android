package com.vextreme.vexlife.android.testing.crawler

import com.vextreme.vexlife.android.testing.journey.VexJourneyCatalog

/**
 * Human-editable default ScreenCrawler preset.
 *
 * Keep this small. It is intentionally a "change one row" surface for local/manual evidence runs.
 *
 * Shards are NOT re-registered here. [VexJourneyCatalog.shardsById] remains the canonical source.
 * The default is "all current and future shards", with optional exclusions.
 */
object VexScreenCrawlerRunConfig {
    /**
     * BCP-47-style language/locale codes.
     *
     * Current default:
     * - en
     *
     * Prepared examples (not product-supported-language claims):
     * - zh
     * - bg
     * - lt
     * - zh-Hans
     * - zh-Hant
     */
    val localeCodes: List<String> = listOf(
        "en",
        // "zh",
        // "bg",
        // "lt",
    )

    /**
     * Keep ordinary evidence runs at normal text unless a batch explicitly needs more.
     */
    val fontProfiles: List<VexCrawlerFontProfile> = listOf(
        VexCrawlerFonts.Normal,
        // VexCrawlerFonts.Large,
        // VexCrawlerFonts.ExtraLarge,
        // VexCrawlerFonts.Small,
    )

    /**
     * Canonical default: all current/future shards.
     *
     * To temporarily omit a shard, add its canonical ID below.
     * Available IDs are always derived from:
     *
     *   VexJourneyCatalog.shardsById.keys
     *
     * Do NOT mirror the full shard list here; that would create a second registry.
     */
    val selection: VexCrawlerSelection =
        VexCrawlerSelection.AllShardsExcept(
            excludedShardIds = setOf(
                // "shared.debug",
                // "shared.gallery",
            ),
        )

    /**
     * For an isolated partition, replace [selection] above with:
     *
     *   VexCrawlerSelection.Shards(
     *       setOf("shared.user-profile"),
     *   )
     *
     * Or use Screen/journey selection through the existing typed config builder/arguments.
     */
    val capturePolicy: VexCrawlerCapturePolicy =
        VexCrawlerCapturePolicy.SCREEN_ONCE

    val availableShardIds: Set<String>
        get() = VexJourneyCatalog.shardsById.keys

    fun build(
        sourceRef: String = "manual",
    ): VexScreenCrawlerConfig =
        VexScreenCrawlerConfig(
            selection = selection,
            locales = localeCodes.map(::VexCrawlerLocale),
            fonts = fontProfiles,
            capturePolicy = capturePolicy,
            sourceRef = sourceRef,
        )
}
