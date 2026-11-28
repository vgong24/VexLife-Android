package com.vextreme.vexlife.android.shell

import com.vextreme.vexlife.android.conversation.VexConversationGlance
import com.vextreme.vexlife.android.conversation.VexConversationHomeAvailability
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockPageRef
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockReveal
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationGlance
import com.vextreme.vexlife.android.screens.shared.maps.VexNavigationSessionPhase
import com.vextreme.vexlife.android.vexstream.VexStreamNowPlaying

/**
 * What deserves the base layer of the app right now.
 *
 * The stage is a projection of independently owned truths, never a fourth owner: it is recomputed
 * from the glances on every change and remembers nothing itself.
 */
internal enum class VexStage {
    /** A journey is under way: the route and the road ahead are the foreground. */
    Map,

    /** Nothing is moving: Vex and quick starts, with the deck carrying what is playing or said. */
    Home,
}

/**
 * The convergence deck's pages. Order is deliberately constant (Music, Journey, Family) so a swipe
 * always means the same thing; liveness changes which page is preferred, never where pages sit.
 */
internal enum class VexDeckPage(
    val pageRef: VexContextDockPageRef,
) {
    Music(VexContextDockPageRef("deck.music")),
    Journey(VexContextDockPageRef("deck.journey")),
    Family(VexContextDockPageRef("deck.family")),
    ;

    companion object {
        fun of(
            pageRef: VexContextDockPageRef?,
        ): VexDeckPage? = entries.firstOrNull { it.pageRef == pageRef }
    }
}

/** Every glance the stage reads, gathered once. */
internal data class VexLiveSet(
    val navigation: VexNavigationGlance? = null,
    val nowPlaying: VexStreamNowPlaying? = null,
    val family: VexConversationGlance = VexConversationGlance.Unobserved,
) {
    /** Started and not abandoned. A prepared-but-unstarted route (READY) is not yet a journey. */
    val navigating: Boolean
        get() =
            navigation != null &&
                navigation.phase != VexNavigationSessionPhase.READY

    val routePrepared: Boolean
        get() = navigation?.phase == VexNavigationSessionPhase.READY

    /** Sound is (or is about to be) coming out. A paused track is current but not live. */
    val listening: Boolean
        get() = nowPlaying?.let { it.playRequested && !it.ended } == true

    val familyHasLatest: Boolean
        get() =
            family.homeAvailability == VexConversationHomeAvailability.Available &&
                family.latestContent != null

    fun isLive(
        page: VexDeckPage,
    ): Boolean =
        when (page) {
            VexDeckPage.Music -> listening
            VexDeckPage.Journey -> navigating
            VexDeckPage.Family -> false
        }
}

/**
 * The arrangement a stage asks for: which pages the deck carries, which one comes forward, and how
 * far it is revealed when the stage begins.
 */
internal data class VexStagePlan(
    val stage: VexStage,
    val pages: List<VexDeckPage>,
    val preferredPage: VexDeckPage,
    val reveal: VexContextDockReveal,
    val onStageScreen: Boolean = true,
) {
    /** The situation a human choice belongs to; a new situation re-arranges the deck once. */
    val context: VexDeckContext
        get() = VexDeckContext(stage, onStageScreen)
}

internal data class VexDeckContext(
    val stage: VexStage,
    val onStageScreen: Boolean,
)

/**
 * Pure stage law.
 *
 * - Navigating -> [VexStage.Map]. The deck starts at Peek so the road stays visible, offering what
 *   is playing first (the map already shows the route), or the journey when nothing plays.
 * - Otherwise -> [VexStage.Home]. Music comes forward and opens when it is playing; a prepared
 *   route or the latest family message comes forward when nothing plays.
 *
 * [ownedHere] removes pages whose owner is the current Screen (the Music page on the Music screen),
 * so the deck never repeats the thing in front of the human. Returns null when no page remains.
 *
 * Off the stage screen ([onStageScreen] false, for example Settings) the deck stays out of the way:
 * tucked to a handle when nothing is live, at Peek when something is.
 */
internal fun vexStagePlan(
    live: VexLiveSet,
    ownedHere: Set<VexDeckPage> = emptySet(),
    onStageScreen: Boolean = true,
): VexStagePlan? {
    val pages = VexDeckPage.entries.filterNot { it in ownedHere }
    if (pages.isEmpty()) {
        return null
    }

    fun firstAvailable(
        vararg wanted: VexDeckPage?,
    ): VexDeckPage = wanted.filterNotNull().firstOrNull { it in pages } ?: pages.first()

    val plan = if (live.navigating) {
        VexStagePlan(
            stage = VexStage.Map,
            pages = pages,
            preferredPage =
                firstAvailable(
                    VexDeckPage.Music.takeIf { live.nowPlaying != null },
                    VexDeckPage.Journey,
                ),
            reveal = VexContextDockReveal.Peek,
        )
    } else {
        VexStagePlan(
            stage = VexStage.Home,
            pages = pages,
            preferredPage =
                firstAvailable(
                    VexDeckPage.Music.takeIf { live.nowPlaying != null },
                    VexDeckPage.Journey.takeIf { live.routePrepared },
                    VexDeckPage.Family.takeIf { live.familyHasLatest },
                    VexDeckPage.Music,
                ),
            reveal =
                if (live.listening && VexDeckPage.Music in pages) {
                    VexContextDockReveal.Open
                } else {
                    VexContextDockReveal.Peek
                },
        )
    }

    if (onStageScreen) {
        return plan
    }
    val anythingLive = live.navigating || live.listening
    return plan.copy(
        reveal = if (anythingLive) VexContextDockReveal.Peek else VexContextDockReveal.Tucked,
        onStageScreen = false,
    )
}

/**
 * Human choices stick within a situation; a new situation (stage, or moving on/off the stage
 * screen) re-arranges once.
 *
 * Returns the plan to apply now, or null when the deck should keep what the human chose. A host
 * calls this whenever the plan changes and records [VexStagePlan.context] after applying.
 */
internal fun vexDeckRearrangement(
    arrangedFor: VexDeckContext?,
    plan: VexStagePlan,
): VexStagePlan? = plan.takeIf { arrangedFor != plan.context }

// [VXG RealForever][SHELL][STAGE]
