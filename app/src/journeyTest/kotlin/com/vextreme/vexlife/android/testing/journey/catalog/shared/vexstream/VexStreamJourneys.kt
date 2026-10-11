package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.shell.AppShell

object VexStreamJourneys : VexJourneyShard {
    override val shardId = "shared.vexstream"

    val connectRefreshForget = vexJourney(
        id = "connect-refresh-forget",
        title = "VexStream connect, refresh, and forget",
    ) {
        start(HomeScreen)
        tap(HomeScreen.VexStreamMusic)
        tap(VexStreamMusicScreen.FindNearby)
        tap(VexStreamMusicScreen.ConfirmPairingCode)
        tap(VexStreamMusicScreen.RefreshCatalog)
        tap(VexStreamMusicScreen.ForgetPaired)
        tap(AppShell.Elements.Back)
    }

    override val journeys = listOf(
        connectRefreshForget,
    )
}
