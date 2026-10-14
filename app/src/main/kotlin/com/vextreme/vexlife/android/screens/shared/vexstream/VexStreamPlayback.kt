package com.vextreme.vexlife.android.screens.shared.vexstream

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMediaStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackArchitecture
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackCase
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackObservation
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
import com.vextreme.vexlife.android.vexarchitecture.VexCurrent
import com.vextreme.vexlife.android.vexarchitecture.VexTransition
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Resolves product-level track availability into one Media3 item.
 *
 * Local verified bytes win. A Hub-backed track becomes an opaque vexstream:// item whose real
 * endpoint/token/certificate are resolved by VexStreamNetworkDataSource inside the playback service.
 */
internal class VexStreamPlaybackSourceResolver(
    context: Context,
) {
    private val mediaStore =
        VexStreamMediaStore.production(
            contextFilesDir = context.filesDir,
        )

    fun mediaItem(
        state: VexStreamTrackState,
    ): MediaItem? {
        val localCopy =
            state.localCopy as? VexStreamLocalCopyState.Downloaded

        val uri =
            localCopy
                ?.let(mediaStore::fileForPlayback)
                ?.let(Uri::fromFile)
                ?: if (state.hubAvailable) {
                    Uri.Builder()
                        .scheme("vexstream")
                        .authority("media")
                        .appendPath(
                            state.track.trackRef.value,
                        )
                        .build()
                } else {
                    return null
                }

        val clipping =
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(
                    (state.track.playbackStartSeconds * 1000.0)
                        .toLong(),
                )
                .apply {
                    state.track.playbackEndSeconds?.let { end ->
                        setEndPositionMs(
                            (end * 1000.0).toLong(),
                        )
                    }
                }
                .build()

        return MediaItem.Builder()
            .setMediaId(state.track.trackRef.value)
            .setUri(uri)
            .setMimeType(MimeTypes.AUDIO_MPEG)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(state.track.title)
                    .setArtist(state.track.artist)
                    .setAlbumTitle(state.track.album)
                    .build(),
            )
            .setClippingConfiguration(clipping)
            .build()
    }
}

internal data class VexStreamPlaybackUiState(
    /**
     * Current Play/Pause meaning observed from Media3.
     *
     * Presentation may read this relationship; it does not gain playback execution authority.
     */
    val currentPlayback: VexCurrent<out VexStreamPlaybackCase> =
        VexStreamPlaybackArchitecture
            .android
            .current(
                VexStreamPlaybackObservation(
                    queueHasItems = false,
                    playRequested = false,
                ),
            ),
    val currentMediaId: String? = null,
    val currentTitle: String = "",
    val currentArtist: String = "",
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queueMediaIds: List<String> = emptyList(),
    val currentIndex: Int = -1,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
) {
    val hasQueue: Boolean
        get() = queueMediaIds.isNotEmpty()

    val repeatEnabled: Boolean
        get() = repeatMode != Player.REPEAT_MODE_OFF

    /** Read-only transition currently represented by the shared Play/Pause control. */
    val availablePlayPauseTransition: VexTransition<*, *>?
        get() =
            currentPlayback.availableTransition(
                VexStreamPlaybackArchitecture
                    .controls
                    .PlayPause,
            )

    /**
     * Shared presentation projection for every Android surface showing this Control.
     *
     * Mini Player and Context Dock should not independently reinterpret the same semantic transition.
     */
    val playPauseIcon: ElementIcon
        get() =
            when (availablePlayPauseTransition) {
                VexStreamPlaybackArchitecture.controls.Pause ->
                    ElementIcon.Pause

                VexStreamPlaybackArchitecture.controls.Play ->
                    ElementIcon.Play

                else ->
                    ElementIcon.PlayPause
            }

    val playPauseEnabled: Boolean
        get() =
            availablePlayPauseTransition != null

    val progressFraction: Float
        get() =
            if (durationMs > 0L) {
                (currentPositionMs.toFloat() / durationMs.toFloat())
                    .coerceIn(0f, 1f)
            } else {
                0f
            }

    val bufferedFraction: Float
        get() =
            if (durationMs > 0L) {
                (bufferedPositionMs.toFloat() / durationMs.toFloat())
                    .coerceIn(0f, 1f)
            } else {
                0f
            }

    companion object {
        val Empty = VexStreamPlaybackUiState()
    }
}

/**
 * Activity/ViewModel-lifetime client for the service-owned VexStream player.
 *
 * The MediaSessionService remains the player/queue owner. Retaining only the UI-side controller here
 * prevents ordinary VexLife Screen navigation from disconnecting the last in-app controller when
 * VexStream leaves composition. The controller is released with the Activity's ViewModel store.
 */
internal class VexStreamPlaybackViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private var cleared = false

    private val controllerFuture =
        MediaController.Builder(
            application,
            SessionToken(
                application,
                ComponentName(
                    application,
                    VexStreamPlaybackService::class.java,
                ),
            ),
        ).buildAsync()

    var controller by mutableStateOf<MediaController?>(null)
        private set

    init {
        controllerFuture.addListener(
            {
                runCatching {
                    controllerFuture.get()
                }.onSuccess { connected ->
                    if (!cleared) {
                        controller = connected
                    }
                }
            },
            ContextCompat.getMainExecutor(application),
        )
    }

    override fun onCleared() {
        cleared = true
        controller = null
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}

/**
 * Projects the retained VexStream media client into Compose.
 *
 * This function does not own player lifetime and does not reconnect on every Screen composition.
 */
@Composable
internal fun rememberVexStreamMediaController(): MediaController? {
    val owner: VexStreamPlaybackViewModel = viewModel()
    return owner.controller
}

@Composable
internal fun rememberVexStreamPlaybackUiState(
    controller: MediaController?,
): State<VexStreamPlaybackUiState> {
    val projected =
        remember(controller) {
            mutableStateOf(
                controller?.toVexStreamUiState()
                    ?: VexStreamPlaybackUiState.Empty,
            )
        }

    DisposableEffect(controller) {
        if (controller == null) {
            projected.value = VexStreamPlaybackUiState.Empty
            return@DisposableEffect onDispose {}
        }

        val listener =
            object : Player.Listener {
                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) {
                    projected.value = player.toVexStreamUiState()
                }
            }

        controller.addListener(listener)
        projected.value = controller.toVexStreamUiState()

        onDispose {
            controller.removeListener(listener)
        }
    }

    LaunchedEffect(controller) {
        if (controller == null) {
            return@LaunchedEffect
        }

        while (true) {
            projected.value = controller.toVexStreamUiState()
            delay(
                if (controller.playWhenReady) {
                    250L
                } else {
                    1_000L
                },
            )
        }
    }

    return projected
}

internal fun MediaController.playVexStreamContext(
    selected: VexStreamTrackState,
    contextTracks: List<VexStreamTrackState>,
    resolver: VexStreamPlaybackSourceResolver,
): Boolean {
    val items =
        contextTracks
            .mapNotNull(resolver::mediaItem)

    if (items.isEmpty()) {
        return false
    }

    val selectedIndex =
        items.indexOfFirst {
            it.mediaId == selected.track.trackRef.value
        }
    if (selectedIndex < 0) {
        return false
    }

    setMediaItems(
        items,
        selectedIndex,
        0L,
    )
    shuffleModeEnabled = false
    prepare()
    play()
    return true
}

internal fun MediaController.playVexStreamCollection(
    tracks: List<VexStreamTrackState>,
    resolver: VexStreamPlaybackSourceResolver,
    shuffled: Boolean = false,
): Boolean {
    val items =
        tracks.mapNotNull(
            resolver::mediaItem,
        )
    if (items.isEmpty()) {
        return false
    }

    val order =
        if (shuffled) {
            vexStreamQueueShuffleTargetOrder(
                itemCount = items.size,
                currentIndex = -1,
            )
        } else {
            items.indices.toList()
        }
    val orderedItems =
        order.map(items::get)

    shuffleModeEnabled = false
    setMediaItems(orderedItems)
    prepare()
    play()
    return true
}

internal fun MediaController.playNextVexStream(
    state: VexStreamTrackState,
    resolver: VexStreamPlaybackSourceResolver,
): Boolean {
    val item =
        resolver.mediaItem(state)
            ?: return false

    if (mediaItemCount == 0) {
        setMediaItem(item)
        prepare()
        play()
        return true
    }

    val insertAt =
        (currentMediaItemIndex + 1)
            .coerceIn(
                0,
                mediaItemCount,
            )
    addMediaItem(
        insertAt,
        item,
    )
    return true
}

internal fun MediaController.queueVexStream(
    state: VexStreamTrackState,
    resolver: VexStreamPlaybackSourceResolver,
): Boolean {
    val item =
        resolver.mediaItem(state)
            ?: return false

    addMediaItem(item)
    return true
}

/**
 * Returns the adjacent queue index crossed by one drag gesture sample.
 *
 * The queue itself remains MediaSession-owned. Compose uses this only to decide when the dragged
 * row has crossed half of its current visual height and should request one Media3 move.
 */
internal fun vexStreamQueueAdjacentMoveTarget(
    currentIndex: Int,
    dragOffsetPx: Float,
    itemHeightPx: Int,
    lastIndex: Int,
): Int? {
    if (
        currentIndex !in 0..lastIndex ||
        itemHeightPx <= 0
    ) {
        return null
    }

    val threshold =
        itemHeightPx.toFloat() / 2f

    return when {
        dragOffsetPx > threshold &&
            currentIndex < lastIndex ->
            currentIndex + 1

        dragOffsetPx < -threshold &&
            currentIndex > 0 ->
            currentIndex - 1

        else -> null
    }
}

/**
 * Forms one visible queue shuffle without enabling Media3's persistent shuffle traversal.
 *
 * Already-played items and the current item stay fixed. Only the future queue is randomized.
 * If randomness produces the same order, rotate once so an explicit reshuffle is perceptible when
 * at least two future items exist.
 */
internal fun vexStreamQueueShuffleTargetOrder(
    itemCount: Int,
    currentIndex: Int,
    random: Random = Random.Default,
): List<Int> {
    if (itemCount <= 0) {
        return emptyList()
    }

    val lockedThrough =
        currentIndex.coerceIn(
            -1,
            itemCount - 1,
        )
    val prefix =
        if (lockedThrough >= 0) {
            (0..lockedThrough).toList()
        } else {
            emptyList()
        }
    val future =
        ((lockedThrough + 1) until itemCount)
            .toList()

    if (future.size <= 1) {
        return prefix + future
    }

    val randomized = future.shuffled(random)
    val changed =
        if (randomized == future) {
            randomized.drop(1) + randomized.first()
        } else {
            randomized
        }

    return prefix + changed
}

internal fun MediaController.shuffleVexStreamQueue(): Boolean {
    val count = mediaItemCount
    if (
        count <= 1 ||
        !isCommandAvailable(
            Player.COMMAND_CHANGE_MEDIA_ITEMS,
        )
    ) {
        return false
    }

    val currentIndex =
        currentMediaItemIndex
            .coerceIn(
                -1,
                count - 1,
            )
    val futureStart =
        (currentIndex + 1)
            .coerceIn(
                0,
                count,
            )
    val futureCount =
        count - futureStart
    if (futureCount <= 1) {
        return false
    }

    val targetOrder =
        vexStreamQueueShuffleTargetOrder(
            itemCount = count,
            currentIndex = currentIndex,
        )
    val shuffledFuture =
        targetOrder
            .drop(futureStart)
            .map(::getMediaItemAt)

    shuffleModeEnabled = false

    // One playlist mutation keeps large queues responsive. The previous implementation emitted one
    // moveMediaItem command per shuffled position, which caused hundreds of MediaSession timeline
    // updates and recompositions for large libraries.
    replaceMediaItems(
        futureStart,
        count,
        shuffledFuture,
    )
    return true
}

internal fun MediaController.toggleVexStreamRepeatAll() {
    repeatMode =
        if (repeatMode == Player.REPEAT_MODE_OFF) {
            Player.REPEAT_MODE_ALL
        } else {
            Player.REPEAT_MODE_OFF
        }
}

private fun Player.toVexStreamUiState(): VexStreamPlaybackUiState {
    val current =
        currentMediaItem

    val durationMs =
        duration.takeIf {
            it != C.TIME_UNSET && it > 0L
        } ?: 0L
    val positionMs =
        currentPosition
            .coerceAtLeast(0L)
            .let {
                if (durationMs > 0L) {
                    it.coerceAtMost(durationMs)
                } else {
                    it
                }
            }
    val bufferedMs =
        bufferedPosition
            .coerceAtLeast(0L)
            .let {
                if (durationMs > 0L) {
                    it.coerceAtMost(durationMs)
                } else {
                    it
                }
            }

    return VexStreamPlaybackUiState(
        currentPlayback =
            VexStreamPlaybackArchitecture
                .android
                .current(
                    this,
                ),
        currentMediaId = current?.mediaId,
        currentTitle =
            current
                ?.mediaMetadata
                ?.title
                ?.toString()
                .orEmpty(),
        currentArtist =
            current
                ?.mediaMetadata
                ?.artist
                ?.toString()
                .orEmpty(),
        repeatMode = repeatMode,
        queueMediaIds =
            (0 until mediaItemCount)
                .map { index ->
                    getMediaItemAt(index).mediaId
                },
        currentIndex =
            if (mediaItemCount > 0) {
                currentMediaItemIndex
            } else {
                -1
            },
        currentPositionMs = positionMs,
        durationMs = durationMs,
        bufferedPositionMs = bufferedMs,
    )
}

internal fun vexStreamSeekPositionMs(
    fraction: Float,
    durationMs: Long,
): Long {
    if (durationMs <= 0L) {
        return 0L
    }

    return (
        durationMs.toDouble() *
            fraction
                .coerceIn(
                    0f,
                    1f,
                )
                .toDouble()
    ).toLong()
        .coerceIn(
            0L,
            durationMs,
        )
}

internal fun formatVexStreamPlaybackTime(
    positionMs: Long,
): String {
    val totalSeconds =
        positionMs
            .coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes =
        (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        "$hours:" +
            minutes.toString().padStart(2, '0') +
            ":" +
            seconds.toString().padStart(2, '0')
    } else {
        "$minutes:" +
            seconds.toString().padStart(2, '0')
    }
}

// [VXG RealForever]
