package com.vextreme.vexlife.android.shell

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.conversation.VexConversationGlance
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.VexIcon
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGlance
import com.vextreme.vexlife.android.ui.look.LocalVexLook
import com.vextreme.vexlife.android.ui.look.VexMotion
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.vexstream.VexStreamNowPlaying
import java.util.Locale

/*
 * Shared presentation vocabulary for "Now" surfaces: the app-shell Now strip, Home, and the Talk to
 * Vex context lens.
 *
 * Everything here formats an already-owned glance. Nothing here reads a Store, owns State or
 * requests an effect; that keeps one visual language across surfaces without a shared owner.
 */

/** One stable accent per feature, so a glance is recognizable wherever it appears. */
internal enum class VexNowFeature {
    Vex,
    Music,
    Navigation,
    Family,
}

internal data class VexNowAccent(
    val container:
        Color,
    val content:
        Color,
)

@Composable
@ReadOnlyComposable
internal fun VexNowFeature.accent(): VexNowAccent =
    when (
        this
    ) {
        VexNowFeature.Vex ->
            VexNowAccent(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onPrimary,
            )

        VexNowFeature.Music ->
            VexNowAccent(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
            )

        VexNowFeature.Navigation ->
            VexNowAccent(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
            )

        VexNowFeature.Family ->
            VexNowAccent(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
            )
    }

internal val VexNowFeature.icon: ElementIcon
    get() =
        when (
            this
        ) {
            VexNowFeature.Vex ->
                ElementIcon.Help

            VexNowFeature.Music ->
                ElementIcon.Music

            VexNowFeature.Navigation ->
                ElementIcon.Location

            VexNowFeature.Family ->
                ElementIcon.Person
        }

/** Rounded accent tile carrying one feature icon. */
@Composable
internal fun VexNowGlyph(
    feature:
        VexNowFeature,
    modifier:
        Modifier =
        Modifier,
    size:
        Dp =
        40.dp,
) {
    val accent =
        feature.accent()

    Surface(
        modifier =
            modifier.size(
                size,
            ),
        shape =
            RoundedCornerShape(
                size / 3,
            ),
        color =
            accent.container,
        contentColor =
            accent.content,
    ) {
        Box(
            contentAlignment =
                Alignment.Center,
        ) {
            VexIcon(
                icon =
                    feature.icon,
                contentDescription =
                    null,
            )
        }
    }
}

@Composable
internal fun VexStreamNowPlaying.displayTitle(): String =
    title.ifBlank {
        stringResource(
            R.string.now_untitled_track,
        )
    }

@Composable
internal fun VexStreamNowPlaying.stateLabel(): String =
    stringResource(
        when {
            ended ->
                R.string.now_playing_state_finished

            transitioning &&
                playRequested ->
                R.string.now_playing_state_starting

            transitioning ->
                R.string.now_playing_state_pausing

            playRequested ->
                R.string.now_playing_state_playing

            else ->
                R.string.now_playing_state_paused
        },
    )

/** Icon that says what tapping the toggle will do next. */
internal val VexStreamNowPlaying.toggleIcon: ElementIcon
    get() =
        if (
            playRequested &&
            !ended
        ) {
            ElementIcon.Pause
        } else {
            ElementIcon.Play
        }

/** The one line a person needs most: the next instruction, or what the journey is doing. */
@Composable
internal fun VexNavigationGlance.headline(): String =
    when (
        phase
    ) {
        VexNavigationSessionPhase.READY ->
            stringResource(
                R.string.now_navigation_ready,
            )

        VexNavigationSessionPhase.OFF_ROUTE ->
            stringResource(
                R.string.now_navigation_off_route,
            )

        VexNavigationSessionPhase.LOCATION_HELD ->
            stringResource(
                R.string.now_navigation_location_held,
            )

        VexNavigationSessionPhase.ARRIVED ->
            stringResource(
                R.string.now_navigation_arrived,
            )

        VexNavigationSessionPhase.ACTIVE,
        VexNavigationSessionPhase.IDLE,
        ->
            nextInstruction
                ?: stringResource(
                    R.string.now_navigation_continue,
                )
    }

/** "in 300 m · Sam's place · 7.2 km left", dropping whatever the session does not know. */
@Composable
internal fun VexNavigationGlance.detail(): String {
    val parts =
        mutableListOf<String>()

    if (
        phase ==
        VexNavigationSessionPhase.ACTIVE
    ) {
        distanceToNextManeuverMeters?.let {
            parts +=
                stringResource(
                    R.string.now_navigation_in_distance,
                    vexNowDistance(
                        it,
                    ),
                )
        }
    }
    parts +=
        destinationLabel
    if (
        phase !=
        VexNavigationSessionPhase.ARRIVED
    ) {
        distanceRemainingMeters?.let {
            parts +=
                stringResource(
                    R.string.now_navigation_left,
                    vexNowDistance(
                        it,
                    ),
                )
        }
    }

    return parts.joinToString(
        " · ",
    )
}

@Composable
internal fun vexNowDistance(
    meters:
        Int,
): String =
    if (
        meters < 1_000
    ) {
        stringResource(
            R.string.now_distance_meters,
            // Round to tens: a person reads "in 300 m", not "in 297 m".
            ((meters + 5) / 10) * 10,
        )
    } else {
        stringResource(
            R.string.now_distance_kilometers,
            String.format(
                Locale.getDefault(),
                "%.1f",
                meters / 1_000.0,
            ),
        )
    }

@Composable
internal fun vexNowDuration(
    seconds:
        Int,
): String {
    val totalMinutes =
        (seconds + 59) / 60

    return if (
        totalMinutes < 60
    ) {
        stringResource(
            R.string.now_duration_minutes,
            totalMinutes,
        )
    } else {
        stringResource(
            R.string.now_duration_hours,
            totalMinutes / 60,
            totalMinutes % 60,
        )
    }
}

/** "Maya: Yep, leaving now." or "Victor · you: …", or null when nothing has been said. */
@Composable
internal fun VexConversationGlance.latestLine(): String? {
    val content =
        latestContent
            ?: return null
    val speaker =
        latestSpeakerLabel
            ?: return content
    val label =
        if (
            latestFromAuthenticatedParticipant
        ) {
            speaker +
                " · " +
                stringResource(
                    R.string.home_family_you,
                )
        } else {
            speaker
        }

    return "$label: $content"
}

/**
 * Vex's presence mark: an orb in the same accent family as the feature glyphs (Music -> Navigation),
 * because Vex spans them. A soft halo breathes around it under Lively motion. Dimmed with a hollow
 * core while Vex is not connected; a solid core is reserved for a connected, attentive Vex.
 */
@Composable
internal fun VexPresenceOrb(
    connected: Boolean = false,
) {
    val colors =
        MaterialTheme.colorScheme
    val strength =
        if (connected) 1f else 0.72f
    val breath =
        rememberVexBreath(
            periodMs = 3_600,
        )

    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Halo: the orb's light, breathing.
        Box(
            modifier = Modifier
                .size((48 + 8 * breath).dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.30f - 0.12f * breath),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            colors.primaryContainer.copy(alpha = strength),
                            colors.tertiaryContainer.copy(alpha = strength),
                        ),
                    ),
                    shape = CircleShape,
                )
                .border(
                    width = 1.dp,
                    color = colors.onPrimaryContainer.copy(alpha = 0.35f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    if (connected) {
                        Modifier
                            .size(13.dp)
                            .background(
                                color = colors.onPrimaryContainer,
                                shape = CircleShape,
                            )
                    } else {
                        Modifier
                            .size(13.dp)
                            .border(
                                width = 2.dp,
                                color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                                shape = CircleShape,
                            )
                    },
            )
        }
    }
}

/** A small pulsing dot that says "this is live right now". Still under Calm motion. */
@Composable
internal fun VexLiveDot(
    feature: VexNowFeature,
) {
    val breath =
        rememberVexBreath(
            periodMs = 1_400,
        )
    val accent =
        feature.accent()

    Box(
        modifier = Modifier.size(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size((8 + 6 * breath).dp)
                .background(
                    color = accent.content.copy(alpha = 0.35f * (1f - breath)),
                    shape = CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    color = accent.content,
                    shape = CircleShape,
                ),
        )
    }
}

/**
 * One 0..1 breathing value. Lively motion animates it; Calm holds it still at rest, so nothing on
 * screen moves on its own.
 */
@Composable
internal fun rememberVexBreath(
    periodMs: Int,
): Float {
    if (LocalVexLook.current.motion != VexMotion.Lively) {
        return 0f
    }
    val transition =
        rememberInfiniteTransition(
            label = "vex-breath",
        )

    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(periodMs),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "vex-breath-value",
    ).value
}

// [VXG RealForever][SHELL][NOW][PRESENTATION]
