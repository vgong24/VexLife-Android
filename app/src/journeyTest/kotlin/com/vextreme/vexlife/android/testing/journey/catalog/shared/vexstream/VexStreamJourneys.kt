package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubCatalogProjection
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubTrackProjection
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamPresentationRefs
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamTrackPresentationScope
import com.vextreme.vexlife.android.shell.AppShell
import com.vextreme.vexlife.android.testing.featurewalk.FeatureWalks
import com.vextreme.vexlife.android.testing.featurewalk.vexFeatureWalkJourney

object VexStreamJourneys : VexJourneyShard {
    override val shardId = "shared.vexstream"

    private fun trackRowActionElementId(
        action: ActionElement,
        scope: VexStreamTrackPresentationScope =
            VexStreamTrackPresentationScope.Songs,
    ): String =
        VexStreamPresentationRefs.trackRowAction(
            scope = scope,
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

    private fun collectionOpenElementId(
        collectionKey: String,
    ): String =
        VexStreamPresentationRefs.collectionRowAction(
            collectionKey = collectionKey,
            action = VexStreamMusicScreen.OpenCollection,
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
            elementId =
                trackRowActionElementId(
                    VexStreamMusicScreen.PlayTrack,
                ),
            behavesLike =
                VexStreamMusicScreen.PlayTrack,
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
            elementId =
                queueEntryActionElementId(
                    VexStreamMusicScreen.QueuePlay,
                ),
            behavesLike =
                VexStreamMusicScreen.QueuePlay,
        )
        tap(VexStreamMusicScreen.CloseQueue)

        tapElementById(
            elementId =
                trackRowActionElementId(
                    VexStreamMusicScreen.TrackMenu,
                ),
            behavesLike =
                VexStreamMusicScreen.TrackMenu,
        )
        tapElementById(
            elementId =
                trackMenuActionElementId(
                    VexStreamMusicScreen.PlayTrackNext,
                ),
            behavesLike =
                VexStreamMusicScreen.PlayTrackNext,
        )
        tapElementById(
            elementId =
                trackRowActionElementId(
                    VexStreamMusicScreen.TrackMenu,
                ),
            behavesLike =
                VexStreamMusicScreen.TrackMenu,
        )
        tapElementById(
            elementId =
                trackMenuActionElementId(
                    VexStreamMusicScreen.AddTrackToQueue,
                ),
            behavesLike =
                VexStreamMusicScreen.AddTrackToQueue,
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
            elementId =
                trackRowActionElementId(
                    VexStreamMusicScreen.TrackMenu,
                ),
            behavesLike =
                VexStreamMusicScreen.TrackMenu,
        )
        tapElementById(
            elementId =
                trackMenuActionElementId(
                    VexStreamMusicScreen.DownloadTrack,
                ),
            behavesLike =
                VexStreamMusicScreen.DownloadTrack,
        )
        tapElementById(
            elementId =
                trackRowActionElementId(
                    VexStreamMusicScreen.TrackMenu,
                ),
            behavesLike =
                VexStreamMusicScreen.TrackMenu,
        )
        tapElementById(
            elementId =
                trackMenuActionElementId(
                    VexStreamMusicScreen.RemoveTrackDownload,
                ),
            behavesLike =
                VexStreamMusicScreen.RemoveTrackDownload,
        )
        tapElementById(
            elementId =
                trackRowActionElementId(
                    VexStreamMusicScreen.TrackMenu,
                ),
            behavesLike =
                VexStreamMusicScreen.TrackMenu,
        )
        tapElementById(
            elementId =
                trackMenuActionElementId(
                    VexStreamMusicScreen.VisitTrackArtist,
                ),
            behavesLike =
                VexStreamMusicScreen.VisitTrackArtist,
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

    private val downloadAndRemoveFeatureWalk =
        vexFeatureWalkJourney(
            featureWalkCase =
                FeatureWalks
                    .vexStream
                    .offlineDownload
                    .downloadAndRemove,
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
        tapElementById(
            elementId =
                trackRowActionElementId(
                    VexStreamMusicScreen.DownloadTrack,
                ),
            behavesLike =
                VexStreamMusicScreen.DownloadTrack,
        )

        tap(VexStreamMusicScreen.DownloadsTab)
        assertVisibleById(
            VexStreamPresentationRefs.trackRow(
                scope =
                    VexStreamTrackPresentationScope.Downloads,
                trackRef =
                    VexStreamJourneyFixtures.TrackOne,
            ),
        )
        tapElementById(
            elementId =
                trackRowActionElementId(
                    action =
                        VexStreamMusicScreen.RemoveTrackDownload,
                    scope =
                        VexStreamTrackPresentationScope.Downloads,
                ),
            behavesLike =
                VexStreamMusicScreen.RemoveTrackDownload,
        )

        tap(VexStreamMusicScreen.SongsTab)
        assertVisibleById(
            VexStreamPresentationRefs.trackRow(
                scope =
                    VexStreamTrackPresentationScope.Songs,
                trackRef =
                    VexStreamJourneyFixtures.TrackOne,
            ),
        )
        tap(AppShell.Elements.Back)
    }

    private val downloadCollectionMissingFeatureWalk =
        vexFeatureWalkJourney(
            featureWalkCase =
                FeatureWalks
                    .vexStream
                    .offlineDownload
                    .downloadCollectionMissing,
        ) {
        start(HomeScreen)
        tap(HomeScreen.ConnectVexStreamMusic)
        tap(VexStreamMusicScreen.FindNearby)
        tap(VexStreamMusicScreen.ConfirmPairingCode)
        tap(AppShell.Elements.Back)
        tap(HomeScreen.VexStreamMusic)

        // Form the partial-offline checkpoint using the ordinary single-track product action first.
        tapElementById(
            elementId =
                trackRowActionElementId(
                    VexStreamMusicScreen.DownloadTrack,
                ),
            behavesLike =
                VexStreamMusicScreen.DownloadTrack,
        )

        // The producer catalog then grows the same album. TrackOne's verified local bytes are
        // preserved by media identity while TrackTwo/TrackThree arrive as Hub-only missing media.
        externalCause(
            repository =
                getRepository(
                    vexStreamJourneyRepository,
                ),
            causeRef =
                "cause.vexstream.mass-download.catalog-expanded",
        ) {
            check(
                state.value
                    .track(
                        VexStreamJourneyFixtures.TrackOne,
                    )
                    ?.localCopy is
                    VexStreamLocalCopyState.Downloaded,
            ) {
                "MassDownload Journey requires TrackOne to be locally verified before catalog expansion"
            }

            publishCatalog(
                massDownloadCatalog(),
            )
        }

        tap(VexStreamMusicScreen.AlbumsTab)
        tapElementById(
            elementId =
                collectionOpenElementId(
                    "album:" +
                        VexStreamJourneyFixtures.AlbumName,
                ),
            behavesLike =
                VexStreamMusicScreen.OpenCollection,
        )
        assertVisibleById(
            VexStreamPresentationRefs.collectionOfflineStatus(
                "album:" +
                VexStreamJourneyFixtures.AlbumName,
            ),
        )
        assertVisible(
            VexStreamMusicScreen.DownloadMissing,
        )
        tap(
            VexStreamMusicScreen.DownloadMissing,
        )

        tap(
            VexStreamMusicScreen.CollectionBack,
        )
        tap(
            VexStreamMusicScreen.DownloadsTab,
        )

        listOf(
            VexStreamJourneyFixtures.TrackOne,
            VexStreamJourneyFixtures.TrackTwo,
            VexStreamJourneyFixtures.TrackThree,
        ).forEach { trackRef ->
            assertVisibleById(
                VexStreamPresentationRefs.trackRow(
                    scope =
                        VexStreamTrackPresentationScope.Downloads,
                    trackRef = trackRef,
                ),
            )
        }

        tap(AppShell.Elements.Back)
    }

    private val sortFilterPlaybackFeatureWalk =
        vexFeatureWalkJourney(
            featureWalkCase =
                FeatureWalks
                    .vexStream
                    .libraryOrganization
                    .sortFilterPlayback,
        ) {
        start(HomeScreen)
        tap(HomeScreen.ConnectVexStreamMusic)
        tap(VexStreamMusicScreen.FindNearby)
        tap(VexStreamMusicScreen.ConfirmPairingCode)
        tap(AppShell.Elements.Back)
        tap(HomeScreen.VexStreamMusic)

        externalCause(
            repository =
                getRepository(
                    vexStreamJourneyRepository,
                ),
            causeRef =
                "cause.vexstream.library-organization.catalog",
        ) {
            publishCatalog(
                organizationCatalog(),
            )
        }

        assertVisibleById(
            VexStreamPresentationRefs.trackRow(
                scope =
                    VexStreamTrackPresentationScope.Songs,
                trackRef =
                    VexStreamJourneyFixtures.TrackOne,
            ),
        )
        assertVisibleById(
            VexStreamPresentationRefs.trackRow(
                scope =
                    VexStreamTrackPresentationScope.Songs,
                trackRef =
                    VexStreamJourneyFixtures
                        .OrganizationLongTrack,
            ),
        )

        tap(
            VexStreamMusicScreen.OrganizeLibrary,
        )

        // Exercise every static organization Control at least once while the accepted
        // semantic coverage contract keeps the Journey catalog aligned with the Screen blueprint.
        tap(
            VexStreamMusicScreen.SortLibraryByArtist,
        )
        tap(
            VexStreamMusicScreen.SortLibraryByAlbum,
        )
        tap(
            VexStreamMusicScreen.SortLibraryByYear,
        )
        tap(
            VexStreamMusicScreen.SortLibraryByDuration,
        )
        tap(
            VexStreamMusicScreen.SortLibraryByTitle,
        )
        tap(
            VexStreamMusicScreen.DurationFilterGreaterThan,
        )
        tap(
            VexStreamMusicScreen.DurationFilterAny,
        )

        tapElementById(
            elementId =
                VexStreamPresentationRefs.organizationGenreOption(
                    "Pop",
                ),
            behavesLike =
                VexStreamMusicScreen.ToggleLibraryFilter,
        )

        tap(
            VexStreamMusicScreen.DurationFilterLessThan,
        )
        enterText(
            VexStreamMusicScreen.DurationThresholdMinutes,
            "10",
        )
        tap(
            VexStreamMusicScreen.CloseOrganization,
        )

        assertVisibleById(
            VexStreamPresentationRefs.trackRow(
                scope =
                    VexStreamTrackPresentationScope.Songs,
                trackRef =
                    VexStreamJourneyFixtures.TrackOne,
            ),
        )
        tapElementById(
            elementId =
                trackRowActionElementId(
                    VexStreamMusicScreen.PlayTrack,
                ),
            behavesLike =
                VexStreamMusicScreen.PlayTrack,
        )
        tap(
            VexStreamMusicScreen.ShuffleAll,
        )

        tap(
            VexStreamMusicScreen.OrganizeLibrary,
        )
        tap(
            VexStreamMusicScreen.ClearLibraryFilters,
        )
        tap(
            VexStreamMusicScreen.CloseOrganization,
        )
        assertVisibleById(
            VexStreamPresentationRefs.trackRow(
                scope =
                    VexStreamTrackPresentationScope.Songs,
                trackRef =
                    VexStreamJourneyFixtures
                        .OrganizationLongTrack,
            ),
        )

        tap(AppShell.Elements.Back)
    }

    val downloadCollectionMissing =
        downloadCollectionMissingFeatureWalk.journey

    val downloadAndRemove =
        downloadAndRemoveFeatureWalk.journey

    val sortFilterPlayback =
        sortFilterPlaybackFeatureWalk.journey

    override val journeys = listOf(
        connectRefreshForget,
        downloadAndRemove,
        downloadCollectionMissing,
        sortFilterPlayback,
    )

    override val featureWalkJourneys =
        listOf(
            downloadAndRemoveFeatureWalk,
            downloadCollectionMissingFeatureWalk,
            sortFilterPlaybackFeatureWalk,
        )

    private fun organizationCatalog(): VexStreamHubCatalogProjection =
        VexStreamHubCatalogProjection(
            hubRef = "hub.journey.library-organization",
            sourceVersion = "journey",
            catalogRevision =
                "catalog.journey.library-organization.1",
            tracks =
                listOf(
                    VexStreamHubTrackProjection(
                        trackRef =
                            VexStreamJourneyFixtures.TrackOne,
                        mediaSha256 =
                            VexStreamMediaSha256(
                                "1".repeat(64),
                            ),
                        mediaByteLength = 4_096L,
                        title = "Journey Song",
                        artist = "Journey Artist",
                        album = "Journey Organization",
                        genre = "Pop",
                        year = "2026",
                        durationSeconds = 8 * 60.0,
                    ),
                    VexStreamHubTrackProjection(
                        trackRef =
                            VexStreamJourneyFixtures
                                .OrganizationLongTrack,
                        mediaSha256 =
                            VexStreamMediaSha256(
                                "4".repeat(64),
                            ),
                        mediaByteLength = 8_192L,
                        title = "Journey Long Mix",
                        artist = "Journey Artist",
                        album = "Journey Organization",
                        genre = "Mix",
                        year = "2026",
                        durationSeconds = 90 * 60.0,
                    ),
                ),
        )

    private fun massDownloadCatalog(): VexStreamHubCatalogProjection =
        VexStreamHubCatalogProjection(
            hubRef = "hub.journey.mass-download",
            sourceVersion = "journey",
            catalogRevision = "catalog.journey.mass-download.1",
            tracks =
                listOf(
                    VexStreamHubTrackProjection(
                        trackRef = VexStreamJourneyFixtures.TrackOne,
                        mediaSha256 =
                            VexStreamMediaSha256(
                                "1".repeat(64),
                            ),
                        mediaByteLength = 4_096L,
                        title = "Journey Song",
                        artist = "Journey Artist",
                        album = VexStreamJourneyFixtures.AlbumName,
                        genre = "Journey",
                        durationSeconds = 180.0,
                    ),
                    VexStreamHubTrackProjection(
                        trackRef = VexStreamJourneyFixtures.TrackTwo,
                        mediaSha256 =
                            VexStreamMediaSha256(
                                "2".repeat(64),
                            ),
                        mediaByteLength = 4_097L,
                        title = "Journey Song Two",
                        artist = "Journey Artist",
                        album = VexStreamJourneyFixtures.AlbumName,
                        genre = "Journey",
                        durationSeconds = 181.0,
                    ),
                    VexStreamHubTrackProjection(
                        trackRef = VexStreamJourneyFixtures.TrackThree,
                        mediaSha256 =
                            VexStreamMediaSha256(
                                "3".repeat(64),
                            ),
                        mediaByteLength = 4_098L,
                        title = "Journey Song Three",
                        artist = "Journey Artist",
                        album = VexStreamJourneyFixtures.AlbumName,
                        genre = "Journey",
                        durationSeconds = 182.0,
                    ),
                ),
        )
}
