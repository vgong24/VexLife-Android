package com.vextreme.vexlife.android.ui.look

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class VexLookTest {
    private val everyLook: List<VexLookChoice> =
        VexLookPreset.entries.map { VexLookChoice.Preset(it) } +
            listOf(0, 90, 180, 270, 330).map { VexLookChoice.Custom(it) }

    @Test
    fun oklchRoundTripsDisplayableColors() {
        listOf(
            Color(0xFF5B3FA0),
            Color(0xFF2A9D8F),
            Color(0xFFE76F51),
            Color(0xFF808080),
        ).forEach { color ->
            val back = VexTone.of(color).toColor()
            assertEquals(color.red, back.red, 0.01f)
            assertEquals(color.green, back.green, 0.01f)
            assertEquals(color.blue, back.blue, 0.01f)
        }
        assertEquals(1.0, VexTone.of(Color.White).lightness, 0.001)
        assertEquals(0.0, VexTone.of(Color.Black).lightness, 0.001)
    }

    @Test
    fun outOfGamutChromaIsReducedNotClippedPerChannel() {
        // Absurd chroma: must still land in sRGB with the requested lightness and hue family.
        val color = VexTone(0.70, 0.40, 140.0).toColor()
        val back = VexTone.of(color)

        assertEquals(0.70, back.lightness, 0.02)
        assertTrue(abs(back.hue - 140.0) < 8.0)
    }

    @Test
    fun everyLookIsReadableInDarkAndLight() {
        everyLook.forEach { choice ->
            listOf(true, false).forEach { dark ->
                val s = choice.spec().colorScheme(dark)
                val name = "${choice.key} dark=$dark"
                assertContrast("$name onSurface/surface", s.onSurface, s.surface, 7.0)
                assertContrast("$name onSurface/containerHigh", s.onSurface, s.surfaceContainerHigh, 7.0)
                assertContrast("$name onSurfaceVariant/containerHigh", s.onSurfaceVariant, s.surfaceContainerHigh, 4.5)
                assertContrast("$name onPrimary/primary", s.onPrimary, s.primary, 4.5)
                assertContrast("$name onPrimaryContainer", s.onPrimaryContainer, s.primaryContainer, 4.5)
                assertContrast("$name onSecondaryContainer", s.onSecondaryContainer, s.secondaryContainer, 4.5)
                assertContrast("$name onTertiaryContainer", s.onTertiaryContainer, s.tertiaryContainer, 4.5)
            }
        }
    }

    @Test
    fun noRoleLeaksFromMaterialBaseline() {
        val baselineDarkPrimaryContainer = Color(0xFF4F378B)
        everyLook.forEach { choice ->
            val s: ColorScheme = choice.spec().colorScheme(dark = true)
            assertNotEquals(choice.key, baselineDarkPrimaryContainer, s.primaryContainer)
        }
    }

    @Test
    fun darkAndLightAreActuallyDarkAndLight() {
        everyLook.forEach { choice ->
            assertTrue(choice.key, choice.spec().colorScheme(true).background.luminance() < 0.05f)
            assertTrue(choice.key, choice.spec().colorScheme(false).background.luminance() > 0.85f)
        }
    }

    @Test
    fun choiceKeysRoundTripAndUnknownFallsBack() {
        everyLook.forEach { choice ->
            assertEquals(choice, VexLookChoice.parse(choice.key))
        }
        assertEquals(VexLookChoice.Default, VexLookChoice.parse(null))
        assertEquals(VexLookChoice.Default, VexLookChoice.parse("preset:retired-look"))
        assertEquals(VexLookChoice.Default, VexLookChoice.parse("custom:999"))
        assertEquals(VexLookChoice.Default, VexLookChoice.parse("garbage"))
    }

    private fun assertContrast(
        label: String,
        foreground: Color,
        background: Color,
        minimum: Double,
    ) {
        val a = foreground.luminance() + 0.05
        val b = background.luminance() + 0.05
        val ratio = max(a, b) / min(a, b)
        assertTrue("$label contrast %.2f < $minimum".format(ratio), ratio >= minimum)
    }
}

// [VXG RealForever][UI][LOOK][PROOF]
