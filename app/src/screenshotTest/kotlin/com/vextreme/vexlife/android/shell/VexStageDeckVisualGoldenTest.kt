package com.vextreme.vexlife.android.shell

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockReveal
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.screens.primary.events.EventsScreen
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.primary.home.HomeScreenContent
import com.vextreme.vexlife.android.screens.shared.conversation.VexFamilyConversationScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreenContent
import com.vextreme.vexlife.android.ui.look.VexAmbientLiveness

/*
 * Semantic visual cases: Stage + Deck.
 *
 * The base layer is whatever deserves the screen (the road while navigating, Vex at rest), and the
 * convergence deck rides over it at the reveal the stage law chose or the human dragged it to.
 */

@Composable
private fun HomeStage(
    id: String,
    live: VexLiveSet,
    page: VexDeckPage? = null,
    reveal: VexContextDockReveal? = null,
) {
    VexPreviewHost(
        id = id,
        startScreen = HomeScreen,
        deck = VexDeckFixture(live = live, page = page, reveal = reveal),
        ambientLiveness =
            VexAmbientLiveness(
                musicPlaying = live.listening,
                navigating = live.navigating,
            ),
    ) { context ->
        HomeScreenContent(
            runtime = context.runtime,
            live = live,
            journey = fixtureJourneyStage.takeIf { live.navigating },
            musicPaired = true,
        )
    }
}

@PreviewTest
@Preview(name = "Stage · Map · deck peek · music", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun stageMapDeckPeekGolden() = HomeStage("golden-stage-map-peek", fixtureDriving)

@PreviewTest
@Preview(name = "Stage · Map · deck open · journey", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun stageMapDeckJourneyGolden() =
    HomeStage("golden-stage-map-journey", fixtureDriving, VexDeckPage.Journey, VexContextDockReveal.Open)

@PreviewTest
@Preview(name = "Stage · Map · deck tucked · light", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun stageMapDeckTuckedLightGolden() =
    HomeStage("golden-stage-map-tucked", fixtureDriving, VexDeckPage.Music, VexContextDockReveal.Tucked)

@PreviewTest
@Preview(name = "Stage · Home · music open", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun stageHomeMusicOpenGolden() = HomeStage("golden-stage-home-music", fixtureListening)

@PreviewTest
@Preview(name = "Stage · Home · music full", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun stageHomeMusicFullGolden() =
    HomeStage("golden-stage-home-music-full", fixtureListening, VexDeckPage.Music, VexContextDockReveal.Full)

@PreviewTest
@Preview(name = "Stage · Home · family full", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun stageHomeFamilyFullGolden() =
    HomeStage("golden-stage-home-family-full", fixtureListening, VexDeckPage.Family, VexContextDockReveal.Full)

@PreviewTest
@Preview(name = "Stage · Home · nothing live", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun stageHomeIdleGolden() = HomeStage("golden-stage-home-idle", VexLiveSet(family = fixtureFamilyUnavailable))

@PreviewTest
@Preview(name = "Deck · on Events · navigating", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun deckOnEventsGolden() {
    VexPreviewHost(
        id = "golden-deck-events",
        startScreen = EventsScreen,
        deck = VexDeckFixture(live = fixtureDriving),
    ) { context ->
        EventsScreen.renderContent(
            runtime = context.runtime,
            repositories = context.repositories,
            relayGraph = context.relayGraph,
        )
    }
}

@PreviewTest
@Preview(name = "Deck · on Family Conversation · tucked", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun deckOnFamilyConversationGolden() {
    VexPreviewHost(
        id = "golden-deck-family",
        startScreen = VexFamilyConversationScreen,
        deck = VexDeckFixture(live = fixtureListening.copy(nowPlaying = fixturePlaying.copy(playRequested = false))),
    ) { context ->
        VexFamilyConversationScreen.renderContent(
            runtime = context.runtime,
            repositories = context.repositories,
            relayGraph = context.relayGraph,
        )
    }
}

@PreviewTest
@Preview(name = "Talk to Vex · context lens", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun talkToVexContextLensGolden() {
    VexPreviewHost(
        id = "golden-talk-to-vex-lens",
        startScreen = TalkToVexScreen,
        deck = VexDeckFixture(live = fixtureDriving),
    ) {
        TalkToVexScreenContent(
            nowPlaying = fixturePlaying,
            navigation = fixtureNavigation,
            family = fixtureFamily,
        )
    }
}

// [VXG RealForever][SHELL][STAGE][DECK][VISUAL-GOLDEN]
