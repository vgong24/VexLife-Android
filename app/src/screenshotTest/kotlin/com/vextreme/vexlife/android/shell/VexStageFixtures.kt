package com.vextreme.vexlife.android.shell

import com.vextreme.vexlife.android.conversation.VexConversationGlance
import com.vextreme.vexlife.android.conversation.VexConversationGlanceLine
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.screens.shared.maps.VexJourneyStage
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGlance
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGlanceStep
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.screens.shared.maps.VexRouteGeometryPoint
import com.vextreme.vexlife.android.screens.shared.maps.cumulativeGeometryMeters
import com.vextreme.vexlife.android.vexstream.VexStreamNowPlaying
import com.vextreme.vexlife.android.vexstream.VexStreamNowPlayingTrack
import kotlin.math.cos
import kotlin.math.sin

/*
 * Shared fixtures for stage / deck screenshot cases.
 *
 * Layoutlib has no VexApplication process graph, Media3 Service or navigation Service, so glances
 * are fixtures. Each is a value the owning projection can really produce (see VexStageTest,
 * VexNavigationGlanceTest, VexStreamNowPlayingTest, VexConversationGlanceTest).
 */

/** A ~7 km city drive built from headings and lengths, so the schematic has real turns to draw. */
internal val fixtureRouteGeometry: List<VexRouteGeometryPoint> =
    run {
        val legs =
            listOf(
                0.0 to 500.0,
                90.0 to 650.0,
                0.0 to 700.0,
                20.0 to 120.0,
                40.0 to 120.0,
                60.0 to 120.0,
                90.0 to 500.0,
                0.0 to 1_200.0,
                330.0 to 160.0,
                300.0 to 160.0,
                270.0 to 400.0,
                0.0 to 1_800.0,
                30.0 to 260.0,
            )
        var latitude = 37.7590
        var longitude = -122.4310
        val points = mutableListOf(VexRouteGeometryPoint(latitude, longitude))
        legs.forEach { (heading, meters) ->
            val steps = (meters / 60.0).toInt().coerceAtLeast(1)
            repeat(steps) {
                val step = meters / steps
                val radians = Math.toRadians(heading)
                latitude += step * cos(radians) / 111_320.0
                longitude += step * sin(radians) / (111_320.0 * cos(Math.toRadians(latitude)))
                points += VexRouteGeometryPoint(latitude, longitude)
            }
        }
        points
    }

private const val FIXTURE_ALONG_METERS = 850

internal val fixtureJourneyStage =
    VexJourneyStage(
        routeKey = "route.fixture.sams-place",
        geometry = fixtureRouteGeometry,
        distanceAlongMeters = FIXTURE_ALONG_METERS,
    )

internal val fixtureNavigation =
    VexNavigationGlance(
        phase = VexNavigationSessionPhase.ACTIVE,
        destinationLabel = "Sam's place",
        nextInstruction = "Turn left onto Main St",
        distanceToNextManeuverMeters = 300,
        distanceRemainingMeters = cumulativeGeometryMeters(fixtureRouteGeometry).last() - FIXTURE_ALONG_METERS,
        plannedDurationSeconds = 22 * 60,
        upcoming =
            listOf(
                VexNavigationGlanceStep("Turn left onto Main St", 300),
                VexNavigationGlanceStep("Bear right onto Harbor Way", 1_000),
                VexNavigationGlanceStep("Turn left onto Ocean Ave", 1_860),
                VexNavigationGlanceStep("Turn left onto Pine St", 3_380),
                VexNavigationGlanceStep("Turn right onto Elm Ct", 5_520),
                VexNavigationGlanceStep("Arrive at Sam's place", 5_780),
            ),
    )

internal val fixturePlaying =
    VexStreamNowPlaying(
        title = "Golden Hour Drive",
        artist = "Lumen & Vale",
        playRequested = true,
        transitioning = false,
        playPauseAvailable = true,
        ended = false,
        upNext =
            listOf(
                VexStreamNowPlayingTrack("Night Market", "Paper Lanterns"),
                VexStreamNowPlayingTrack("Coastline", "Harbor Lights"),
                VexStreamNowPlayingTrack("Slow Burn", "Lumen & Vale"),
                VexStreamNowPlayingTrack("Afterglow", "Northbound"),
                VexStreamNowPlayingTrack("Paper Planes Home", "Quiet Night"),
            ),
        previousAvailable = true,
        nextAvailable = true,
    )

internal val fixtureFamily =
    VexConversationGlance(
        homeAvailability = VexConversationHomeAvailability.Available,
        latestSpeakerLabel = "Maya",
        latestFromAuthenticatedParticipant = false,
        latestContent = "Yep, leaving now. See you there!",
        participantLabels = listOf("Victor", "Maya", "Sam", "Vex"),
        recent =
            listOf(
                VexConversationGlanceLine("Sam", "Dinner's at 7. Door's unlocked, come straight up.", false),
                VexConversationGlanceLine("Victor", "Perfect, I'll grab drinks on the way", true),
                VexConversationGlanceLine("Maya", "Are you picking me up or meeting there?", false),
                VexConversationGlanceLine("Victor", "Meeting there, heading out now", true),
                VexConversationGlanceLine("Maya", "Yep, leaving now. See you there!", false),
            ),
    )

internal val fixtureFamilyUnavailable =
    VexConversationGlance.Unobserved.copy(
        homeAvailability = VexConversationHomeAvailability.Unavailable,
    )

internal val fixtureDriving =
    VexLiveSet(
        navigation = fixtureNavigation,
        nowPlaying = fixturePlaying,
        family = fixtureFamily,
    )

internal val fixtureListening =
    VexLiveSet(
        nowPlaying = fixturePlaying,
        family = fixtureFamily,
    )
