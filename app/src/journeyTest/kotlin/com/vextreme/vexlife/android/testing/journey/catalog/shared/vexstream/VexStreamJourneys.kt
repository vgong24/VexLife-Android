package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamPresentationRefs
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamTrackPresentationScope
import com.vextreme.vexlife.android.shell.AppShell

object VexStreamJourneys : VexJourneyShard {
    override val shardId = "shared.vexstream"

    private fun trackRowActionElementId(
        action: ActionElement,
    ): String =
        VexStreamPresentationRefs.trackRowAction(
            scope =
                VexStreamTrackPresentationScope.Songs,
            trackRef =
                VexStreamJourneyFixtures.TrackOne,
            action = action,
        )

    private fun trackMenuActionElementId(
        action: ActionElement,
    ): String =
        VexStreamPresentationRefs.trackMenuAction(
            scope =
                VexStreamTrackPresentationScope.Songs,
            trackRef =
                VexStreamJourneyFixtures.TrackOne,
            action = action,
        )

    private fun queueEntryActionElementId(
        action: ActionElement,
    ): String {
        val stableKey =
            VexStreamPresentationRefs.queueEntryKey(
                mediaId =
                    VexStreamJourneyFixtures
                        .TrackOne
                        .value,
                occurrence = 1,
            )

        return VexStreamPresentationRefs.queueEntryAction(
            stableKey = stableKey,
            action = action,
        )
    }

    private val vexStreamJourneyRepository =
        journeyRepository(
            repositoryRef =
                "repository.vexstream.journey",
        ) { repositories ->
            repositories.vexStream as?
                InMemoryVexStreamMusicRepository
                ?: error(
                    "VexStream journey requires InMemoryVexStreamMusicRepository",
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

        assertVisibleById(
            VexStreamPresentationRefs.trackRow(
                scope =
                    VexStreamTrackPresentationScope.Songs,
                trackRef =
                    VexStreamJourneyFixtures.TrackOne,
            ),
        )

        tap(VexStreamMusicScreen.ManageConnection)
        tap(VexStreamMusicScreen.RefreshCatalog)
        tap(VexStreamMusicScreen.CloseConnectionMenu)

        // Direct song-cell grammar: body is Play; the trailing menu owns secondary actions.
        tapElementById(
            trackRowActionElementId(
                VexStreamMusicScreen.PlayTrack,
            ),
        )
        tap(VexStreamMusicScreen.ShowQueue)
        assertVisibleById(
            VexStreamPresentationRefs.queueEntry(
                VexStreamPresentationRefs.queueEntryKey(
                    mediaId =
                        VexStreamJourneyFixtures
                            .TrackOne
                            .value,
                    occurrence = 1,
                ),
            ),
        )
        tapElementById(
            queueEntryActionElementId(
                VexStreamMusicScreen.QueuePlay,
            ),
        )
        tap(VexStreamMusicScreen.CloseQueue)

        tapElementById(
            trackRowActionElementId(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapElementById(
            trackMenuActionElementId(
                VexStreamMusicScreen.PlayTrackNext,
            ),
        )
        tapElementById(
            trackRowActionElementId(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapElementById(
            trackMenuActionElementId(
                VexStreamMusicScreen.AddTrackToQueue,
            ),
        )

        // The one-song fixture now has multiple queue entries, so transport controls are meaningful.
        tap(VexStreamMusicScreen.NextTrack)
        tap(VexStreamMusicScreen.PreviousTrack)
        tap(VexStreamMusicScreen.TogglePlayback)
        tap(VexStreamMusicScreen.ShuffleQueue)

        // Queue-sheet Shuffle asks the same one-shot Queue question. Exercise it while the
        // three-occurrence fixture still has two future items; the later one-song collection does
        // not truthfully make this Control available.
        tap(VexStreamMusicScreen.ShowQueue)
        tap(VexStreamMusicScreen.QueueShuffle)
        tap(VexStreamMusicScreen.CloseQueue)

        tapElementById(
            trackRowActionElementId(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapElementById(
            trackMenuActionElementId(
                VexStreamMusicScreen.DownloadTrack,
            ),
        )
        tapElementById(
            trackRowActionElementId(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapElementById(
            trackMenuActionElementId(
                VexStreamMusicScreen.RemoveTrackDownload,
            ),
        )
        tapElementById(
            trackRowActionElementId(
                VexStreamMusicScreen.TrackMenu,
            ),
        )
        tapElementById(
            trackMenuActionElementId(
                VexStreamMusicScreen.VisitTrackArtist,
            ),
        )
        tap(VexStreamMusicScreen.CollectionBack)
        tap(VexStreamMusicScreen.SongsTab)

        tap(VexStreamMusicScreen.PlayAll)
        tap(VexStreamMusicScreen.ShuffleAll)
        tap(VexStreamMusicScreen.ShowQueue)
        tap(VexStreamMusicScreen.ToggleRepeat)
        tap(VexStreamMusicScreen.ClearQueue)
        tap(VexStreamMusicScreen.CloseQueue)

        tap(VexStreamMusicScreen.SearchLibrary)
        assertVisible(
            VexStreamMusicScreen.SearchPrompt,
        )
        enterText(
            VexStreamMusicScreen.SearchQuery,
            "Journey",
        )
        assertVisibleById(
            VexStreamPresentationRefs.trackRow(
                scope =
                    VexStreamTrackPresentationScope.Search,
                trackRef =
                    VexStreamJourneyFixtures.TrackOne,
            ),
        )
        tap(VexStreamMusicScreen.CloseSearch)
        tap(VexStreamMusicScreen.AlbumsTab)
        tap(VexStreamMusicScreen.ArtistsTab)
        tap(VexStreamMusicScreen.PlaylistsTab)
        tap(VexStreamMusicScreen.DownloadsTab)
        tap(VexStreamMusicScreen.SongsTab)

        externalCause(
            repository =
                getRepository(
                    vexStreamJourneyRepository,
                ),
            causeRef =
                "cause.vexstream.paired-hub-unavailable",
        ) {
            publishHubUnavailable(
                VexStreamHubState.Unreachable,
            )
        }
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
