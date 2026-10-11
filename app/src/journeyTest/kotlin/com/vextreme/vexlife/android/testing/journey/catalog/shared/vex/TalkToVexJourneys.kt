package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.shell.AppShell

object TalkToVexJourneys : VexJourneyShard {
    override val shardId = "shared.talk-to-vex"

    val placeholder = vexJourney(
        id = "placeholder",
            title = "Talk to Vex placeholder",
        ) {
            start(HomeScreen)
            tap(HomeScreen.TalkToVex)
            tap(TalkToVexScreen.PlaceholderAction)
            tap(AppShell.Elements.Back)
        }

    override val journeys = listOf(
        placeholder,
    )
}
