package com.vextreme.vexlife.android.screens.primary.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ColumnElement
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexComposeScope
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.navigateTo
import com.vextreme.vexlife.android.elements.vex
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.shared.conversation.VexFamilyConversationScreen
import com.vextreme.vexlife.android.screens.shared.maps.VexJourneyStage
import com.vextreme.vexlife.android.screens.shared.maps.VexJourneyStageMap
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsScreen
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGlance
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.rememberVexJourneyStage
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.shell.LocalVexDeckInset
import com.vextreme.vexlife.android.shell.VexLiveSet
import com.vextreme.vexlife.android.shell.VexNowFeature
import com.vextreme.vexlife.android.shell.VexNowGlyph
import com.vextreme.vexlife.android.shell.VexPresenceOrb
import com.vextreme.vexlife.android.shell.VexStage
import com.vextreme.vexlife.android.shell.headline
import com.vextreme.vexlife.android.shell.rememberVexLive
import com.vextreme.vexlife.android.shell.vexNowDistance
import com.vextreme.vexlife.android.shell.vexStagePlan
import com.vextreme.vexlife.android.ui.VexLifeRelayGraph

/**
 * Home is the stage: the base layer under the shell's convergence deck.
 *
 * While a journey is under way the stage is the road ahead (the route-matched position on a live
 * map). Otherwise it is Vex and a few quick starts, and the deck carries what is playing or said.
 * Which stage shows is the pure [vexStagePlan] of owner glances; Home owns none of their truth and
 * keeps no HomeState. Journey-referenced element ids are preserved.
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
                element = JourneySection,
                style = VexElementStyles.RaisedSurface,
            ) {
                button(VexMaps)
            }

            column(
                element = VexStreamSection,
                style = VexElementStyles.RaisedSurface,
            ) {
                button(ConnectVexStreamMusic)
                button(VexStreamMusic)
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
        val live =
            rememberVexLive(
                repositories = repositories,
                relayGraph = relayGraph,
            )

        HomeScreenContent(
            runtime = runtime,
            live = live.set,
            journey = rememberVexJourneyStage(),
            musicPaired = live.musicIdle.paired,
        )
    }
}

/**
 * Presentational Home stage. Live glances arrive as parameters so previews and screenshot cases can
 * render any real-world combination without a process graph.
 */
@Composable
internal fun HomeScreenContent(
    runtime: VexRuntime,
    live: VexLiveSet = VexLiveSet(),
    journey: VexJourneyStage? = null,
    musicPaired: Boolean = false,
) {
    val stage =
        vexStagePlan(live)?.stage
            ?.takeIf { it != VexStage.Map || journey != null }
            ?: VexStage.Home

    Crossfade(
        targetState = stage,
        label = "home-stage",
    ) { shown ->
        when (shown) {
            VexStage.Map ->
                HomeMapStage(
                    runtime = runtime,
                    navigation = live.navigation,
                    journey = requireNotNull(journey),
                )

            VexStage.Home ->
                HomeRestStage(
                    runtime = runtime,
                    musicPaired = musicPaired,
                )
        }
    }
}

/** The road ahead, with the next maneuver on top. Tapping either opens the full map tool. */
@Composable
private fun HomeMapStage(
    runtime: VexRuntime,
    navigation: VexNavigationGlance?,
    journey: VexJourneyStage,
) {
    val deckInset = LocalVexDeckInset.current
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        VexJourneyStageMap(
            stage = journey,
            modifier =
                Modifier
                    .fillMaxSize()
                    .vex(HomeScreen.VexMaps)
                    .clickable { runtime.activate(HomeScreen.VexMaps) },
        )

        navigation?.let { glance ->
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .vexElement(HomeScreen.JourneySection),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 8.dp,
            ) {
                VexComposeScope(parent = HomeScreen.JourneySection) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VexNowGlyph(
                            feature = VexNowFeature.Navigation,
                            size = 48.dp,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            glance.distanceToNextManeuverMeters
                                ?.takeIf { glance.phase == VexNavigationSessionPhase.ACTIVE }
                                ?.let { meters ->
                                    VexText(
                                        id = "distance",
                                        text = vexNowDistance(meters),
                                        style = MaterialTheme.typography.headlineMedium,
                                    )
                                }
                            VexText(
                                id = "headline",
                                text = glance.headline(),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            VexText(
                                id = "destination",
                                text = glance.destinationLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.vexmaps_attribution),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = deckInset + 6.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Nothing is moving: Vex, and the doors to each feature. The deck below carries what is current. */
@Composable
private fun HomeRestStage(
    runtime: VexRuntime,
    musicPaired: Boolean,
) {
    val deckInset = LocalVexDeckInset.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp, bottom = deckInset + 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .vexElement(HomeScreen.VexSection),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VexComposeScope(parent = HomeScreen.VexSection) {
                VexPresenceOrb(size = 132.dp)
                VexText(
                    id = "name",
                    text = stringResource(R.string.home_vex_eyebrow),
                    style = MaterialTheme.typography.displaySmall,
                )
                VexText(
                    id = "status",
                    text = stringResource(R.string.home_vex_not_connected),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                VexButton(
                    element = HomeScreen.TalkToVex,
                    handler = runtime,
                    style = VexElementStyles.PrimaryAction,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HomeQuickStart(
                runtime = runtime,
                section = HomeScreen.JourneySection,
                element = HomeScreen.VexMaps,
                feature = VexNowFeature.Navigation,
                label = stringResource(R.string.home_journey_idle_title),
                modifier = Modifier.weight(1f),
            )
            HomeQuickStart(
                runtime = runtime,
                section = HomeScreen.VexStreamSection,
                element =
                    if (musicPaired) {
                        HomeScreen.VexStreamMusic
                    } else {
                        HomeScreen.ConnectVexStreamMusic
                    },
                feature = VexNowFeature.Music,
                label = stringResource(R.string.deck_page_music),
                modifier = Modifier.weight(1f),
            )
            HomeQuickStart(
                runtime = runtime,
                section = HomeScreen.FamilySection,
                element = HomeScreen.FamilyConversation,
                feature = VexNowFeature.Family,
                label = stringResource(R.string.deck_page_family),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * One door on the rest stage. The tile carries the registered element's semantics, so a journey's
 * tap(HomeScreen.VexMaps) lands here exactly as it did on the old card button.
 */
@Composable
private fun HomeQuickStart(
    runtime: VexRuntime,
    section: ColumnElement,
    element: ButtonElement,
    feature: VexNowFeature,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.vexElement(section),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .vex(element)
                    .clickable { runtime.activate(element) },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VexNowGlyph(
                    feature = feature,
                    size = 44.dp,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
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
        )
    }
}

// [VXG RealForever]
