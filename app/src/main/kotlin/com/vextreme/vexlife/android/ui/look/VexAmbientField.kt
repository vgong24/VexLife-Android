package com.vextreme.vexlife.android.ui.look

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vextreme.vexlife.android.ui.currentVexThemePresentation
import kotlin.math.max

/** What is live right now, as far as ambient light is concerned. Presentation input only. */
internal data class VexAmbientLiveness(
    val musicPlaying: Boolean = false,
    val navigating: Boolean = false,
)

/**
 * VexLife's ambient light: a few very soft color fields behind the whole app.
 *
 * It reflects what is live: the Music light warms and breathes while something plays, the
 * Navigation light rises while a journey is active. Calm motion freezes it; Ambient off removes it.
 * It draws behind content and never carries information on its own.
 */
@Composable
internal fun VexAmbientField(
    liveness: VexAmbientLiveness,
    modifier: Modifier = Modifier,
) {
    val look = LocalVexLook.current
    if (!look.ambient) {
        return
    }
    val dark = !currentVexThemePresentation().lightSystemBars
    val spec = remember(look.choice) { look.choice.spec() }
    val colors = remember(spec, dark) { spec.ambientColors(dark) }
    val lively = look.motion == VexMotion.Lively

    val drift: Float
    val breath: Float
    if (lively) {
        val transition = rememberInfiniteTransition(label = "vex-ambient")
        drift =
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(DRIFT_MS, easing = LinearEasing), RepeatMode.Reverse),
                label = "vex-ambient-drift",
            ).value
        breath =
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(BREATH_MS), RepeatMode.Reverse),
                label = "vex-ambient-breath",
            ).value
    } else {
        drift = 0.35f
        breath = 0.5f
    }

    val musicLight by
        animateFloatAsState(
            targetValue = if (liveness.musicPlaying) 1f else 0f,
            animationSpec = tween(LIVENESS_MS),
            label = "vex-ambient-music",
        )
    val navigationLight by
        animateFloatAsState(
            targetValue = if (liveness.navigating) 1f else 0f,
            animationSpec = tween(LIVENESS_MS),
            label = "vex-ambient-navigation",
        )

    val intensity = spec.ambientIntensity * if (dark) 1f else 0.85f

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val radius = max(w, h) * 0.62f

        // Music / primary: upper left, breathing while something plays.
        softLight(
            color = colors[0],
            center = Offset(w * (0.12f + 0.22f * drift), h * (0.08f + 0.10f * drift)),
            radius = radius * (1f + 0.06f * breath * musicLight),
            alpha = intensity * (0.34f + 0.26f * musicLight + 0.08f * breath * musicLight),
        )
        // Navigation / tertiary: right, rising while a journey is active.
        softLight(
            color = colors[1],
            center = Offset(w * (0.98f - 0.20f * drift), h * (0.52f + 0.12f * drift)),
            radius = radius * 0.9f,
            alpha = intensity * (0.22f + 0.32f * navigationLight),
        )
        // Family / secondary: low, quiet.
        softLight(
            color = colors[2],
            center = Offset(w * (0.30f + 0.18f * drift), h * (1.02f - 0.08f * drift)),
            radius = radius * 0.85f,
            alpha = intensity * 0.20f,
        )
    }
}

private fun DrawScope.softLight(
    color: Color,
    center: Offset,
    radius: Float,
    alpha: Float,
) {
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha.coerceIn(0f, 1f)), Color.Transparent),
                center = center,
                radius = radius,
            ),
        radius = radius,
        center = center,
    )
}

private const val DRIFT_MS = 38_000
private const val BREATH_MS = 4_200
private const val LIVENESS_MS = 1_200

// [VXG RealForever][UI][LOOK][AMBIENT]
