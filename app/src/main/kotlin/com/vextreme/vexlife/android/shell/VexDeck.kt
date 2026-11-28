package com.vextreme.vexlife.android.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.conversation.toVexConversationGlance
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.elements.contextdock.VexContextDock
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockLabels
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockLevels
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockPage
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockReveal
import com.vextreme.vexlife.android.elements.contextdock.rememberVexContextDockState
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.conversation.VexFamilyConversationScreen
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsScreen
import com.vextreme.vexlife.android.screens.shared.maps.rememberVexNavigationGlance
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.ui.VexLifeRelayGraph
import com.vextreme.vexlife.android.ui.look.VexAmbientLiveness
import com.vextreme.vexlife.android.vexstream.rememberVexStreamNowPlaying

/**
 * Read-only: how much of the bottom of the viewport the resting deck covers (0 when no deck).
 *
 * AppFrame provides it; a stage Screen reads it to keep its own content above the resting deck while
 * still drawing its base layer underneath.
 */
internal val LocalVexDeckInset = compositionLocalOf { 0.dp }

/** Screens whose content is a stage: drawn full-bleed under the deck rather than above it. */
internal fun isVexStageScreen(
    screenId: String,
): Boolean = screenId == HomeScreen.screenId

/** VexMaps' map hosts its own dock (with the same Journey and Family pages); the shell deck stands aside. */
internal fun isVexDeckShownOn(
    screenId: String,
): Boolean = screenId != VexMapsScreen.screenId

/** Deck pages whose owner is already the Screen in front of the human. */
internal fun vexDeckPagesOwnedBy(
    screenId: String,
): Set<VexDeckPage> =
    when (screenId) {
        VexStreamMusicScreen.screenId -> setOf(VexDeckPage.Music)
        VexFamilyConversationScreen.screenId -> setOf(VexDeckPage.Family)
        VexMapsScreen.screenId -> setOf(VexDeckPage.Journey)
        else -> emptySet()
    }

/** Keeps a non-stage Screen's content clear of the resting deck. */
@Composable
internal fun VexDeckClearance(
    screenId: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(bottom = if (isVexStageScreen(screenId)) 0.dp else LocalVexDeckInset.current),
    ) {
        content()
    }
}

/** A screenshot / preview deck: the live set it projects and, optionally, a page and reveal to show. */
internal data class VexDeckFixture(
    val live: VexLiveSet,
    val page: VexDeckPage? = null,
    val reveal: VexContextDockReveal? = null,
    val musicIdle: VexDeckMusicIdle = VexDeckMusicIdle(paired = true, summary = null),
)

/** What the deck is doing, reported up to AppFrame: raised (Open / Full) decks hide the FAB. */
internal data class VexDeckFootprint(
    val raised: Boolean = false,
)

/** Every live glance the stage and deck read, plus the finite requests they may make. */
internal class VexLive(
    val set: VexLiveSet,
    val music: VexDeckMusicActions,
    val musicIdle: VexDeckMusicIdle,
)

@Composable
internal fun rememberVexLive(
    repositories: AppRepositories,
    relayGraph: VexLifeRelayGraph,
): VexLive {
    val music = rememberVexStreamNowPlaying()
    val navigation = rememberVexNavigationGlance()
    val conversation by
        relayGraph.conversation.outputStates.collectAsState(
            initial = relayGraph.conversation.currentOutput,
        )
    val vexStreamState by repositories.vexStream.state.collectAsState()
    val paired = vexStreamState.connectionState as? VexStreamConnectionState.Paired
    val summary =
        paired?.let {
            if (vexStreamState.hubState is VexStreamHubState.Ready) {
                val playable = vexStreamState.playableTracks()
                stringResource(
                    R.string.home_vexstream_library_summary,
                    playable.size,
                    playable.count { it.localCopy is VexStreamLocalCopyState.Downloaded },
                )
            } else {
                stringResource(R.string.home_vexstream_offline)
            }
        }

    return VexLive(
        set =
            VexLiveSet(
                navigation = navigation,
                nowPlaying = music?.nowPlaying,
                family = conversation.toVexConversationGlance(),
            ),
        music =
            VexDeckMusicActions(
                togglePlayback = { music?.togglePlayback?.invoke() },
                skipNext = { music?.skipNext?.invoke() },
                skipPrevious = { music?.skipPrevious?.invoke() },
            ),
        musicIdle = VexDeckMusicIdle(paired = paired != null, summary = summary),
    )
}

/** Ambient light input from the same feature-owned glances the stage reads. */
@Composable
internal fun rememberVexAmbientLiveness(): VexAmbientLiveness {
    val music = rememberVexStreamNowPlaying()
    val navigation = rememberVexNavigationGlance()

    return VexAmbientLiveness(
        musicPlaying = music?.nowPlaying?.let { it.playRequested && !it.ended } == true,
        navigating = navigation != null,
    )
}

/**
 * The convergence deck as a shell layer.
 *
 * It plans from [live] (pure [vexStagePlan]), keeps the human's page and reveal across Screens, re-
 * arranges once when the situation changes, and records every human page/reveal change as a runtime
 * interaction so what is in front of the human is recognized, not inferred.
 *
 * [initialPage] / [initialReveal] are screenshot fixtures; the running app passes neither.
 */
@Composable
internal fun VexDeckLayer(
    runtime: VexRuntime,
    live: VexLiveSet,
    music: VexDeckMusicActions,
    musicIdle: VexDeckMusicIdle,
    modifier: Modifier = Modifier,
    initialPage: VexDeckPage? = null,
    initialReveal: VexContextDockReveal? = null,
    onFootprintChanged: (VexDeckFootprint) -> Unit = {},
) {
    val screenId = runtime.currentScreen.screenId
    val plan =
        vexStagePlan(
            live = live,
            ownedHere = vexDeckPagesOwnedBy(screenId),
            onStageScreen = isVexStageScreen(screenId),
        )
    val state =
        rememberVexContextDockState(
            initialPageRef = (initialPage ?: plan?.preferredPage)?.pageRef,
            initialReveal = initialReveal ?: plan?.reveal ?: VexContextDockReveal.Peek,
        )
    var arrangedFor by rememberSaveable { mutableStateOf(plan?.context?.encode()) }
    var lastRecordedPage by remember { mutableStateOf(state.selectedPageRef) }

    LaunchedEffect(plan?.context) {
        val rearranged =
            plan?.let { vexDeckRearrangement(arrangedFor?.let(::decodeVexDeckContext), it) }
                ?: return@LaunchedEffect
        state.select(rearranged.preferredPage.pageRef)
        state.show(rearranged.reveal)
        lastRecordedPage = rearranged.preferredPage.pageRef
        arrangedFor = rearranged.context.encode()
    }

    val shown = plan != null && isVexDeckShownOn(screenId)
    val raised = shown && state.reveal >= VexContextDockReveal.Open
    SideEffect {
        onFootprintChanged(
            VexDeckFootprint(
                raised = raised,
            ),
        )
    }
    if (!shown || plan == null) {
        return
    }

    val labels =
        VexContextDockLabels(
            showMore = stringResource(R.string.deck_show_more),
            showLess = stringResource(R.string.deck_show_less),
            dismiss = stringResource(R.string.deck_dismiss),
            restore = stringResource(R.string.deck_restore),
        )
    val pages =
        plan.pages.map { page ->
            VexContextDockPage(
                pageRef = page.pageRef,
                label = stringResource(page.labelRes()),
                live = live.isLive(page),
            ) { reveal ->
                when (page) {
                    VexDeckPage.Music ->
                        VexDeckMusicPage(
                            runtime = runtime,
                            reveal = reveal,
                            nowPlaying = live.nowPlaying,
                            idle = musicIdle,
                            actions = music,
                        )

                    VexDeckPage.Journey ->
                        VexDeckJourneyPage(
                            runtime = runtime,
                            reveal = reveal,
                            navigation = live.navigation,
                        )

                    VexDeckPage.Family ->
                        VexDeckFamilyPage(
                            runtime = runtime,
                            reveal = reveal,
                            family = live.family,
                        )
                }
            }
        }

    VexContextDock(
        pages = pages,
        state = state,
        levels = VexContextDockLevels.Layered,
        labels = labels,
        modifier =
            modifier
                .vexElement(AppShell.Elements.Deck)
                .padding(horizontal = 8.dp)
                .padding(bottom = 8.dp),
        onSelectedPageChanged = { ref ->
            if (ref != lastRecordedPage) {
                lastRecordedPage = ref
                VexDeckPage.of(ref)?.let { page ->
                    runtime.recordDynamicInteraction(
                        subjectRef = "presentation.deck.page.${page.name.lowercase()}",
                        action = "Selected",
                    )
                }
            }
        },
        onRevealChanged = { reveal ->
            runtime.recordDynamicInteraction(
                subjectRef = "presentation.deck.reveal.${reveal.name.lowercase()}",
                action = "Changed",
            )
        },
    )
}

/** The live deck: [VexDeckLayer] fed by [rememberVexLive]. */
@Composable
internal fun VexDeckHost(
    runtime: VexRuntime,
    repositories: AppRepositories,
    relayGraph: VexLifeRelayGraph,
    modifier: Modifier = Modifier,
    onFootprintChanged: (VexDeckFootprint) -> Unit = {},
) {
    val live = rememberVexLive(repositories, relayGraph)
    VexDeckLayer(
        runtime = runtime,
        live = live.set,
        music = live.music,
        musicIdle = live.musicIdle,
        modifier = modifier,
        onFootprintChanged = onFootprintChanged,
    )
}

internal fun VexDeckPage.labelRes(): Int =
    when (this) {
        VexDeckPage.Music -> R.string.deck_page_music
        VexDeckPage.Journey -> R.string.deck_page_journey
        VexDeckPage.Family -> R.string.deck_page_family
    }

private fun VexDeckContext.encode(): String = "${stage.name}:$onStageScreen"

private fun decodeVexDeckContext(
    encoded: String,
): VexDeckContext? {
    val (stage, onStage) = encoded.split(":").takeIf { it.size == 2 } ?: return null
    return VexDeckContext(
        stage = VexStage.entries.firstOrNull { it.name == stage } ?: return null,
        onStageScreen = onStage.toBooleanStrictOrNull() ?: return null,
    )
}

// [VXG RealForever][SHELL][DECK]
