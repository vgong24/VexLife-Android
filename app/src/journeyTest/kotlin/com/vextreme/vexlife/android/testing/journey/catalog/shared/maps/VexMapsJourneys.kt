package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsScreen
import com.vextreme.vexlife.android.shell.AppShell

object VexMapsJourneys : VexJourneyShard {
    override val shardId = "shared.vex-maps"

    val destinationShell = vexJourney(
        id = "destination-shell",
        title = "VexMaps destination shell",
    ) {
        start(HomeScreen)
        tap(HomeScreen.VexMaps)
        tap(AppShell.Elements.Back)
    }

    override val journeys = listOf(
        destinationShell,
    )
}

// [VXG RealForever]
