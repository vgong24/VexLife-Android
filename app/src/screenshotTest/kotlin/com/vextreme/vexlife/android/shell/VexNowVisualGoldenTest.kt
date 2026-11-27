package com.vextreme.vexlife.android.shell

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vextreme.vexlife.android.conversation.VexConversationGlance
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.screens.primary.events.EventsScreen
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.primary.home.HomeScreenContent
import com.vextreme.vexlife.android.screens.shared.conversation.VexFamilyConversationScreen
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGlance
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreenContent
import com.vextreme.vexlife.android.vexstream.VexStreamNowPlaying

/*
 * Semantic visual cases: one living surface.
 *
 * Feature glances are fixtures here because Layoutlib has no VexApplication process graph, Media3
 * Service or navigation Service. Each fixture is a value the owning feature's projection can really
 * produce (see VexStreamNowPlayingTest, VexNavigationGlanceTest, VexConversationGlanceTest).
 */

private val drivingNowPlaying =
    VexStreamNowPlaying(
        title = "Alright",
        artist = "Kendrick Lamar",
        playRequested = true,
        transitioning = false,
        playPauseAvailable = true,
        ended = false,
    )

private val drivingNavigation =
    VexNavigationGlance(
        phase = VexNavigationSessionPhase.ACTIVE,
        destinationLabel = "Sam's place",
        nextInstruction = "Turn left onto Main St",
        distanceToNextManeuverMeters = 297,
        distanceRemainingMeters = 7_240,
        plannedDurationSeconds = 22 * 60,
    )

private val familyAvailable =
    VexConversationGlance(
        homeAvailability = VexConversationHomeAvailability.Available,
        latestSpeakerLabel = "Maya",
        latestFromAuthenticatedParticipant = false,
        latestContent = "Yep, leaving now. See you there!",
        participantLabels = listOf("Victor", "Maya", "Vex"),
    )

private val familyUnavailable =
    VexConversationGlance.Unobserved.copy(
        homeAvailability = VexConversationHomeAvailability.Unavailable,
    )

@PreviewTest
@Preview(name = "Home · Now · nothing current", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun homeNowNothingCurrentGolden() {
    VexPreviewHost(
        id = "golden-home-now-idle",
        startScreen = HomeScreen,
    ) { context ->
        HomeScreenContent(
            runtime = context.runtime,
            repositories = context.repositories,
            family = familyUnavailable,
        )
    }
}

@PreviewTest
@Preview(name = "Home · Now · driving", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun homeNowDrivingGolden() {
    VexPreviewHost(
        id = "golden-home-now-driving",
        startScreen = HomeScreen,
    ) { context ->
        HomeScreenContent(
            runtime = context.runtime,
            repositories = context.repositories,
            nowPlaying = drivingNowPlaying,
            navigation = drivingNavigation,
            family = familyAvailable,
        )
    }
}

@PreviewTest
@Preview(name = "Home · Now · driving · light", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun homeNowDrivingLightGolden() {
    VexPreviewHost(
        id = "golden-home-now-driving-light",
        startScreen = HomeScreen,
    ) { context ->
        HomeScreenContent(
            runtime = context.runtime,
            repositories = context.repositories,
            nowPlaying = drivingNowPlaying,
            navigation = drivingNavigation,
            family = familyAvailable,
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
    ) {
        TalkToVexScreenContent(
            nowPlaying = drivingNowPlaying,
            navigation = drivingNavigation,
            family = familyAvailable,
        )
    }
}

@PreviewTest
@Preview(name = "Now strip · on Family Conversation", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun nowStripOnFamilyConversationGolden() {
    VexPreviewHost(
        id = "golden-now-strip-family",
        startScreen = VexFamilyConversationScreen,
        nowStrip = { runtime ->
            VexNowStrip(
                runtime = runtime,
                nowPlaying = drivingNowPlaying,
                navigation = drivingNavigation,
                onTogglePlayback = {},
            )
        },
    ) { context ->
        VexFamilyConversationScreen.renderContent(
            runtime = context.runtime,
            repositories = context.repositories,
            relayGraph = context.relayGraph,
        )
    }
}

@PreviewTest
@Preview(name = "Now strip · on Events · paused", widthDp = 412, heightDp = 915, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun nowStripOnEventsGolden() {
    VexPreviewHost(
        id = "golden-now-strip-events",
        startScreen = EventsScreen,
        nowStrip = { runtime ->
            VexNowStrip(
                runtime = runtime,
                nowPlaying = drivingNowPlaying.copy(playRequested = false),
                navigation = null,
                onTogglePlayback = {},
            )
        },
    ) { context ->
        EventsScreen.renderContent(
            runtime = context.runtime,
            repositories = context.repositories,
            relayGraph = context.relayGraph,
        )
    }
}

// [VXG RealForever][SHELL][NOW][VISUAL-GOLDEN]
