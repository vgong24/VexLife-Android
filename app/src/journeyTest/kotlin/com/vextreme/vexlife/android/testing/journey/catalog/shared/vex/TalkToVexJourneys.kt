package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.shell.AppShell

object TalkToVexJourneys : VexJourneyShard {
    override val shardId = "shared.talk-to-vex"

    /**
     * Before Vex is connected, Talk to Vex is an honest context lens: a person reaches it from Home,
     * sees Vex's status and the bounded glances Vex would receive, and returns.
     */
    val contextLens = vexJourney(
        id = "context-lens",
        title = "Talk to Vex context lens",
    ) {
        start(HomeScreen)
        tap(HomeScreen.TalkToVex)
        assertVisible(TalkToVexScreen.StatusSection)
        assertVisible(TalkToVexScreen.LensSection)
        tap(AppShell.Elements.Back)
    }

    override val journeys = listOf(
        contextLens,
    )
}
