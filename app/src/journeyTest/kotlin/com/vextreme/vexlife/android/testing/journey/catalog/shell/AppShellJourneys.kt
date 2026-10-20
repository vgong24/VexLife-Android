package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.shell.AppShell

object AppShellJourneys : VexJourneyShard {
    override val shardId = "shell"

    val primaryPeersNativeBack = vexJourney(
        id = "primary-peers-native-back",
        title = "Primary peers and native root Back",
        environments = listOf(
            VexJourneyEnvironments.Baseline,
            VexJourneyEnvironments.LargeText,
            VexJourneyEnvironments.AlternateLocaleFallback,
        ),
    ) {
        start(HomeScreen)
        tap(AppShell.Elements.Events)
        tap(AppShell.Elements.Home)
        tap(AppShell.Elements.Settings)
        back()
    }

    override val journeys = listOf(
        primaryPeersNativeBack,
    )
}
