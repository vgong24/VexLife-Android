package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamPresentationRefs
import com.vextreme.vexlife.android.shell.AppShell

object VexStreamJourneys : VexJourneyShard {
    override val shardId = "shared.vexstream"

    private fun trackAction(
        action: ActionElement,
    ): VexDynamicElementRef =
        dynamicElement(
            elementId =
                VexStreamPresentationRefs.trackAction(
                    trackRef =
                        VexStreamJourneyFixtures.TrackOne,
                    action = action,
                ),
            actionTemplate =
                action,
        )

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

        expectVisible(
            dynamicElement(
                VexStreamPresentationRefs.trackRow(
                    VexStreamJourneyFixtures.TrackOne,
                ),
            ),
        )

        tap(VexStreamMusicScreen.ManageConnection)
        tap(VexStreamMusicScreen.RefreshCatalog)
        tap(VexStreamMusicScreen.CloseConnectionMenu)

        // Direct song-cell grammar: body is Play; the trailing menu owns secondary actions.
        tapDynamic(
            trackAction(
                VexStreamMusicScreen.PlayTrack,
            ),
        )
        tap(VexStreamMusicScreen.ShowQueue)
        tap(VexStreamMusicScreen.QueuePlay)
        tap(VexStreamMusicScreen.CloseQueue)

        tapDynamic(
            trackAction(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapDynamic(
            trackAction(
                VexStreamMusicScreen.PlayTrackNext,
            ),
        )
        tapDynamic(
            trackAction(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapDynamic(
            trackAction(
                VexStreamMusicScreen.AddTrackToQueue,
            ),
        )

        // The one-song fixture now has multiple queue entries, so transport controls are meaningful.
        tap(VexStreamMusicScreen.NextTrack)
        tap(VexStreamMusicScreen.PreviousTrack)
        tap(VexStreamMusicScreen.TogglePlayback)
        tap(VexStreamMusicScreen.ShuffleQueue)

        tapDynamic(
            trackAction(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapDynamic(
            trackAction(
                VexStreamMusicScreen.DownloadTrack,
            ),
        )
        tapDynamic(
            trackAction(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapDynamic(
            trackAction(
                VexStreamMusicScreen.RemoveTrackDownload,
            ),
        )
        tapDynamic(
            trackAction(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapDynamic(
            trackAction(
                VexStreamMusicScreen.VisitTrackArtist,
            ),
        )
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
