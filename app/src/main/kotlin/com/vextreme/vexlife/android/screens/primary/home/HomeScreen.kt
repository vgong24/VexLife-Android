package com.vextreme.vexlife.android.screens.primary.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.conversation.VexConversationGlance
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.conversation.toVexConversationGlance
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.elements.ColumnElement
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexContainer
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexIconButton
import com.vextreme.vexlife.android.elements.VexRow
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.localEffect
import com.vextreme.vexlife.android.elements.navigateTo
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.shared.conversation.VexFamilyConversationScreen
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsScreen
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGlance
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.rememberVexNavigationGlance
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.shell.VexNowFeature
import com.vextreme.vexlife.android.shell.VexLiveDot
import com.vextreme.vexlife.android.shell.VexNowGlyph
import com.vextreme.vexlife.android.shell.VexPresenceOrb
import com.vextreme.vexlife.android.shell.accent
import com.vextreme.vexlife.android.shell.detail
import com.vextreme.vexlife.android.shell.displayTitle
import com.vextreme.vexlife.android.shell.headline
import com.vextreme.vexlife.android.shell.latestLine
import com.vextreme.vexlife.android.shell.stateLabel
import com.vextreme.vexlife.android.shell.toggleIcon
import com.vextreme.vexlife.android.shell.vexNowDuration
import com.vextreme.vexlife.android.ui.VexLifeRelayGraph
import com.vextreme.vexlife.android.vexstream.VexStreamNowPlaying
import com.vextreme.vexlife.android.vexstream.rememberVexStreamNowPlaying

/**
 * Home is "Now": one living view of what is current across VexLife.
 *
 * Each card is a projection of a rightful owner's glance (VexStream Player, VexMaps Navigation,
 * Family Conversation). Home composes them and may reorder them by liveness; it owns none of their
 * truth and keeps no HomeState. Journey-referenced element ids are preserved.
 */
object HomeScreen : AppScreenBlueprint(
    screenKey = "home",
    titleRes = R.string.home_title,
) {
    val VexSection = columnRef(
        "body.vex",
    )

    val VexStreamSection = columnRef(
        "body.vexstream",
    )

    val JourneySection = columnRef(
        "body.journey",
    )

    val FamilySection = columnRef(
        "body.family",
    )

    val ConnectVexStreamMusic = buttonRef(
        id = "body.vexstream.connect",
        labelRes = R.string.home_vexstream_connect,
        style = VexElementStyles.PrimaryAction,
        intent = navigateTo(VexStreamMusicScreen),
    )

    /*
     * Preserve the already-observed semantic ID even though the product has graduated from the
     * provisional "Vex systems" visual grouping into its own Home section.
     */
    val VexStreamMusic = buttonRef(
        id = "body.systems.vexstream-music",
        labelRes = R.string.home_vexstream_open,
        style = VexElementStyles.PrimaryAction,
        intent = navigateTo(VexStreamMusicScreen),
    )

    /** Human toggle on a visible Now Playing card; realized by the Playback Family. */
    val NowPlayingToggle = buttonRef(
        id = "body.vexstream.play-pause",
        labelRes = R.string.now_strip_toggle_playback,
        style = VexElementStyles.ToolbarIcon,
        icon = com.vextreme.vexlife.android.elements.ElementIcon.PlayPause,
        intent = localEffect(),
    )

    val FamilyConversation = buttonRef(
        id = "body.systems.family-conversation",
        labelRes = R.string.conversation_family_title,
        style = VexElementStyles.SecondaryAction,
        intent = navigateTo(VexFamilyConversationScreen),
    )

    val VexMaps = buttonRef(
        id = "body.systems.vex-maps",
        labelRes = R.string.home_vexmaps,
        style = VexElementStyles.SecondaryAction,
        intent = navigateTo(VexMapsScreen),
    )

    val TalkToVex = buttonRef(
        id = "body.systems.talk-to-vex",
        labelRes = R.string.home_talk_to_vex,
        style = VexElementStyles.SecondaryAction,
        intent = navigateTo(TalkToVexScreen),
    )

    override val content = layout {
        scrollableColumn("body") {
            column(
                element = VexSection,
                style = VexElementStyles.RaisedSurface,
            ) {
                button(TalkToVex)
            }

            column(
                element = VexStreamSection,
                style = VexElementStyles.RaisedSurface,
            ) {
                button(ConnectVexStreamMusic)
                button(VexStreamMusic)
                button(NowPlayingToggle)
            }

            column(
                element = JourneySection,
                style = VexElementStyles.RaisedSurface,
            ) {
                button(VexMaps)
            }

            column(
                element = FamilySection,
                style = VexElementStyles.RaisedSurface,
            ) {
                button(FamilyConversation)
            }
        }
    }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        relayGraph: VexLifeRelayGraph,
    ) {
        val music =
            rememberVexStreamNowPlaying()
        val navigation =
            rememberVexNavigationGlance()
        val conversation by
            relayGraph
                .conversation
                .outputStates
                .collectAsState(
                    initial =
                        relayGraph.conversation.currentOutput,
                )

        HomeScreenContent(
            runtime = runtime,
            repositories = repositories,
            nowPlaying = music?.nowPlaying,
            navigation = navigation,
            family = conversation.toVexConversationGlance(),
            onTogglePlayback = {
                music
                    ?.togglePlayback
                    ?.invoke()
            },
        )
    }
}

private enum class HomeCard {
    Listening,
    Journey,
    Family,
}

/**
 * Presentational Home. Live glances arrive as parameters so previews and screenshot cases can
 * render any real-world combination without a process graph.
 */
@Composable
internal fun HomeScreenContent(
    runtime: VexRuntime,
    repositories: AppRepositories,
    nowPlaying: VexStreamNowPlaying? = null,
    navigation: VexNavigationGlance? = null,
    family: VexConversationGlance = VexConversationGlance.Unobserved,
    onTogglePlayback: () -> Unit = {},
) {
    // What is happening now rises to the top; otherwise a stable, calm order.
    val order =
        listOfNotNull(
            HomeCard.Journey.takeIf { navigation != null },
            HomeCard.Listening.takeIf { nowPlaying != null },
        ).let { live ->
            live + HomeCard.entries.filterNot { it in live }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState(),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HomeVexCard(
            runtime = runtime,
        )
        order.forEach { card ->
            key(card) {
                when (card) {
                    HomeCard.Listening ->
                        HomeListeningCard(
                            runtime = runtime,
                            repositories = repositories,
                            nowPlaying = nowPlaying,
                            onTogglePlayback = onTogglePlayback,
                        )

                    HomeCard.Journey ->
                        HomeJourneyCard(
                            runtime = runtime,
                            navigation = navigation,
                        )

                    HomeCard.Family ->
                        HomeFamilyCard(
                            runtime = runtime,
                            family = family,
                        )
                }
            }
        }
        // Room for the shell FAB so the last card can scroll fully clear of it.
        Spacer(
            Modifier.height(72.dp),
        )
    }
}

@Composable
private fun HomeVexCard(
    runtime: VexRuntime,
) {
    VexContainer(
        element = HomeScreen.VexSection,
        style = VexElementStyles.RaisedSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VexPresenceOrb()
            Column(
                modifier = Modifier.weight(1f),
            ) {
                VexText(
                    id = "name",
                    text = stringResource(R.string.home_vex_eyebrow),
                    style = MaterialTheme.typography.titleLarge,
                )
                VexText(
                    id = "status",
                    text = stringResource(R.string.home_vex_not_connected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            VexButton(
                element = HomeScreen.TalkToVex,
                handler = runtime,
            )
        }
    }
}

@Composable
private fun HomeNowCard(
    element: ColumnElement,
    feature: VexNowFeature,
    eyebrow: String,
    live: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    VexContainer(
        element = element,
        style = VexElementStyles.RaisedSurface,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VexNowGlyph(
                feature = feature,
                size = 28.dp,
            )
            VexText(
                id = "eyebrow",
                text = eyebrow,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (live) {
                VexLiveDot(
                    feature = feature,
                )
            }
        }
        content()
    }
}

@Composable
private fun HomeListeningCard(
    runtime: VexRuntime,
    repositories: AppRepositories,
    nowPlaying: VexStreamNowPlaying?,
    onTogglePlayback: () -> Unit,
) {
    val vexStreamState by repositories.vexStream.state.collectAsState()
    val paired =
        vexStreamState.connectionState as? VexStreamConnectionState.Paired

    HomeNowCard(
        element = HomeScreen.VexStreamSection,
        feature = VexNowFeature.Music,
        live = nowPlaying?.let { it.playRequested && !it.ended } == true,
        eyebrow =
            stringResource(
                if (nowPlaying != null) {
                    R.string.home_listening_eyebrow
                } else {
                    R.string.home_vexstream_music
                },
            ),
    ) {
        when {
            nowPlaying != null -> {
                VexText(
                    id = "title",
                    text = nowPlaying.displayTitle(),
                    style = MaterialTheme.typography.headlineSmall,
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
                VexRow(
                    id = "controls",
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HomeStatePill(
                        id = "state",
                        text = nowPlaying.stateLabel(),
                        feature = VexNowFeature.Music,
                    )
                    Box(Modifier.weight(1f))
                    VexIconButton(
                        element = HomeScreen.NowPlayingToggle,
                        handler =
                            ElementActionHandler { element ->
                                runtime.activate(element)
                                onTogglePlayback()
                            },
                        enabled =
                            nowPlaying.playPauseAvailable &&
                                !nowPlaying.transitioning,
                        iconOverride = nowPlaying.toggleIcon,
                    )
                    VexButton(
                        element = HomeScreen.VexStreamMusic,
                        handler = runtime,
                        style = VexElementStyles.SecondaryAction,
                    )
                }
            }

            paired == null -> {
                VexText(
                    id = "disconnected",
                    text = stringResource(R.string.home_vexstream_disconnected_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VexButton(
                    element = HomeScreen.ConnectVexStreamMusic,
                    handler = runtime,
                )
            }

            else -> {
                val playable =
                    vexStreamState.playableTracks()
                VexText(
                    id = "connection",
                    text =
                        if (vexStreamState.hubState is VexStreamHubState.Ready) {
                            stringResource(
                                R.string.home_vexstream_connected,
                                paired.displayName,
                            )
                        } else {
                            stringResource(R.string.home_vexstream_offline)
                        },
                    style = MaterialTheme.typography.titleMedium,
                )
                VexText(
                    id = "library",
                    text =
                        stringResource(
                            R.string.home_vexstream_library_summary,
                            playable.size,
                            playable.count {
                                it.localCopy is VexStreamLocalCopyState.Downloaded
                            },
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VexButton(
                    element = HomeScreen.VexStreamMusic,
                    handler = runtime,
                )
            }
        }
    }
}

@Composable
private fun HomeJourneyCard(
    runtime: VexRuntime,
    navigation: VexNavigationGlance?,
) {
    HomeNowCard(
        element = HomeScreen.JourneySection,
        feature = VexNowFeature.Navigation,
        live = navigation != null,
        eyebrow = stringResource(R.string.home_journey_eyebrow),
    ) {
        if (navigation == null) {
            VexText(
                id = "idle-title",
                text = stringResource(R.string.home_journey_idle_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            VexText(
                id = "idle-body",
                text = stringResource(R.string.home_journey_idle_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            VexText(
                id = "headline",
                text = navigation.headline(),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            VexText(
                id = "detail",
                text = navigation.detail(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Labelled as planned: VexMaps owns no live time-remaining figure yet.
            navigation.plannedDurationSeconds
                ?.takeIf {
                    navigation.phase != VexNavigationSessionPhase.ARRIVED
                }
                ?.let { seconds ->
                    HomeStatePill(
                        id = "planned",
                        text =
                            stringResource(
                                R.string.now_navigation_planned,
                                vexNowDuration(seconds),
                            ),
                        feature = VexNowFeature.Navigation,
                    )
                }
        }
        VexButton(
            element = HomeScreen.VexMaps,
            handler = runtime,
        )
    }
}

@Composable
private fun HomeFamilyCard(
    runtime: VexRuntime,
    family: VexConversationGlance,
) {
    HomeNowCard(
        element = HomeScreen.FamilySection,
        feature = VexNowFeature.Family,
        eyebrow = stringResource(R.string.home_family_eyebrow),
    ) {
        when (family.homeAvailability) {
            VexConversationHomeAvailability.Unavailable ->
                VexText(
                    id = "unavailable",
                    text = stringResource(R.string.home_family_unavailable_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

            VexConversationHomeAvailability.Unobserved ->
                VexText(
                    id = "unobserved",
                    text = stringResource(R.string.home_family_unobserved_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

            VexConversationHomeAvailability.Available -> {
                if (family.participantLabels.isNotEmpty()) {
                    VexText(
                        id = "participants",
                        text = family.participantLabels.joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                VexText(
                    id = "latest",
                    text =
                        family.latestLine()
                            ?: stringResource(R.string.home_family_empty_body),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        VexButton(
            element = HomeScreen.FamilyConversation,
            handler = runtime,
        )
    }
}

@Composable
private fun HomeStatePill(
    id: String,
    text: String,
    feature: VexNowFeature,
) {
    val accent =
        feature.accent()
    VexText(
        id = id,
        text = text,
        modifier = Modifier
            .background(
                color = accent.container,
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = accent.content,
    )
}

@VexScreenPreviews
@Composable
private fun HomeScreenPreview() {
    VexPreviewHost(
        id = "home",
        startScreen = HomeScreen,
    ) { context ->
        HomeScreenContent(
            runtime = context.runtime,
            repositories = context.repositories,
        )
    }
}

// [VXG RealForever]
