package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.shell.AppShell

object VexStreamJourneys : VexJourneyShard {
    override val shardId = "shared.vexstream"

    val placeholder = vexJourney(
        id = "placeholder",
            title = "VexStream Music placeholder",
        ) {
            start(HomeScreen)
            tap(HomeScreen.VexStreamMusic)
            tap(VexStreamMusicScreen.PlaceholderAction)
            tap(AppShell.Elements.Back)
        }

    override val journeys = listOf(
        placeholder,
    )
}
