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

    private fun trackRowAction(
        action: ActionElement,
    ): VexDynamicElementRef =
        dynamicElement(
            elementId =
                VexStreamPresentationRefs.trackRowAction(
                    trackRef =
                        VexStreamJourneyFixtures.TrackOne,
                    action = action,
                ),
            actionTemplate =
                action,
        )

    private fun trackMenuAction(
        action: ActionElement,
    ): VexDynamicElementRef =
        dynamicElement(
            elementId =
                VexStreamPresentationRefs.trackMenuAction(
                    trackRef =
                        VexStreamJourneyFixtures.TrackOne,
                    action = action,
                ),
            actionTemplate =
                action,
        )

    private fun queueEntryAction(
        action: ActionElement,
    ): VexDynamicElementRef {
        val stableKey =
            VexStreamPresentationRefs.queueEntryKey(
                mediaId =
                    VexStreamJourneyFixtures
                        .TrackOne
                        .value,
                occurrence = 1,
            )

        return dynamicElement(
            elementId =
                VexStreamPresentationRefs.queueEntryAction(
                    stableKey = stableKey,
                    action = action,
                ),
            actionTemplate = action,
        )
    }

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
            trackRowAction(
                VexStreamMusicScreen.PlayTrack,
            ),
        )
        tap(VexStreamMusicScreen.ShowQueue)
        expectVisible(
            dynamicElement(
                VexStreamPresentationRefs.queueEntry(
                    VexStreamPresentationRefs.queueEntryKey(
                        mediaId =
                            VexStreamJourneyFixtures
                                .TrackOne
                                .value,
                        occurrence = 1,
                    ),
                ),
            ),
        )
        tapDynamic(
            queueEntryAction(
                VexStreamMusicScreen.QueuePlay,
            ),
        )
        tap(VexStreamMusicScreen.CloseQueue)

        tapDynamic(
            trackRowAction(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapDynamic(
            trackMenuAction(
                VexStreamMusicScreen.PlayTrackNext,
            ),
        )
        tapDynamic(
            trackRowAction(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapDynamic(
            trackMenuAction(
                VexStreamMusicScreen.AddTrackToQueue,
            ),
        )

        // The one-song fixture now has multiple queue entries, so transport controls are meaningful.
        tap(VexStreamMusicScreen.NextTrack)
        tap(VexStreamMusicScreen.PreviousTrack)
        tap(VexStreamMusicScreen.TogglePlayback)
        tap(VexStreamMusicScreen.ShuffleQueue)

        tapDynamic(
            trackRowAction(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapDynamic(
            trackMenuAction(
                VexStreamMusicScreen.DownloadTrack,
            ),
        )
        tapDynamic(
            trackRowAction(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapDynamic(
            trackMenuAction(
                VexStreamMusicScreen.RemoveTrackDownload,
            ),
        )
        tapDynamic(
            trackRowAction(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapDynamic(
            trackMenuAction(
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
