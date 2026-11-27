package com.vextreme.vexlife.android.ui.look

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * One color described the way a person perceives it, in OKLCH.
 *
 * [lightness] 0..1 (perceived lightness), [chroma] 0..~0.37 (colorfulness), [hue] degrees.
 * OKLCH is perceptually uniform: equal lightness reads as equally light across hues, so a whole
 * palette can be derived from a few hue seeds and still feel balanced. This is why Looks can be
 * generated (including a person's own hue) instead of hand-picking ~30 roles per theme.
 */
internal data class VexTone(
    val lightness: Double,
    val chroma: Double,
    val hue: Double,
) {
    init {
        require(lightness in 0.0..1.0) { "OKLCH lightness must be within 0..1" }
        require(chroma >= 0.0) { "OKLCH chroma must be non-negative" }
    }

    /**
     * The nearest displayable sRGB color with this lightness and hue.
     *
     * Out-of-gamut chroma is reduced (binary search) rather than clipped per channel, which would
     * shift hue and lightness.
     */
    fun toColor(
        alpha: Float = 1f,
    ): Color {
        var low = 0.0
        var high = chroma
        var best = linearSrgb(chroma)
        if (!best.inGamut()) {
            repeat(24) {
                val mid = (low + high) / 2
                val candidate = linearSrgb(mid)
                if (candidate.inGamut()) {
                    low = mid
                    best = candidate
                } else {
                    high = mid
                }
            }
            best = linearSrgb(low)
        }

        return Color(
            red = best.r.toSrgbChannel(),
            green = best.g.toSrgbChannel(),
            blue = best.b.toSrgbChannel(),
            alpha = alpha,
        )
    }

    private fun linearSrgb(
        chroma: Double,
    ): LinearRgb {
        val radians = hue * PI / 180.0
        val a = chroma * cos(radians)
        val b = chroma * sin(radians)

        // OKLab -> LMS (cube roots), Björn Ottosson's reference matrices.
        val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).cube()
        val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).cube()
        val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).cube()

        return LinearRgb(
            r = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            b = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
        )
    }

    private data class LinearRgb(
        val r: Double,
        val g: Double,
        val b: Double,
    ) {
        fun inGamut(): Boolean =
            r in -EPSILON..1 + EPSILON &&
                g in -EPSILON..1 + EPSILON &&
                b in -EPSILON..1 + EPSILON
    }

    companion object {
        private const val EPSILON = 1e-4

        /** OKLCH of one sRGB color; used by tests and to derive a Look from a person's chosen color. */
        fun of(
            color: Color,
        ): VexTone {
            val r = color.red.toDouble().toLinear()
            val g = color.green.toDouble().toLinear()
            val b = color.blue.toDouble().toLinear()

            val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
            val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
            val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)

            val lightness = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
            val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
            val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
            val chroma = kotlin.math.sqrt(a * a + bb * bb)
            val hue = (Math.toDegrees(kotlin.math.atan2(bb, a)) + 360.0) % 360.0

            return VexTone(
                lightness = lightness.coerceIn(0.0, 1.0),
                chroma = chroma,
                hue = hue,
            )
        }
    }
}

private fun Double.cube(): Double =
    this * this * this

private fun Double.toLinear(): Double =
    if (this <= 0.04045) this / 12.92 else ((this + 0.055) / 1.055).pow(2.4)

private fun Double.toSrgbChannel(): Float {
    val clamped = coerceIn(0.0, 1.0)
    val encoded =
        if (clamped <= 0.0031308) clamped * 12.92 else 1.055 * clamped.pow(1 / 2.4) - 0.055

    return encoded.toFloat().coerceIn(0f, 1f)
}

// [VXG RealForever][UI][LOOK][TONE]
