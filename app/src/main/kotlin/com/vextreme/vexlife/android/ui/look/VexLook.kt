package com.vextreme.vexlife.android.ui.look

import androidx.annotation.StringRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * A Look is VexLife's personality layer: palette, display typeface, shape and ambient light.
 *
 * Light vs Dark stays with ThemeMode; a Look answers the orthogonal question "what does VexLife feel
 * like?" Every Look is generated from a few OKLCH hue seeds, so every Material role is authored
 * (nothing leaks from Material's baseline purple) and a person's own hue is a first-class Look.
 *
 * Feature accents are stable across Looks: Music = primary, Navigation = tertiary, Family = secondary,
 * Vex = the primary -> tertiary gradient that spans them.
 */
internal enum class VexLookPreset(
    val key: String,
    @StringRes val nameRes: Int,
    @StringRes val moodRes: Int,
) {
    Nocturne("nocturne", R.string.look_nocturne, R.string.look_nocturne_mood),
    Aurora("aurora", R.string.look_aurora, R.string.look_aurora_mood),
    Ember("ember", R.string.look_ember, R.string.look_ember_mood),
    Tide("tide", R.string.look_tide, R.string.look_tide_mood),
    Paper("paper", R.string.look_paper, R.string.look_paper_mood),
}

/** What a person chose: one curated preset, or their own hue. */
internal sealed interface VexLookChoice {
    val key: String

    data class Preset(
        val preset: VexLookPreset,
    ) : VexLookChoice {
        override val key: String
            get() = "preset:" + preset.key
    }

    data class Custom(
        val hueDegrees: Int,
    ) : VexLookChoice {
        init {
            require(hueDegrees in 0..359) { "custom Look hue must be within 0..359" }
        }

        override val key: String
            get() = "custom:$hueDegrees"
    }

    companion object {
        val Default: VexLookChoice =
            Preset(VexLookPreset.Nocturne)

        /** Inverse of [key]; unknown or legacy values fall back to [Default] rather than failing. */
        fun parse(
            key: String?,
        ): VexLookChoice {
            if (key == null) return Default
            val (kind, value) =
                key.split(":", limit = 2).let { parts ->
                    parts.getOrNull(0) to parts.getOrNull(1)
                }

            return when (kind) {
                "preset" ->
                    VexLookPreset.entries
                        .firstOrNull { it.key == value }
                        ?.let(::Preset)
                        ?: Default

                "custom" ->
                    value
                        ?.toIntOrNull()
                        ?.takeIf { it in 0..359 }
                        ?.let(::Custom)
                        ?: Default

                else ->
                    Default
            }
        }
    }
}

/** How much the interface may move on its own. Calm honors people who prefer reduced motion. */
internal enum class VexMotion(
    val key: String,
) {
    Lively("lively"),
    Calm("calm"),
}

/** A person's complete appearance preference: one durable value, saved as one unit. */
internal data class VexLookPreference(
    val choice: VexLookChoice = VexLookChoice.Default,
    val motion: VexMotion = VexMotion.Lively,
    val ambient: Boolean = true,
) : VexFamily.State<VexLookFamily>

/** Hue/chroma seeds a Look is generated from (OKLCH). */
internal data class VexLookSeeds(
    val primaryHue: Double,
    val primaryChroma: Double,
    val secondaryHue: Double,
    val secondaryChroma: Double,
    val tertiaryHue: Double,
    val tertiaryChroma: Double,
    val neutralHue: Double,
    val neutralChroma: Double,
)

internal enum class VexDisplayFace {
    Sora,
    SpaceGrotesk,
    Fraunces,
}

/** Everything needed to render one Look, derived from a [VexLookChoice]. */
internal data class VexLookSpec(
    val seeds: VexLookSeeds,
    val displayFace: VexDisplayFace,
    val cornerDp: Int,
    val ambientIntensity: Float,
) {
    fun colorScheme(
        dark: Boolean,
    ): ColorScheme =
        if (dark) darkScheme() else lightScheme()

    /** Soft ambient light sources, strongest first: primary (Music), tertiary (Navigation), secondary. */
    fun ambientColors(
        dark: Boolean,
    ): List<Color> {
        val lightness = if (dark) 0.40 else 0.88
        val chromaScale = if (dark) 1.0 else 0.55

        return listOf(
            seeds.primaryHue to seeds.primaryChroma,
            seeds.tertiaryHue to seeds.tertiaryChroma,
            seeds.secondaryHue to seeds.secondaryChroma,
        ).map { (hue, chroma) ->
            VexTone(lightness, chroma * chromaScale, hue).toColor()
        }
    }

    fun shapes(): Shapes =
        Shapes(
            extraSmall = RoundedCornerShape((cornerDp / 4).dp),
            small = RoundedCornerShape((cornerDp / 2).dp),
            medium = RoundedCornerShape((cornerDp * 3 / 4).dp),
            large = RoundedCornerShape(cornerDp.dp),
            extraLarge = RoundedCornerShape((cornerDp + 8).dp),
        )

    private fun darkScheme(): ColorScheme {
        val s = seeds
        val primary = VexTone(0.80, s.primaryChroma, s.primaryHue).toColor()

        return darkColorScheme(
            primary = primary,
            onPrimary = VexTone(0.24, s.primaryChroma * 0.6, s.primaryHue).toColor(),
            primaryContainer = VexTone(0.40, s.primaryChroma * 0.9, s.primaryHue).toColor(),
            onPrimaryContainer = VexTone(0.93, s.primaryChroma * 0.4, s.primaryHue).toColor(),
            inversePrimary = VexTone(0.50, s.primaryChroma, s.primaryHue).toColor(),
            secondary = VexTone(0.80, s.secondaryChroma, s.secondaryHue).toColor(),
            onSecondary = VexTone(0.24, s.secondaryChroma * 0.6, s.secondaryHue).toColor(),
            secondaryContainer = VexTone(0.38, s.secondaryChroma * 0.9, s.secondaryHue).toColor(),
            onSecondaryContainer = VexTone(0.93, s.secondaryChroma * 0.4, s.secondaryHue).toColor(),
            tertiary = VexTone(0.80, s.tertiaryChroma, s.tertiaryHue).toColor(),
            onTertiary = VexTone(0.24, s.tertiaryChroma * 0.6, s.tertiaryHue).toColor(),
            tertiaryContainer = VexTone(0.39, s.tertiaryChroma * 0.9, s.tertiaryHue).toColor(),
            onTertiaryContainer = VexTone(0.93, s.tertiaryChroma * 0.4, s.tertiaryHue).toColor(),
            background = neutral(0.155),
            onBackground = neutral(0.95, 0.5),
            surface = neutral(0.175),
            onSurface = neutral(0.95, 0.5),
            surfaceVariant = neutral(0.28, 1.5),
            onSurfaceVariant = neutral(0.80, 1.6),
            surfaceTint = primary,
            inverseSurface = neutral(0.93, 0.6),
            inverseOnSurface = neutral(0.24),
            error = VexTone(0.75, 0.14, 25.0).toColor(),
            onError = VexTone(0.25, 0.08, 25.0).toColor(),
            errorContainer = VexTone(0.40, 0.12, 25.0).toColor(),
            onErrorContainer = VexTone(0.92, 0.04, 25.0).toColor(),
            outline = neutral(0.60, 1.5),
            outlineVariant = neutral(0.34, 1.5),
            scrim = Color.Black,
            surfaceBright = neutral(0.30),
            surfaceDim = neutral(0.155),
            surfaceContainerLowest = neutral(0.13),
            surfaceContainerLow = neutral(0.19),
            surfaceContainer = neutral(0.215),
            surfaceContainerHigh = neutral(0.245),
            surfaceContainerHighest = neutral(0.28),
        )
    }

    private fun lightScheme(): ColorScheme {
        val s = seeds
        val primary = VexTone(0.50, s.primaryChroma, s.primaryHue).toColor()

        return lightColorScheme(
            primary = primary,
            onPrimary = VexTone(0.99, 0.01, s.primaryHue).toColor(),
            primaryContainer = VexTone(0.90, s.primaryChroma * 0.45, s.primaryHue).toColor(),
            onPrimaryContainer = VexTone(0.30, s.primaryChroma * 0.8, s.primaryHue).toColor(),
            inversePrimary = VexTone(0.80, s.primaryChroma, s.primaryHue).toColor(),
            secondary = VexTone(0.50, s.secondaryChroma, s.secondaryHue).toColor(),
            onSecondary = VexTone(0.99, 0.01, s.secondaryHue).toColor(),
            secondaryContainer = VexTone(0.91, s.secondaryChroma * 0.45, s.secondaryHue).toColor(),
            onSecondaryContainer = VexTone(0.30, s.secondaryChroma * 0.8, s.secondaryHue).toColor(),
            tertiary = VexTone(0.52, s.tertiaryChroma, s.tertiaryHue).toColor(),
            onTertiary = VexTone(0.99, 0.01, s.tertiaryHue).toColor(),
            tertiaryContainer = VexTone(0.90, s.tertiaryChroma * 0.45, s.tertiaryHue).toColor(),
            onTertiaryContainer = VexTone(0.32, s.tertiaryChroma * 0.8, s.tertiaryHue).toColor(),
            background = neutral(0.975, 1.2),
            onBackground = neutral(0.20),
            surface = neutral(0.99, 0.6),
            onSurface = neutral(0.20),
            surfaceVariant = neutral(0.92, 1.4),
            onSurfaceVariant = neutral(0.42, 1.6),
            surfaceTint = primary,
            inverseSurface = neutral(0.28),
            inverseOnSurface = neutral(0.96),
            error = VexTone(0.55, 0.17, 25.0).toColor(),
            onError = VexTone(0.99, 0.01, 25.0).toColor(),
            errorContainer = VexTone(0.92, 0.05, 25.0).toColor(),
            onErrorContainer = VexTone(0.35, 0.12, 25.0).toColor(),
            outline = neutral(0.62, 1.5),
            outlineVariant = neutral(0.86, 1.5),
            scrim = Color.Black,
            surfaceBright = neutral(0.99, 0.6),
            surfaceDim = neutral(0.90),
            surfaceContainerLowest = neutral(1.0, 0.0),
            surfaceContainerLow = neutral(0.97),
            surfaceContainer = neutral(0.955),
            surfaceContainerHigh = neutral(0.935),
            surfaceContainerHighest = neutral(0.915),
        )
    }

    private fun neutral(
        lightness: Double,
        chromaScale: Double = 1.0,
    ): Color =
        VexTone(lightness, seeds.neutralChroma * chromaScale, seeds.neutralHue).toColor()
}

internal fun VexLookChoice.spec(): VexLookSpec =
    when (this) {
        is VexLookChoice.Preset -> preset.spec()
        is VexLookChoice.Custom ->
            VexLookSpec(
                seeds = VexLookSeeds(
                    primaryHue = hueDegrees.toDouble(),
                    primaryChroma = 0.14,
                    secondaryHue = (hueDegrees + 325) % 360.0,
                    secondaryChroma = 0.09,
                    tertiaryHue = (hueDegrees + 70) % 360.0,
                    tertiaryChroma = 0.12,
                    neutralHue = hueDegrees.toDouble(),
                    neutralChroma = 0.016,
                ),
                displayFace = VexDisplayFace.Sora,
                cornerDp = 20,
                ambientIntensity = 0.8f,
            )
    }

internal fun VexLookPreset.spec(): VexLookSpec =
    when (this) {
        // Ink and violet: the calm default.
        VexLookPreset.Nocturne ->
            VexLookSpec(
                seeds = VexLookSeeds(292.0, 0.15, 255.0, 0.09, 15.0, 0.13, 280.0, 0.018),
                displayFace = VexDisplayFace.Sora,
                cornerDp = 20,
                ambientIntensity = 0.75f,
            )

        // Midnight teal with mint and violet light, like the northern sky.
        VexLookPreset.Aurora ->
            VexLookSpec(
                seeds = VexLookSeeds(170.0, 0.13, 230.0, 0.10, 305.0, 0.14, 215.0, 0.022),
                displayFace = VexDisplayFace.SpaceGrotesk,
                cornerDp = 24,
                ambientIntensity = 1.0f,
            )

        // Warm charcoal, amber and rose: evening light.
        VexLookPreset.Ember ->
            VexLookSpec(
                seeds = VexLookSeeds(62.0, 0.14, 35.0, 0.11, 355.0, 0.13, 50.0, 0.016),
                displayFace = VexDisplayFace.Fraunces,
                cornerDp = 18,
                ambientIntensity = 0.85f,
            )

        // Deep ocean blues with sea-glass and sand.
        VexLookPreset.Tide ->
            VexLookSpec(
                seeds = VexLookSeeds(228.0, 0.12, 190.0, 0.09, 75.0, 0.11, 245.0, 0.022),
                displayFace = VexDisplayFace.SpaceGrotesk,
                cornerDp = 22,
                ambientIntensity = 0.85f,
            )

        // Editorial: warm paper, ink blue, a vermilion mark. Crisper corners, quieter light.
        VexLookPreset.Paper ->
            VexLookSpec(
                seeds = VexLookSeeds(262.0, 0.08, 135.0, 0.06, 35.0, 0.14, 85.0, 0.018),
                displayFace = VexDisplayFace.Fraunces,
                cornerDp = 12,
                ambientIntensity = 0.4f,
            )
    }

private val SoraFamily =
    FontFamily(
        Font(R.font.sora_regular, FontWeight.Normal),
        Font(R.font.sora_semibold, FontWeight.SemiBold),
        Font(R.font.sora_bold, FontWeight.Bold),
    )

private val SpaceGroteskFamily =
    FontFamily(
        Font(R.font.space_grotesk_medium, FontWeight.Medium),
        Font(R.font.space_grotesk_bold, FontWeight.Bold),
    )

private val FrauncesFamily =
    FontFamily(
        Font(R.font.fraunces_medium, FontWeight.Medium),
        Font(R.font.fraunces_bold, FontWeight.Bold),
    )

internal val VexDisplayFace.family: FontFamily
    get() =
        when (this) {
            VexDisplayFace.Sora -> SoraFamily
            VexDisplayFace.SpaceGrotesk -> SpaceGroteskFamily
            VexDisplayFace.Fraunces -> FrauncesFamily
        }

/**
 * Display face for display/headline/title roles; body text stays the platform face for reading.
 * labelSmall keeps the existing monospace diagnostic voice.
 */
internal fun VexLookSpec.typography(): Typography {
    val base = Typography()
    val display = displayFace.family
    val heavy = if (displayFace == VexDisplayFace.Fraunces) FontWeight.Bold else FontWeight.SemiBold

    fun TextStyle.display(
        weight: FontWeight = heavy,
    ): TextStyle =
        copy(fontFamily = display, fontWeight = weight)

    return base.copy(
        displayLarge = base.displayLarge.display(),
        displayMedium = base.displayMedium.display(),
        displaySmall = base.displaySmall.display(),
        headlineLarge = base.headlineLarge.display(FontWeight.Bold),
        headlineMedium = base.headlineMedium.display(FontWeight.Bold),
        headlineSmall = base.headlineSmall.display(),
        titleLarge = base.titleLarge.display(),
        titleMedium = base.titleMedium.display(),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.8.sp,
        ),
    )
}

// [VXG RealForever][UI][LOOK]
