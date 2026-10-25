package com.vextreme.vexlife.android.vexstream.track

import android.os.Handler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher

/**
 * Request to play one exact rendered Track inside the ordered Track context the listener acted on.
 *
 * The payload is current feature data, not a finite Behavior Control. The request deliberately
 * carries the rendered Track states needed for source resolution instead of promoting Hub track
 * refs into portable media identity.
 */
internal data class VexStreamTrackPlayRequest(
    val selected: VexStreamTrackState,
    val contextTracks: List<VexStreamTrackState>,
) {
    init {
        val trackRefs =
            contextTracks.map {
                it.track.trackRef
            }

        require(
            trackRefs.distinct().size ==
                trackRefs.size,
        ) {
            "Track play context must not contain duplicate track refs"
        }
        require(
            trackRefs.count {
                it ==
                    selected.track.trackRef
            } == 1,
        ) {
            "Track play selection must occur exactly once in its rendered context"
        }
    }
}

internal enum class VexStreamTrackQueuePlacement {
    Next,
    End,
}

/**
 * Request to place one rendered Track relative to the current Media3 queue.
 *
 * [Next] means immediately after the current item, or start playback when the provider queue is
 * empty. [End] means append without claiming playback should start.
 */
internal data class VexStreamTrackQueueRequest(
    val track: VexStreamTrackState,
    val placement: VexStreamTrackQueuePlacement,
)

/**
 * The exact source-resolution capability Track playback needs.
 *
 * The existing Android playback source resolver realizes this interface. Keeping the dependency
 * here prevents the Track request owner from depending on Compose/presentation source placement.
 */
internal fun interface VexStreamTrackMediaItemResolver {
    fun mediaItem(
        state: VexStreamTrackState,
    ): MediaItem?
}

/**
 * Feature-local payload boundary for Track -> Media3 composition.
 *
 * UI availability is informative only. Every activation resolves the requested media again and
 * re-checks the exact provider commands used by that operation before mutating Media3.
 */
internal interface VexStreamTrackPlaybackConnection {
    fun isPlayAvailable(
        request: VexStreamTrackPlayRequest,
    ): Boolean

    fun play(
        request: VexStreamTrackPlayRequest,
    ): Boolean

    fun isQueueAvailable(
        request: VexStreamTrackQueueRequest,
    ): Boolean

    fun queue(
        request: VexStreamTrackQueueRequest,
    ): Boolean
}

@Composable
internal fun rememberVexStreamTrackPlaybackConnection(
    controller: MediaController?,
    resolver: VexStreamTrackMediaItemResolver?,
): VexStreamTrackPlaybackConnection? {
    val connection =
        remember(
            controller,
            resolver,
        ) {
            if (
                controller == null ||
                resolver == null
            ) {
                null
            } else {
                Media3VexStreamTrackPlaybackConnection(
                    controller = controller,
                    resolver = resolver,
                )
            }
        }

    DisposableEffect(
        connection,
    ) {
        onDispose {
            connection?.shutdown()
        }
    }

    return connection
}

internal class Media3VexStreamTrackPlaybackConnection(
    private val controller: MediaController,
    private val resolver: VexStreamTrackMediaItemResolver,
) :
    VexStreamTrackPlaybackConnection {
    private val executionRoot =
        VexExecutionRoot(
            dispatcher = Dispatchers.IO,
        )
    private val execution by lazy {
        VexStreamTrackPlaybackExecution(
            root = executionRoot,
            controller = controller,
            resolver = resolver,
            connection = this,
        )
    }

    override fun isPlayAvailable(
        request: VexStreamTrackPlayRequest,
    ): Boolean =
        vexStreamTrackSourceProjectedAvailable(
            request.selected,
        ) &&
            playCommandsAvailable()

    /**
     * Admits the human Play request immediately, then performs source resolution under managed IO
     * execution. Media3 mutation is handed back only to the controller's application looper.
     */
    override fun play(
        request: VexStreamTrackPlayRequest,
    ): Boolean {
        if (!isPlayAvailable(request)) {
            return false
        }

        return execution.play(
            request,
        )
    }

    override fun isQueueAvailable(
        request: VexStreamTrackQueueRequest,
    ): Boolean =
        vexStreamTrackSourceProjectedAvailable(
            request.track,
        ) &&
            queueTarget(
                request,
            ) != null

    override fun queue(
        request: VexStreamTrackQueueRequest,
    ): Boolean {
        if (!isQueueAvailable(request)) {
            return false
        }

        return execution.queue(
            request,
        )
    }

    internal fun commitPlay(
        resolved: ResolvedTrackPlay,
    ): Boolean {
        if (
            !playCommandsAvailable()
        ) {
            return false
        }

        controller.setMediaItems(
            resolved.items,
            resolved.selectedIndex,
            0L,
        )

        if (
            controller.shuffleModeEnabled
        ) {
            controller.shuffleModeEnabled =
                false
        }

        controller.prepare()
        controller.play()
        return true
    }

    internal fun commitQueue(
        request: VexStreamTrackQueueRequest,
        item: MediaItem,
    ): Boolean {
        return when (
            val target = queueTarget(
                request,
            )
                ?: return false
        ) {
            VexStreamTrackQueueTarget.StartPlayback -> {
                controller.setMediaItem(
                    item,
                )
                controller.prepare()
                controller.play()
                true
            }

            is VexStreamTrackQueueTarget.Insert -> {
                controller.addMediaItem(
                    target.index,
                    item,
                )
                true
            }

            VexStreamTrackQueueTarget.Append -> {
                controller.addMediaItem(
                    item,
                )
                true
            }
        }
    }

    private fun playCommandsAvailable(): Boolean =
        vexStreamTrackPlayCommandsAvailable(
            changeMediaItemsAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_CHANGE_MEDIA_ITEMS,
                ),
            prepareAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_PREPARE,
                ),
            playAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_PLAY_PAUSE,
                ),
            shuffleEnabled =
                controller.shuffleModeEnabled,
            setShuffleModeAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_SET_SHUFFLE_MODE,
                ),
        )

    private fun queueTarget(
        request: VexStreamTrackQueueRequest,
    ): VexStreamTrackQueueTarget? {
        val timelineAvailable =
            controller.isCommandAvailable(
                Player.COMMAND_GET_TIMELINE,
            )
        val mediaItemCount =
            if (
                request.placement ==
                    VexStreamTrackQueuePlacement.Next &&
                timelineAvailable
            ) {
                controller.mediaItemCount
            } else {
                null
            }
        val currentMediaItemIndex =
            if (
                request.placement ==
                    VexStreamTrackQueuePlacement.Next &&
                timelineAvailable
            ) {
                controller.currentMediaItemIndex
            } else {
                null
            }

        return vexStreamTrackQueueTarget(
            placement =
                request.placement,
            mediaItemCount =
                mediaItemCount,
            currentMediaItemIndex =
                currentMediaItemIndex,
            timelineAvailable =
                timelineAvailable,
            setMediaItemAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_SET_MEDIA_ITEM,
                ),
            changeMediaItemsAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_CHANGE_MEDIA_ITEMS,
                ),
            prepareAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_PREPARE,
                ),
            playAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_PLAY_PAUSE,
                ),
        )
    }

    fun shutdown() {
        executionRoot.forceStopAll(
            "VexStream Track playback presentation disposed",
        )
    }
}

private class VexStreamTrackPlaybackExecution(
    root: VexExecutionRoot,
    controller: MediaController,
    private val resolver:
        VexStreamTrackMediaItemResolver,
    private val connection:
        Media3VexStreamTrackPlaybackConnection,
) :
    VexExecution(
        root,
    ) {
    private val providerDispatcher =
        Handler(
            controller.applicationLooper,
        ).asCoroutineDispatcher(
            "VexStream Media3",
        )

    fun play(
        request: VexStreamTrackPlayRequest,
    ): Boolean =
        runCatching {
            launchOnce(
                ::playForExecution,
                request,
            )
        }.isSuccess

    fun queue(
        request: VexStreamTrackQueueRequest,
    ): Boolean =
        runCatching {
            launchOnce(
                ::queueForExecution,
                request,
            )
        }.isSuccess

    private suspend fun playForExecution(
        request: VexStreamTrackPlayRequest,
    ) {
        val resolved =
            resolveVexStreamTrackPlay(
                request = request,
                resolver = resolver,
            )
                ?: return

        handoff(
            providerDispatcher,
        ) {
            connection.commitPlay(
                resolved,
            )
        }
    }

    private suspend fun queueForExecution(
        request: VexStreamTrackQueueRequest,
    ) {
        val item =
            resolver.mediaItem(
                request.track,
            )
                ?: return

        handoff(
            providerDispatcher,
        ) {
            connection.commitQueue(
                request = request,
                item = item,
            )
        }
    }
}

private fun resolveVexStreamTrackPlay(
    request: VexStreamTrackPlayRequest,
    resolver: VexStreamTrackMediaItemResolver,
): ResolvedTrackPlay? {
    val items =
        request.contextTracks
            .mapNotNull(
                resolver::mediaItem,
            )

    val selectedIndex =
        vexStreamTrackPlayTargetIndex(
            request = request,
            resolvedMediaIds =
                items.map {
                    it.mediaId
                },
        )
            ?: return null

    return ResolvedTrackPlay(
        items = items,
        selectedIndex = selectedIndex,
    )
}

private data class ResolvedTrackPlay(
    val items: List<MediaItem>,
    val selectedIndex: Int,
)

internal fun vexStreamTrackPlayTargetIndex(
    request: VexStreamTrackPlayRequest,
    resolvedMediaIds: List<String>,
): Int? =
    resolvedMediaIds
        .indexOfFirst {
            it ==
                request
                    .selected
                    .track
                    .trackRef
                    .value
        }
        .takeIf {
            it >= 0
        }

/**
 * Cheap presentation projection only.
 *
 * This intentionally does not verify local bytes. Local-file digest verification and final source
 * resolution remain activation-time work in [VexStreamTrackMediaItemResolver], but Track activation
 * now runs that blocking preparation under VexExecution on IO rather than inside the Compose input
 * callback. List composition therefore stays cheap and Media3 mutation remains on its provider
 * looper.
 */
internal fun vexStreamTrackSourceProjectedAvailable(
    state: VexStreamTrackState,
): Boolean =
    state.availability !=
        VexStreamTrackAvailability.UNAVAILABLE

internal fun vexStreamTrackPlayCommandsAvailable(
    changeMediaItemsAvailable: Boolean,
    prepareAvailable: Boolean,
    playAvailable: Boolean,
    shuffleEnabled: Boolean,
    setShuffleModeAvailable: Boolean,
): Boolean =
    changeMediaItemsAvailable &&
        prepareAvailable &&
        playAvailable &&
        (
            !shuffleEnabled ||
                setShuffleModeAvailable
        )

internal sealed interface VexStreamTrackQueueTarget {
    data object StartPlayback :
        VexStreamTrackQueueTarget

    data class Insert(
        val index: Int,
    ) :
        VexStreamTrackQueueTarget

    data object Append :
        VexStreamTrackQueueTarget
}

internal fun vexStreamTrackQueueTarget(
    placement: VexStreamTrackQueuePlacement,
    mediaItemCount: Int?,
    currentMediaItemIndex: Int?,
    timelineAvailable: Boolean,
    setMediaItemAvailable: Boolean,
    changeMediaItemsAvailable: Boolean,
    prepareAvailable: Boolean,
    playAvailable: Boolean,
): VexStreamTrackQueueTarget? =
    when (
        placement
    ) {
        VexStreamTrackQueuePlacement.End ->
            VexStreamTrackQueueTarget
                .Append
                .takeIf {
                    changeMediaItemsAvailable
                }

        VexStreamTrackQueuePlacement.Next -> {
            if (
                !timelineAvailable ||
                mediaItemCount == null ||
                currentMediaItemIndex == null
            ) {
                null
            } else {
                require(
                    mediaItemCount >= 0,
                ) {
                    "mediaItemCount must not be negative"
                }

                if (
                    mediaItemCount == 0
                ) {
                    VexStreamTrackQueueTarget
                        .StartPlayback
                        .takeIf {
                            setMediaItemAvailable &&
                                prepareAvailable &&
                                playAvailable
                        }
                } else {
                    VexStreamTrackQueueTarget
                        .Insert(
                            index =
                                (
                                    currentMediaItemIndex +
                                        1
                                ).coerceIn(
                                    0,
                                    mediaItemCount,
                                ),
                        )
                        .takeIf {
                            changeMediaItemsAvailable
                        }
                }
            }
        }
    }

// [VXG RealForever]
