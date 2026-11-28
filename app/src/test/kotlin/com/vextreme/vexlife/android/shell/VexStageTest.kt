package com.vextreme.vexlife.android.shell

import com.vextreme.vexlife.android.conversation.VexConversationGlance
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockReveal
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGlance
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.vexstream.VexStreamNowPlaying
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexStageTest {
    private fun journey(
        phase: VexNavigationSessionPhase = VexNavigationSessionPhase.ACTIVE,
    ) =
        VexNavigationGlance(
            phase = phase,
            destinationLabel = "Sam's place",
            nextInstruction = "Turn left onto Main St",
            distanceToNextManeuverMeters = 300,
            distanceRemainingMeters = 7200,
            plannedDurationSeconds = 1320,
        )

    private fun music(
        playing: Boolean = true,
    ) =
        VexStreamNowPlaying(
            title = "Alright",
            artist = "Kendrick Lamar",
            playRequested = playing,
            transitioning = false,
            playPauseAvailable = true,
            ended = false,
        )

    private val familyWithLatest =
        VexConversationGlance(
            homeAvailability = VexConversationHomeAvailability.Available,
            latestSpeakerLabel = "Maya",
            latestFromAuthenticatedParticipant = false,
            latestContent = "Yep, leaving now.",
            participantLabels = listOf("Victor", "Maya"),
        )

    @Test
    fun navigatingPutsTheMapOnStageWithWhatIsPlayingAtPeek() {
        val plan = requireNotNull(vexStagePlan(VexLiveSet(navigation = journey(), nowPlaying = music())))

        assertEquals(VexStage.Map, plan.stage)
        assertEquals(VexDeckPage.Music, plan.preferredPage)
        assertEquals(VexContextDockReveal.Peek, plan.reveal)
    }

    @Test
    fun navigatingInSilenceOffersTheJourney() {
        val plan = requireNotNull(vexStagePlan(VexLiveSet(navigation = journey())))

        assertEquals(VexStage.Map, plan.stage)
        assertEquals(VexDeckPage.Journey, plan.preferredPage)
    }

    @Test
    fun everyActiveNavigationPhaseIsAMapStageButAPreparedRouteIsNot() {
        listOf(
            VexNavigationSessionPhase.ACTIVE,
            VexNavigationSessionPhase.OFF_ROUTE,
            VexNavigationSessionPhase.LOCATION_HELD,
            VexNavigationSessionPhase.ARRIVED,
        ).forEach { phase ->
            assertEquals(phase.name, VexStage.Map, vexStagePlan(VexLiveSet(navigation = journey(phase)))?.stage)
        }

        val prepared = requireNotNull(vexStagePlan(VexLiveSet(navigation = journey(VexNavigationSessionPhase.READY))))
        assertEquals(VexStage.Home, prepared.stage)
        assertEquals(VexDeckPage.Journey, prepared.preferredPage)
    }

    @Test
    fun atHomePlayingMusicComesForwardAndOpens() {
        val plan = requireNotNull(vexStagePlan(VexLiveSet(nowPlaying = music(), family = familyWithLatest)))

        assertEquals(VexStage.Home, plan.stage)
        assertEquals(VexDeckPage.Music, plan.preferredPage)
        assertEquals(VexContextDockReveal.Open, plan.reveal)
    }

    @Test
    fun aPausedTrackIsCurrentButDoesNotOpenTheDeck() {
        val plan = requireNotNull(vexStagePlan(VexLiveSet(nowPlaying = music(playing = false))))

        assertEquals(VexDeckPage.Music, plan.preferredPage)
        assertEquals(VexContextDockReveal.Peek, plan.reveal)
    }

    @Test
    fun inSilenceTheLatestFamilyMessageComesForward() {
        val plan = requireNotNull(vexStagePlan(VexLiveSet(family = familyWithLatest)))

        assertEquals(VexDeckPage.Family, plan.preferredPage)
        assertEquals(VexContextDockReveal.Peek, plan.reveal)
    }

    @Test
    fun pageOrderNeverChangesWithLiveness() {
        val orders =
            listOf(
                VexLiveSet(),
                VexLiveSet(navigation = journey()),
                VexLiveSet(nowPlaying = music(), family = familyWithLatest),
            ).map { requireNotNull(vexStagePlan(it)).pages }

        orders.forEach { order ->
            assertEquals(listOf(VexDeckPage.Music, VexDeckPage.Journey, VexDeckPage.Family), order)
        }
    }

    @Test
    fun theDeckNeverRepeatsTheScreenInFrontOfTheHuman() {
        val onMusicScreen =
            requireNotNull(
                vexStagePlan(
                    VexLiveSet(nowPlaying = music()),
                    ownedHere = setOf(VexDeckPage.Music),
                ),
            )

        assertEquals(listOf(VexDeckPage.Journey, VexDeckPage.Family), onMusicScreen.pages)
        assertEquals(VexDeckPage.Journey, onMusicScreen.preferredPage)
        // Music is playing but not on the deck here, so nothing asks the deck to open over the screen.
        assertEquals(VexContextDockReveal.Peek, onMusicScreen.reveal)

        assertNull(vexStagePlan(VexLiveSet(), ownedHere = VexDeckPage.entries.toSet()))
    }

    @Test
    fun humanChoiceSticksWithinAStageAndANewStageRearrangesOnce() {
        val home = requireNotNull(vexStagePlan(VexLiveSet(nowPlaying = music())))
        val driving = requireNotNull(vexStagePlan(VexLiveSet(navigation = journey(), nowPlaying = music())))

        assertEquals(home, vexDeckRearrangement(arrangedFor = null, plan = home))
        assertNull(vexDeckRearrangement(arrangedFor = home.context, plan = home))
        assertEquals(driving, vexDeckRearrangement(arrangedFor = home.context, plan = driving))
        assertNull(vexDeckRearrangement(arrangedFor = driving.context, plan = driving))

        val settings = requireNotNull(vexStagePlan(VexLiveSet(nowPlaying = music()), onStageScreen = false))
        assertEquals(settings, vexDeckRearrangement(arrangedFor = home.context, plan = settings))
    }

    @Test
    fun offTheStageScreenTheDeckTucksAwayUnlessSomethingIsLive() {
        assertEquals(
            VexContextDockReveal.Tucked,
            vexStagePlan(VexLiveSet(family = familyWithLatest), onStageScreen = false)?.reveal,
        )
        assertEquals(
            VexContextDockReveal.Tucked,
            vexStagePlan(VexLiveSet(nowPlaying = music(playing = false)), onStageScreen = false)?.reveal,
        )
        assertEquals(
            VexContextDockReveal.Peek,
            vexStagePlan(VexLiveSet(nowPlaying = music()), onStageScreen = false)?.reveal,
        )
        assertEquals(
            VexContextDockReveal.Peek,
            vexStagePlan(VexLiveSet(navigation = journey()), onStageScreen = false)?.reveal,
        )
    }

    @Test
    fun onlyPlayingMusicAndAnActiveJourneyAreMarkedLive() {
        val live = VexLiveSet(navigation = journey(), nowPlaying = music(), family = familyWithLatest)

        assertEquals(
            listOf(true, true, false),
            VexDeckPage.entries.map(live::isLive),
        )
        assertEquals(
            listOf(false, false, false),
            VexDeckPage.entries.map(VexLiveSet(nowPlaying = music(playing = false))::isLive),
        )
    }
}
