package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.runtime.Composable
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamConnectionState
import com.vextreme.vexlife.android.data.vexstream.VexStreamHubState
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaSha256
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicRepository
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamPlaylist
import com.vextreme.vexlife.android.data.vexstream.VexStreamPlaylistRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrack
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexElementStyles
import com.vextreme.vexlife.android.elements.close
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.elements.localEffect
import com.vextreme.vexlife.android.elements.open
import com.vextreme.vexlife.android.elements.swapTo
import com.vextreme.vexlife.android.elements.then
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackArchitecture
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.runtime.appAction
import com.vextreme.vexlife.android.screens.AppScreenBlueprint
import com.vextreme.vexlife.android.screens.ScreenActionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One registered VexLife Screen, with feature-local library lenses.
 *
 * The static blueprint names only durable semantic surfaces. Songs/albums/artists remain dynamic
 * repository data; playback lifetime/queue belongs to MediaSessionService.
 */
object VexStreamMusicScreen : AppScreenBlueprint(
    screenKey = "vexstream-music",
    titleRes = R.string.vexstream_music_title,
) {
    val ConnectionSection = columnRef("body.connection")

    val SearchQuery = textFieldRef(
        id = "body.library.search",
        labelRes = R.string.vexstream_music_search,
    )

    val SongsPanel = scrollableColumnRef("body.library.songs")
    val AlbumsPanel = scrollableColumnRef("body.library.albums")
    val ArtistsPanel = scrollableColumnRef("body.library.artists")
    val PlaylistsPanel = scrollableColumnRef("body.library.playlists")
    val DownloadsPanel = scrollableColumnRef("body.library.downloads")

    val LibraryContent = swappableRef(
        id = "body.library",
        defaultChild = SongsPanel,
    )

    val PlayerSection = columnRef("body.player")

    val QueueSheet = modalBottomSheetRef(
        id = "body.queue.sheet",
    )

    val SearchSheet = modalBottomSheetRef(
        id = "body.library.search-sheet",
    )

    val ConnectionMenuSheet = modalBottomSheetRef(
        id = "body.connection.menu",
    )

    val ForgetConfirmationDialog = dialogRef(
        id = "body.connection.forget-confirmation",
    )

    val FindNearby = buttonRef(
        id = "body.connection.find",
        labelRes = R.string.vexstream_music_find,
        style = VexElementStyles.PrimaryAction,
        intent = invoke(AppActions.FindNearbyVexStream),
    )

    val ConfirmPairingCode = buttonRef(
        id = "body.connection.confirm-code",
        labelRes = R.string.vexstream_music_confirm_code,
        style = VexElementStyles.PrimaryAction,
        intent = invoke(AppActions.ConfirmVexStreamPairingCode),
    )

    val RefreshCatalog = buttonRef(
        id = "body.connection.refresh",
        labelRes = R.string.vexstream_music_refresh,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.RefreshVexStreamCatalog),
    )

    val SearchLibrary = buttonRef(
        id = "body.library.search.open",
        labelRes = R.string.vexstream_music_search_action,
        style = VexElementStyles.ToolbarIcon,
        icon = ElementIcon.Search,
        intent = open(SearchSheet),
    )

    val CloseSearch = buttonRef(
        id = "body.library.search.close",
        labelRes = R.string.close,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Close,
        intent = close(SearchSheet),
    )

    val ManageConnection = buttonRef(
        id = "body.connection.manage",
        labelRes = R.string.vexstream_music_manage_connection,
        style = VexElementStyles.ToolbarIcon,
        icon = ElementIcon.Settings,
        intent = open(ConnectionMenuSheet),
    )

    val Reconnect = buttonRef(
        id = "body.connection.reconnect",
        labelRes = R.string.vexstream_music_reconnect,
        style = VexElementStyles.SecondaryAction,
        intent = invoke(AppActions.ReconnectVexStream),
    )

    val RequestForgetPaired = buttonRef(
        id = "body.connection.forget.request",
        labelRes = R.string.vexstream_music_forget,
        style = VexElementStyles.DangerAction,
        intent = open(ForgetConfirmationDialog),
    )

    val ForgetPaired = buttonRef(
        id = "body.connection.forget",
        labelRes = R.string.vexstream_music_forget_confirm,
        style = VexElementStyles.DangerAction,
        intent =
            invoke(AppActions.ForgetPairedVexStream)
                .then(close(ForgetConfirmationDialog))
                .then(close(ConnectionMenuSheet)),
    )

    val CancelForgetPaired = buttonRef(
        id = "body.connection.forget.cancel",
        labelRes = R.string.vexstream_music_cancel,
        style = VexElementStyles.SecondaryAction,
        intent = close(ForgetConfirmationDialog),
    )

    val CloseConnectionMenu = buttonRef(
        id = "body.connection.menu.close",
        labelRes = R.string.close,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Close,
        intent = close(ConnectionMenuSheet),
    )

    val SongsTab = buttonRef(
        id = "body.library-tabs.songs",
        labelRes = R.string.vexstream_music_songs,
        style = VexElementStyles.Navigation,
        intent = swapTo(
            container = LibraryContent,
            child = SongsPanel,
        ),
    )

    val AlbumsTab = buttonRef(
        id = "body.library-tabs.albums",
        labelRes = R.string.vexstream_music_albums,
        style = VexElementStyles.Navigation,
        intent = swapTo(
            container = LibraryContent,
            child = AlbumsPanel,
        ),
    )

    val ArtistsTab = buttonRef(
        id = "body.library-tabs.artists",
        labelRes = R.string.vexstream_music_artists,
        style = VexElementStyles.Navigation,
        intent = swapTo(
            container = LibraryContent,
            child = ArtistsPanel,
        ),
    )

    val PlaylistsTab = buttonRef(
        id = "body.library-tabs.playlists",
        labelRes = R.string.vexstream_music_playlists,
        style = VexElementStyles.Navigation,
        intent = swapTo(
            container = LibraryContent,
            child = PlaylistsPanel,
        ),
    )

    val DownloadsTab = buttonRef(
        id = "body.library-tabs.downloads",
        labelRes = R.string.vexstream_music_downloads,
        style = VexElementStyles.Navigation,
        intent = swapTo(
            container = LibraryContent,
            child = DownloadsPanel,
        ),
    )

    val CollectionBack = buttonRef(
        id = "body.collection.back",
        labelRes = R.string.back,
        style = VexElementStyles.Navigation,
        icon = ElementIcon.Back,
        intent = localEffect(),
    )

    val TrackMenu = buttonRef(
        id = "body.track-actions.menu",
        labelRes = R.string.vexstream_music_track_menu,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Menu,
        intent = localEffect(),
    )

    val PlayTrack = controlButtonRef(
        id = "body.track-actions.play",
        labelRes = R.string.vexstream_music_play,
        control =
            VexStreamPlaybackArchitecture
                .controls
                .PlayTrack,
        style = VexElementStyles.PrimaryAction,
        icon = ElementIcon.Play,
    )

    val PlayTrackNext = buttonRef(
        id = "body.track-actions.play-next",
        labelRes = R.string.vexstream_music_play_next,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.PlayNext,
        intent = localEffect(),
    )

    val AddTrackToQueue = buttonRef(
        id = "body.track-actions.add-queue",
        labelRes = R.string.vexstream_music_add_queue,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Queue,
        intent = localEffect(),
    )

    val DownloadTrack = buttonRef(
        id = "body.track-actions.download",
        labelRes = R.string.vexstream_music_download,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Download,
        intent = localEffect(),
    )

    val RemoveTrackDownload = buttonRef(
        id = "body.track-actions.remove-download",
        labelRes = R.string.vexstream_music_remove_download,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.RemoveDownload,
        intent = localEffect(),
    )

    val VisitTrackArtist = buttonRef(
        id = "body.track-actions.visit-artist",
        labelRes = R.string.vexstream_music_visit_artist,
        style = VexElementStyles.Navigation,
        icon = ElementIcon.Person,
        intent = localEffect(),
    )

    val PlayAll = buttonRef(
        id = "body.library-actions.play-all",
        labelRes = R.string.vexstream_music_play_all,
        style = VexElementStyles.PrimaryAction,
        icon = ElementIcon.Play,
        intent = localEffect(),
    )

    val ShuffleAll = buttonRef(
        id = "body.library-actions.shuffle-all",
        labelRes = R.string.vexstream_music_shuffle_all,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Shuffle,
        intent = localEffect(),
    )

    val PreviousTrack = controlButtonRef(
        id = "body.player.previous",
        labelRes = R.string.vexstream_music_previous,
        control =
            VexStreamPlaybackArchitecture
                .controls
                .Previous,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Previous,
    )

    val TogglePlayback = controlButtonRef(
        id = "body.player.play-pause",
        labelRes = R.string.vexstream_music_play_pause,
        control =
            VexStreamPlaybackArchitecture
                .controls
                .PlayPause,
        style = VexElementStyles.PrimaryAction,
        icon = ElementIcon.PlayPause,
    )

    val NextTrack = controlButtonRef(
        id = "body.player.next",
        labelRes = R.string.vexstream_music_next,
        control =
            VexStreamPlaybackArchitecture
                .controls
                .Next,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Next,
    )

    val SeekPlayback = buttonRef(
        id = "body.player.seek",
        labelRes = R.string.vexstream_music_seek,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val ShuffleQueue = buttonRef(
        id = "body.player.shuffle",
        labelRes = R.string.vexstream_music_shuffle,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Shuffle,
        intent = localEffect(),
    )

    val QueueShuffle = buttonRef(
        id = "body.queue.sheet.shuffle",
        labelRes = R.string.vexstream_music_shuffle,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Shuffle,
        intent = localEffect(),
    )

    val ToggleRepeat = buttonRef(
        id = "body.queue.sheet.repeat",
        labelRes = R.string.vexstream_music_repeat,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Repeat,
        intent = localEffect(),
    )

    val ClearQueue = buttonRef(
        id = "body.queue.sheet.clear",
        labelRes = R.string.vexstream_music_clear_queue,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val QueuePlay = buttonRef(
        id = "body.queue.sheet.play",
        labelRes = R.string.vexstream_music_queue_play,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val QueueReorder = buttonRef(
        id = "body.queue.sheet.reorder",
        labelRes = R.string.vexstream_music_queue_reorder,
        style = VexElementStyles.SecondaryAction,
        intent = localEffect(),
    )

    val ShowQueue = buttonRef(
        id = "body.player.queue.show",
        labelRes = R.string.vexstream_music_queue,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Queue,
        intent = open(QueueSheet),
    )

    val CloseQueue = buttonRef(
        id = "body.queue.sheet.close",
        labelRes = R.string.close,
        style = VexElementStyles.SecondaryAction,
        icon = ElementIcon.Close,
        intent = close(QueueSheet),
    )

    override val content = layout {
        column("body") {
            column(
                element = ConnectionSection,
                style = VexElementStyles.RaisedSurface,
            ) {
                text(
                    id = "intro",
                    textRes = R.string.vexstream_music_intro,
                    role = TextRole.Supporting,
                )
                button(FindNearby)
                button(ConfirmPairingCode)
            }

            row("library-tabs") {
                button(SongsTab)
                button(AlbumsTab)
                button(ArtistsTab)
                button(PlaylistsTab)
                button(DownloadsTab)
            }

            swappable(LibraryContent) {
                scrollableColumn(SongsPanel) {}
                scrollableColumn(AlbumsPanel) {}
                scrollableColumn(ArtistsPanel) {}
                scrollableColumn(PlaylistsPanel) {}
                scrollableColumn(DownloadsPanel) {}
            }

            button(CollectionBack)

            column("track-actions") {
                button(TrackMenu)
                button(PlayTrack)
                button(PlayTrackNext)
                button(AddTrackToQueue)
                button(DownloadTrack)
                button(RemoveTrackDownload)
                button(VisitTrackArtist)
            }

            row("library-actions") {
                button(PlayAll)
                button(ShuffleAll)
                button(ShowQueue)
            }

            column(
                element = PlayerSection,
                style = VexElementStyles.RaisedSurface,
            ) {
                button(SeekPlayback)

                row("controls") {
                    button(PreviousTrack)
                    button(TogglePlayback)
                    button(NextTrack)
                    button(ShuffleQueue)
                }
            }

            modalBottomSheet(QueueSheet) {
                button(QueueShuffle)
                button(ToggleRepeat)
                button(ClearQueue)
                button(QueuePlay)
                button(QueueReorder)
                button(CloseQueue)
            }

            modalBottomSheet(SearchSheet) {
                textField(SearchQuery)
                button(CloseSearch)
            }

            modalBottomSheet(ConnectionMenuSheet) {
                button(RefreshCatalog)
                button(Reconnect)
                button(RequestForgetPaired)
                button(CloseConnectionMenu)
            }

            dialog(ForgetConfirmationDialog) {
                button(ForgetPaired)
                button(CancelForgetPaired)
            }
        }
    }

    internal override val toolbarActions
        get() =
            listOf(
                SearchLibrary,
                ManageConnection,
            )

    internal override fun appActionRegistrations(
        context: ScreenActionContext,
    ): AppActionRegistrations =
        vexStreamActionRegistrations(
            repository = context.repositories.vexStream,
            scope = context.scope,
        )

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
        darkTheme: Boolean,
    ) {
        VexStreamMusicScreenContent(
            runtime = runtime,
            repository = repositories.vexStream,
        )
    }
}

internal fun vexStreamActionRegistrations(
    repository: VexStreamMusicRepository,
    scope: CoroutineScope,
): AppActionRegistrations =
    AppActionRegistrations.of(
        appAction(
            action = AppActions.FindNearbyVexStream,
            ownerRef = VexStreamMusicScreen.screenId,
        ) {
            scope.launch {
                repository.findNearbyVexStream()
            }
        },
        appAction(
            action = AppActions.ReconnectVexStream,
            ownerRef = VexStreamMusicScreen.screenId,
        ) {
            scope.launch {
                repository.reconnectPairedVexStream()
            }
        },
        appAction(
            action = AppActions.ConfirmVexStreamPairingCode,
            ownerRef = VexStreamMusicScreen.screenId,
        ) {
            scope.launch {
                repository.confirmPairingCode()
            }
        },
        appAction(
            action = AppActions.RefreshVexStreamCatalog,
            ownerRef = VexStreamMusicScreen.screenId,
        ) {
            scope.launch {
                repository.refreshCatalog()
            }
        },
        appAction(
            action = AppActions.ForgetPairedVexStream,
            ownerRef = VexStreamMusicScreen.screenId,
        ) {
            scope.launch {
                repository.forgetPairedVexStream()
            }
        },
    )

@Composable
fun VexStreamMusicScreenContent(
    runtime: VexRuntime,
    repository: VexStreamMusicRepository,
) {
    VexStreamLibrarySurface(
        runtime = runtime,
        repository = repository,
    )
}

@VexScreenPreviews
@Composable
private fun VexStreamMusicScreenPreview() {
    VexPreviewHost(
        id = "vexstream-music",
        startScreen = VexStreamMusicScreen,
        repositoriesFactory = {
            com.vextreme.vexlife.android.preview.VexPreviewRepositories
                .empty()
                .copy(
                    vexStream =
                        InMemoryVexStreamMusicRepository(
                            initialState = previewState(),
                        ),
                )
        },
    ) { context ->
        VexStreamMusicScreenContent(
            runtime = context.runtime,
            repository = context.repositories.vexStream,
        )
    }
}

private fun previewState(): VexStreamMusicState {
    val streamed =
        VexStreamTrack(
            trackRef = VexStreamTrackRef("preview.streamed"),
            mediaSha256 =
                VexStreamMediaSha256(
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                ),
            mediaByteLength = 5_000_000,
            title = "VexStream Mobile",
            artist = "Vex",
            album = "Desktop Hub",
            durationSeconds = 205.0,
        )
    val downloaded =
        VexStreamTrack(
            trackRef = VexStreamTrackRef("preview.downloaded"),
            mediaSha256 =
                VexStreamMediaSha256(
                    "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                ),
            mediaByteLength = 4_000_000,
            title = "Take It With You",
            artist = "Vex",
            album = "Offline",
            durationSeconds = 184.0,
        )

    return VexStreamMusicState(
        hubState =
            VexStreamHubState.Ready(
                hubRef = "vexstream.preview",
                catalogRevision = "preview-1",
            ),
        connectionState =
            VexStreamConnectionState.Paired(
                displayName = "VexStream on Home PC",
            ),
        tracks =
            listOf(
                VexStreamTrackState(
                    track = streamed,
                    hubAvailable = true,
                ),
                VexStreamTrackState(
                    track = downloaded,
                    hubAvailable = true,
                    localCopy =
                        VexStreamLocalCopyState.Downloaded(
                            mediaSha256 = downloaded.mediaSha256,
                            byteLength = downloaded.mediaByteLength,
                            localMediaRef = "preview.local",
                        ),
                ),
            ),
        playlists =
            listOf(
                VexStreamPlaylist(
                    playlistRef =
                        VexStreamPlaylistRef(
                            "preview.playlist.mobile",
                        ),
                    name = "Road Trip",
                    trackRefs =
                        listOf(
                            streamed.trackRef,
                            downloaded.trackRef,
                        ),
                ),
            ),
    )
}

// [VXG RealForever]
