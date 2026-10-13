package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.shell.AppShell

object VexStreamJourneys : VexJourneyShard {
    override val shardId = "shared.vexstream"

    private val pairedHubUnavailable =
        journeySignal(
            signalRef = "signal.vexstream.paired-hub-unavailable",
        ) { repositories ->
            val vexStream =
                repositories.vexStream as?
                    InMemoryVexStreamMusicRepository
                    ?: error(
                        "VexStream journey requires InMemoryVexStreamMusicRepository",
                    )
            vexStream.publishHubUnavailable(
                VexStreamHubState.Unreachable,
            )
        }

    val connectRefreshForget = vexJourney(
        id = "connect-refresh-forget",
        title = "VexStream connect, library-first controls, and confirmed forget",
    ) {
        start(HomeScreen)
        tap(HomeScreen.ConnectVexStreamMusic)
        tap(VexStreamMusicScreen.FindNearby)
        tap(VexStreamMusicScreen.ConfirmPairingCode)
        tap(AppShell.Elements.Back)
        tap(HomeScreen.VexStreamMusic)
        tap(VexStreamMusicScreen.ManageConnection)
        tap(VexStreamMusicScreen.RefreshCatalog)
        tap(VexStreamMusicScreen.CloseConnectionMenu)

        // Direct song-cell grammar: body is Play; the trailing menu owns secondary actions.
        tap(VexStreamMusicScreen.PlayTrack)
        tap(VexStreamMusicScreen.ShowQueue)
        tap(VexStreamMusicScreen.QueuePlay)
        tap(VexStreamMusicScreen.CloseQueue)

        tap(VexStreamMusicScreen.TrackMenu)
        tap(VexStreamMusicScreen.PlayTrackNext)
        tap(VexStreamMusicScreen.TrackMenu)
        tap(VexStreamMusicScreen.AddTrackToQueue)

        // The one-song fixture now has multiple queue entries, so transport controls are meaningful.
        tap(VexStreamMusicScreen.NextTrack)
        tap(VexStreamMusicScreen.PreviousTrack)
        tap(VexStreamMusicScreen.TogglePlayback)
        tap(VexStreamMusicScreen.ShuffleQueue)

        tap(VexStreamMusicScreen.TrackMenu)
        tap(VexStreamMusicScreen.DownloadTrack)
        tap(VexStreamMusicScreen.TrackMenu)
        tap(VexStreamMusicScreen.RemoveTrackDownload)
        tap(VexStreamMusicScreen.TrackMenu)
        tap(VexStreamMusicScreen.VisitTrackArtist)
        tap(VexStreamMusicScreen.CollectionBack)
        tap(VexStreamMusicScreen.SongsTab)

        tap(VexStreamMusicScreen.PlayAll)
        tap(VexStreamMusicScreen.ShuffleAll)
        tap(VexStreamMusicScreen.ShowQueue)
        tap(VexStreamMusicScreen.QueueShuffle)
        tap(VexStreamMusicScreen.ToggleRepeat)
        tap(VexStreamMusicScreen.ClearQueue)
        tap(VexStreamMusicScreen.CloseQueue)

        tap(VexStreamMusicScreen.SearchLibrary)
        type(
            VexStreamMusicScreen.SearchQuery,
            "Journey",
        )
        tap(VexStreamMusicScreen.CloseSearch)
        tap(VexStreamMusicScreen.AlbumsTab)
        tap(VexStreamMusicScreen.ArtistsTab)
        tap(VexStreamMusicScreen.PlaylistsTab)
        tap(VexStreamMusicScreen.DownloadsTab)
        tap(VexStreamMusicScreen.SongsTab)

        signal(pairedHubUnavailable)
        tap(VexStreamMusicScreen.ManageConnection)
        tap(VexStreamMusicScreen.Reconnect)
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
