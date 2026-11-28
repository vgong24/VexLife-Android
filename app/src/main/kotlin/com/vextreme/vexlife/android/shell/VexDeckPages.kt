package com.vextreme.vexlife.android.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.conversation.VexConversationGlance
import com.vextreme.vexlife.android.conversation.VexConversationGlanceLine
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexComposeScope
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexIconButton
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockReveal
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGlance
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGlanceStep
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.vexstream.VexStreamNowPlaying
import com.vextreme.vexlife.android.vexstream.VexStreamNowPlayingTrack
import kotlin.math.absoluteValue

/** What the Music page says when nothing is current, read by the host from the VexStream repository. */
internal data class VexDeckMusicIdle(
    val paired: Boolean,
    val summary: String?,
)

/** The finite music requests a deck page may make; each goes to the Playback / Queue Family. */
internal data class VexDeckMusicActions(
    val togglePlayback: () -> Unit = {},
    val skipNext: () -> Unit = {},
    val skipPrevious: () -> Unit = {},
)

/** Semantic parent for one deck page, so the lens reads "element.shell.deck.page.music/title". */
internal fun VexDeckPage.semanticRef(): String = "element.shell.deck.page.${name.lowercase()}"

// ---------------------------------------------------------------- Music

@Composable
internal fun VexDeckMusicPage(
    runtime: VexRuntime,
    reveal: VexContextDockReveal,
    nowPlaying: VexStreamNowPlaying?,
    idle: VexDeckMusicIdle,
    actions: VexDeckMusicActions,
) {
    VexComposeScope(parentRef = VexDeckPage.Music.semanticRef()) {
        if (nowPlaying == null) {
            VexDeckMusicIdlePage(runtime, reveal, idle)
            return@VexComposeScope
        }
        when (reveal) {
            VexContextDockReveal.Tucked,
            VexContextDockReveal.Peek,
            -> VexDeckPeekRow(
                leading = { VexDeckCover(seed = nowPlaying.displayTitle(), size = 46.dp) },
                title = nowPlaying.displayTitle(),
                detail = nowPlaying.artist.ifBlank { nowPlaying.stateLabel() },
                trailing = {
                    VexDeckToggle(runtime, nowPlaying, actions)
                    VexDeckSkip(runtime, AppShell.Elements.DeckMusicNext, nowPlaying.nextAvailable, actions.skipNext)
                },
            )

            VexContextDockReveal.Open ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VexDeckCover(seed = nowPlaying.displayTitle(), size = 72.dp)
                        VexDeckTitleBlock(nowPlaying, Modifier.weight(1f))
                    }
                    VexDeckTransport(runtime, nowPlaying, actions)
                    VexDeckUpNext(nowPlaying.upNext.take(3))
                }

            VexContextDockReveal.Full ->
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    VexDeckCover(
                        seed = nowPlaying.displayTitle(),
                        size = 248.dp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    VexDeckTitleBlock(nowPlaying, Modifier.fillMaxWidth(), centered = true)
                    VexDeckTransport(runtime, nowPlaying, actions)
                    VexDeckUpNext(nowPlaying.upNext)
                    VexButton(
                        element = AppShell.Elements.DeckMusicOpen,
                        handler = runtime,
                        style = VexElementStyles.SecondaryAction,
                    )
                }
        }
    }
}

@Composable
private fun VexDeckMusicIdlePage(
    runtime: VexRuntime,
    reveal: VexContextDockReveal,
    idle: VexDeckMusicIdle,
) {
    val title = stringResource(R.string.deck_music_idle_title)
    val detail =
        idle.summary
            ?: stringResource(
                if (idle.paired) R.string.deck_music_idle_paired else R.string.deck_music_idle_unpaired,
            )
    if (reveal <= VexContextDockReveal.Peek) {
        VexDeckPeekRow(
            leading = { VexNowGlyph(feature = VexNowFeature.Music, size = 46.dp) },
            title = title,
            detail = detail,
            trailing = { VexDeckOpen(runtime, AppShell.Elements.DeckMusicOpen) },
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VexDeckPeekRow(
                leading = { VexNowGlyph(feature = VexNowFeature.Music, size = 46.dp) },
                title = title,
                detail = detail,
            )
            VexText(
                id = "idle-body",
                text = stringResource(R.string.home_vexstream_disconnected_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VexButton(element = AppShell.Elements.DeckMusicOpen, handler = runtime)
        }
    }
}

@Composable
private fun VexDeckTitleBlock(
    nowPlaying: VexStreamNowPlaying,
    modifier: Modifier,
    centered: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        VexText(
            id = "title",
            text = nowPlaying.displayTitle(),
            style =
                if (centered) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (nowPlaying.artist.isNotBlank()) {
            VexText(
                id = "artist",
                text = nowPlaying.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        VexDeckPill(id = "state", text = nowPlaying.stateLabel(), feature = VexNowFeature.Music)
    }
}

@Composable
private fun VexDeckTransport(
    runtime: VexRuntime,
    nowPlaying: VexStreamNowPlaying,
    actions: VexDeckMusicActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VexDeckSkip(runtime, AppShell.Elements.DeckMusicPrevious, nowPlaying.previousAvailable, actions.skipPrevious)
        Box(
            modifier =
                Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            VexDeckToggle(runtime, nowPlaying, actions)
        }
        VexDeckSkip(runtime, AppShell.Elements.DeckMusicNext, nowPlaying.nextAvailable, actions.skipNext)
    }
}

@Composable
private fun VexDeckToggle(
    runtime: VexRuntime,
    nowPlaying: VexStreamNowPlaying,
    actions: VexDeckMusicActions,
) {
    VexIconButton(
        element = AppShell.Elements.DeckMusicToggle,
        handler =
            ElementActionHandler { element ->
                runtime.activate(element)
                actions.togglePlayback()
            },
        enabled = nowPlaying.playPauseAvailable && !nowPlaying.transitioning,
        iconOverride = nowPlaying.toggleIcon,
    )
}

@Composable
private fun VexDeckSkip(
    runtime: VexRuntime,
    element: ButtonElement,
    available: Boolean,
    request: () -> Unit,
) {
    VexIconButton(
        element = element,
        handler =
            ElementActionHandler { tapped ->
                runtime.activate(tapped)
                request()
            },
        enabled = available,
    )
}

@Composable
private fun VexDeckUpNext(
    tracks: List<VexStreamNowPlayingTrack>,
) {
    if (tracks.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VexText(
            id = "up-next",
            text = stringResource(R.string.deck_music_up_next),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        tracks.forEachIndexed { index, track ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VexDeckCover(seed = track.title, size = 36.dp)
                Column(Modifier.weight(1f)) {
                    VexText(
                        id = "up-next.$index.title",
                        text = track.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (track.artist.isNotBlank()) {
                        VexText(
                            id = "up-next.$index.artist",
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Generated cover art: VexStream carries no artwork today, so each title grows its own light from
 * the current Look's palette. Deterministic, so a song always looks like itself.
 */
@Composable
internal fun VexDeckCover(
    seed: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val hash = seed.hashCode().absoluteValue
    val palette =
        listOf(colors.primary, colors.tertiary, colors.secondary, colors.primaryContainer, colors.tertiaryContainer)
    val first = palette[hash % palette.size]
    val second = palette[(hash / 7 + 2) % palette.size]
    val fx = 0.2f + (hash % 60) / 100f
    val fy = 0.15f + (hash / 60 % 60) / 100f
    BoxWithLight(
        modifier = modifier.size(size).aspectRatio(1f),
        corner = size / 5,
        base = colors.surfaceContainerHighest,
        first = first,
        second = second,
        fx = fx,
        fy = fy,
    )
}

@Composable
private fun BoxWithLight(
    modifier: Modifier,
    corner: Dp,
    base: androidx.compose.ui.graphics.Color,
    first: androidx.compose.ui.graphics.Color,
    second: androidx.compose.ui.graphics.Color,
    fx: Float,
    fy: Float,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(corner))
                .background(base)
                .background(
                    Brush.linearGradient(
                        colors = listOf(first.copy(alpha = 0.95f), second.copy(alpha = 0.55f)),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                ),
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.62f)
                .aspectRatio(1f)
                .align(BiasAlignmentOf(fx, fy))
                .background(
                    Brush.radialGradient(
                        listOf(second.copy(alpha = 0.9f), second.copy(alpha = 0f)),
                    ),
                    CircleShape,
                ),
        )
    }
}

private fun BiasAlignmentOf(
    fx: Float,
    fy: Float,
): Alignment = androidx.compose.ui.BiasAlignment(fx * 2f - 1f, fy * 2f - 1f)

// ---------------------------------------------------------------- Journey

@Composable
internal fun VexDeckJourneyPage(
    runtime: VexRuntime,
    reveal: VexContextDockReveal,
    navigation: VexNavigationGlance?,
    // False when the page is already hosted by VexMaps, where "Open VexMaps" would lead nowhere.
    openable: Boolean = true,
) {
    VexComposeScope(parentRef = VexDeckPage.Journey.semanticRef()) {
        if (navigation == null) {
            val title = stringResource(R.string.home_journey_idle_title)
            if (reveal <= VexContextDockReveal.Peek) {
                VexDeckPeekRow(
                    leading = { VexNowGlyph(feature = VexNowFeature.Navigation, size = 46.dp) },
                    title = title,
                    detail = stringResource(R.string.deck_journey_idle_detail),
                    trailing =
                        if (openable) {
                            { VexDeckOpen(runtime, AppShell.Elements.DeckJourneyOpen) }
                        } else {
                            null
                        },
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    VexDeckPeekRow(
                        leading = { VexNowGlyph(feature = VexNowFeature.Navigation, size = 46.dp) },
                        title = title,
                        detail = stringResource(R.string.deck_journey_idle_detail),
                    )
                    VexText(
                        id = "idle-body",
                        text = stringResource(R.string.home_journey_idle_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (openable) {
                        VexButton(element = AppShell.Elements.DeckJourneyOpen, handler = runtime)
                    }
                }
            }
            return@VexComposeScope
        }

        when (reveal) {
            VexContextDockReveal.Tucked,
            VexContextDockReveal.Peek,
            -> VexDeckPeekRow(
                leading = { VexNowGlyph(feature = VexNowFeature.Navigation, size = 46.dp) },
                title = navigation.headline(),
                detail = navigation.detail(),
                trailing =
                    if (openable) {
                        { VexDeckOpen(runtime, AppShell.Elements.DeckJourneyOpen) }
                    } else {
                        null
                    },
            )

            VexContextDockReveal.Open,
            VexContextDockReveal.Full,
            ->
                Column(
                    modifier =
                        if (reveal == VexContextDockReveal.Full) {
                            Modifier.verticalScroll(rememberScrollState())
                        } else {
                            Modifier
                        },
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    VexDeckPeekRow(
                        leading = { VexNowGlyph(feature = VexNowFeature.Navigation, size = 46.dp) },
                        title = navigation.destinationLabel,
                        detail = navigation.progressLine() ?: navigation.headline(),
                    )
                    navigation.plannedDurationSeconds
                        ?.takeIf { navigation.phase != VexNavigationSessionPhase.ARRIVED }
                        ?.let { seconds ->
                            VexDeckPill(
                                id = "planned",
                                text = stringResource(R.string.now_navigation_planned, vexNowDuration(seconds)),
                                feature = VexNowFeature.Navigation,
                            )
                        }
                    VexDeckSteps(
                        steps =
                            if (reveal == VexContextDockReveal.Full) {
                                navigation.upcoming
                            } else {
                                navigation.upcoming.take(3)
                            },
                    )
                    if (openable) {
                        VexButton(
                            element = AppShell.Elements.DeckJourneyOpen,
                            handler = runtime,
                            style = VexElementStyles.SecondaryAction,
                        )
                    }
                }
        }
    }
}

/** "in 300 m · 5.8 km left": the journey's progress without repeating the destination beside it. */
@Composable
private fun VexNavigationGlance.progressLine(): String? {
    val parts = mutableListOf<String>()
    if (phase == VexNavigationSessionPhase.ACTIVE) {
        distanceToNextManeuverMeters?.let {
            parts += stringResource(R.string.now_navigation_in_distance, vexNowDistance(it))
        }
    }
    if (phase != VexNavigationSessionPhase.ARRIVED) {
        distanceRemainingMeters?.let {
            parts += stringResource(R.string.now_navigation_left, vexNowDistance(it))
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun VexDeckSteps(
    steps: List<VexNavigationGlanceStep>,
) {
    if (steps.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VexText(
            id = "ahead",
            text = stringResource(R.string.deck_journey_ahead),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        steps.forEachIndexed { index, step ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .background(
                                if (index == 0) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    VexText(
                        id = "step.$index.order",
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                VexText(
                    id = "step.$index.instruction",
                    text = step.instruction,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                VexText(
                    id = "step.$index.distance",
                    text = vexNowDistance(step.distanceFromHereMeters),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Family

@Composable
internal fun VexDeckFamilyPage(
    runtime: VexRuntime,
    reveal: VexContextDockReveal,
    family: VexConversationGlance,
) {
    VexComposeScope(parentRef = VexDeckPage.Family.semanticRef()) {
        val status =
            when (family.homeAvailability) {
                VexConversationHomeAvailability.Unavailable -> stringResource(R.string.home_family_unavailable_body)
                VexConversationHomeAvailability.Unobserved -> stringResource(R.string.home_family_unobserved_body)
                VexConversationHomeAvailability.Available ->
                    family.latestLine() ?: stringResource(R.string.home_family_empty_body)
            }
        val title =
            family.participantLabels
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" · ")
                ?: stringResource(R.string.home_family_eyebrow)

        if (reveal <= VexContextDockReveal.Peek || family.recent.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VexDeckPeekRow(
                    leading = { VexNowGlyph(feature = VexNowFeature.Family, size = 46.dp) },
                    title = title,
                    detail = status,
                    detailLines = if (reveal <= VexContextDockReveal.Peek) 1 else 3,
                    trailing =
                        if (reveal <= VexContextDockReveal.Peek) {
                            { VexDeckOpen(runtime, AppShell.Elements.DeckFamilyOpen) }
                        } else {
                            null
                        },
                )
                if (reveal > VexContextDockReveal.Peek) {
                    VexButton(element = AppShell.Elements.DeckFamilyOpen, handler = runtime)
                }
            }
            return@VexComposeScope
        }

        val lines =
            if (reveal == VexContextDockReveal.Full) family.recent else family.recent.takeLast(3)
        Column(
            modifier =
                if (reveal == VexContextDockReveal.Full) {
                    Modifier.verticalScroll(rememberScrollState())
                } else {
                    Modifier
                },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VexText(
                id = "participants",
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            lines.forEachIndexed { index, line ->
                VexDeckBubble(index, line)
            }
            Spacer(Modifier.height(2.dp))
            VexButton(element = AppShell.Elements.DeckFamilyOpen, handler = runtime)
        }
    }
}

@Composable
private fun ColumnScope.VexDeckBubble(
    index: Int,
    line: VexConversationGlanceLine,
) {
    val mine = line.fromAuthenticatedParticipant
    Column(
        modifier = Modifier.align(if (mine) Alignment.End else Alignment.Start).widthIn(max = 300.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        VexText(
            id = "line.$index.speaker",
            text = if (mine) stringResource(R.string.deck_family_you) else line.speakerLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VexText(
            id = "line.$index.content",
            text = line.content,
            modifier =
                Modifier
                    .background(
                        if (mine) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (mine) 18.dp else 6.dp,
                            bottomEnd = if (mine) 6.dp else 18.dp,
                        ),
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (mine) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
    }
}

// ---------------------------------------------------------------- Shared

@Composable
private fun VexDeckPeekRow(
    leading: @Composable () -> Unit,
    title: String,
    detail: String?,
    detailLines: Int = 1,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(modifier = Modifier.weight(1f)) {
            VexText(
                id = "headline",
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            detail?.let {
                VexText(
                    id = "detail",
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = detailLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.let {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { it() }
        }
    }
}

@Composable
private fun VexDeckOpen(
    runtime: VexRuntime,
    element: ButtonElement,
) {
    VexIconButton(
        element = element,
        handler = runtime,
    )
}

@Composable
private fun VexDeckPill(
    id: String,
    text: String,
    feature: VexNowFeature,
) {
    val accent = feature.accent()
    VexText(
        id = id,
        text = text,
        modifier =
            Modifier
                .background(accent.container, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = accent.content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

// [VXG RealForever][SHELL][DECK][PAGES]
