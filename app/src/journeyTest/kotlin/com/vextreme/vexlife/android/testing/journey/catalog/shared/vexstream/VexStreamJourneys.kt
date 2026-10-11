package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.shell.AppShell

object VexStreamJourneys : VexJourneyShard {
    override val shardId = "shared.vexstream"

    val connectRefreshForget = vexJourney(
        id = "connect-refresh-forget",
        title = "VexStream connect, library-first controls, and confirmed forget",
    ) {
        start(HomeScreen)
        tap(HomeScreen.ConnectVexStreamMusic)
        tap(VexStreamMusicScreen.FindNearby)
        tap(VexStreamMusicScreen.ConfirmPairingCode)
        tap(VexStreamMusicScreen.RefreshCatalog)
        type(
            VexStreamMusicScreen.SearchQuery,
            "Vex",
        )
        tap(VexStreamMusicScreen.AlbumsTab)
        tap(VexStreamMusicScreen.ArtistsTab)
        tap(VexStreamMusicScreen.PlaylistsTab)
        tap(VexStreamMusicScreen.DownloadsTab)
        tap(VexStreamMusicScreen.SongsTab)
        tap(VexStreamMusicScreen.ShowQueue)
        tap(VexStreamMusicScreen.CloseQueue)
        tap(VexStreamMusicScreen.ManageConnection)
        tap(VexStreamMusicScreen.RefreshCatalog)
        tap(VexStreamMusicScreen.CloseConnectionMenu)
        tap(VexStreamMusicScreen.ManageConnection)
        tap(VexStreamMusicScreen.RequestForgetPaired)
        tap(VexStreamMusicScreen.CancelForgetPaired)
        tap(VexStreamMusicScreen.RequestForgetPaired)
        tap(VexStreamMusicScreen.ForgetPaired)
        tap(AppShell.Elements.Back)
    }

    override val journeys = listOf(
        connectRefreshForget,
    )
}
