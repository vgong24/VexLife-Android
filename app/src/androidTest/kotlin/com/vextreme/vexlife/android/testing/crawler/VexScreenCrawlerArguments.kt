package com.vextreme.vexlife.android.testing.crawler

import androidx.test.platform.app.InstrumentationRegistry

internal data class VexScreenCrawlerParameter(
    val enabled: Boolean,
    val case: VexScreenCrawlerCase?,
    val disabledReason: String? = null,
) {
    override fun toString(): String =
        case?.caseRef ?: "screen-crawler-disabled"
}

internal object VexScreenCrawlerArguments {
    fun parameters(): List<VexScreenCrawlerParameter> {
        val arguments = InstrumentationRegistry.getArguments()
        val enabled = arguments
            .getString("vexCrawlerEnabled")
            ?.toBooleanStrictOrNull()
            ?: false

        if (!enabled) {
            return listOf(
                VexScreenCrawlerParameter(
                    enabled = false,
                    case = null,
                    disabledReason =
                        "set vexCrawlerEnabled=true to run ScreenCrawler",
                ),
            )
        }

        val config = VexScreenCrawlerConfig(
            selection = parseSelection(arguments),
            locales = parseLocales(
                arguments.getString("vexCrawlerLocales"),
            ),
            fonts = parseFonts(
                arguments.getString("vexCrawlerFonts"),
            ),
            capturePolicy = parseCapturePolicy(
                arguments.getString("vexCrawlerCapture"),
            ),
            sourceRef =
                arguments.getString("vexCrawlerSourceRef")
                    ?.takeIf { it.isNotBlank() }
                    ?: "unknown",
        )

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

    private fun parseSelection(
        arguments: android.os.Bundle,
    ): VexCrawlerSelection {
        val shards = arguments.csv("vexCrawlerShards")
        val screens = arguments.csv("vexCrawlerScreens")
        val journeys = arguments.csv("vexCrawlerJourneys")

        val supplied =
            listOf(shards, screens, journeys)
                .count { it.isNotEmpty() }

        require(supplied <= 1) {
            "choose only one crawler selector: shards, screens, or journeys"
        }

        return when {
            shards.isNotEmpty() ->
                VexCrawlerSelection.Shards(shards.toSet())

            screens.isNotEmpty() ->
                VexCrawlerSelection.Screens(screens.toSet())

            journeys.isNotEmpty() ->
                VexCrawlerSelection.Journeys(journeys.toSet())

            else ->
                VexCrawlerSelection.AllShards
        }
    }

    private fun parseLocales(raw: String?): List<VexCrawlerLocale> {
        val values = raw.csvOrDefault("en-US")

        return values.map { value ->
            when (value.lowercase()) {
                "en", "en-us" ->
                    VexCrawlerLocales.EnglishUs

                "zh", "zh-cn", "zh-hans" ->
                    VexCrawlerLocales.ChineseSimplified

                else ->
                    VexCrawlerLocale(
                        id = value
                            .lowercase()
                            .replace('_', '-')
                            .replace(
                                Regex("[^a-z0-9-]+"),
                                "-",
                            )
                            .trim('-'),
                        localeTag = value,
                    )
            }
        }
    }

    private fun parseFonts(
        raw: String?,
    ): List<VexCrawlerFontProfile> =
        raw.csvOrDefault("normal").map { value ->
            when (value.lowercase()) {
                "small" -> VexCrawlerFonts.Small
                "normal", "default" -> VexCrawlerFonts.Normal
                "large" -> VexCrawlerFonts.Large
                "extra-large", "xlarge" -> VexCrawlerFonts.ExtraLarge

                else -> {
                    val scale = value.toFloatOrNull()
                        ?: error(
                            "unknown crawler font '$value'; " +
                                "use small|normal|large|extra-large|<scale>",
                        )
                    VexCrawlerFontProfile(
                        id = "custom-" +
                            value.replace('.', '-') + "x",
                        scale = scale,
                    )
                }
            }
        }

    private fun parseCapturePolicy(
        raw: String?,
    ): VexCrawlerCapturePolicy =
        when (raw?.lowercase()?.trim()) {
            null, "", "screen-once" ->
                VexCrawlerCapturePolicy.SCREEN_ONCE

            "every-step" ->
                VexCrawlerCapturePolicy.EVERY_STEP

            "final-only" ->
                VexCrawlerCapturePolicy.FINAL_ONLY

            else ->
                error(
                    "unknown vexCrawlerCapture '$raw'; " +
                        "use screen-once|every-step|final-only",
                )
        }
}

private fun android.os.Bundle.csv(
    key: String,
): List<String> =
    getString(key)
        ?.split(',')
        ?.map(String::trim)
        ?.filter(String::isNotBlank)
        .orEmpty()

private fun String?.csvOrDefault(
    defaultValue: String,
): List<String> =
    this
        ?.split(',')
        ?.map(String::trim)
        ?.filter(String::isNotBlank)
        ?.takeIf(List<String>::isNotEmpty)
        ?: listOf(defaultValue)
