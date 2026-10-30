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
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

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

    /**
     * Best-effort preparation for ancillary artwork before [mediaItem] publishes metadata.
     *
     * Ordinary resolver fixtures inherit the no-op. Production may cross its repository/network
     * boundary here because Track execution already owns an IO worker.
     */
    suspend fun prepareArtwork(
        state: VexStreamTrackState,
    ) = Unit
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
    private val executionDelegate =
        lazy {
            VexStreamTrackPlaybackExecution(
            root = executionRoot,
            owner =
                this@Media3VexStreamTrackPlaybackConnection,
            providerDispatcher =
                Handler(
                    controller.applicationLooper,
                ).asCoroutineDispatcher(
                    "VexStream Media3",
                ),
            resolver = resolver,
            commitPlay = ::commitPlay,
                commitQueue = ::commitQueue,
            )
        }
    private val execution:
        VexStreamTrackPlaybackExecution
        get() =
            executionDelegate.value

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
        if (executionDelegate.isInitialized()) {
            execution.shutdown()
        }
        executionRoot.forceStopAll(
            "VexStream Track playback presentation disposed",
        )
    }
}

@OptIn(VexExecution.RelationshipFoundationApi::class)
internal class VexStreamTrackPlaybackExecution(
    root: VexExecutionRoot,
    private val owner:
        Media3VexStreamTrackPlaybackConnection,
    private val providerDispatcher:
        CoroutineDispatcher,
    private val resolver:
        VexStreamTrackMediaItemResolver,
    private val commitPlay:
        (ResolvedTrackPlay) -> Boolean,
    private val commitQueue:
        (
            VexStreamTrackQueueRequest,
            MediaItem,
        ) -> Boolean,
) :
    VexExecution<
        Media3VexStreamTrackPlaybackConnection
    >(
        root,
    ) {
    private enum class RelationshipKind {
        PlayRequests,
        QueueRequests,
    }

    private class Relationship(
        private val owner:
            Media3VexStreamTrackPlaybackConnection,
        private val kind:
            RelationshipKind,
    ) :
        VexExecution.RelationshipId<
            Media3VexStreamTrackPlaybackConnection
        > {
        override fun equals(
            other:
                Any?,
        ):
            Boolean =
            other is
                Relationship &&
                owner ===
                other.owner &&
                kind ==
                other.kind

        override fun hashCode():
            Int =
            31 *
                System.identityHashCode(
                    owner,
                ) +
                kind.hashCode()
    }

    private val playRequestsRelationship =
        Relationship(
            owner =
                owner,
            kind =
                RelationshipKind.PlayRequests,
        )
    private val queueRequestsRelationship =
        Relationship(
            owner =
                owner,
            kind =
                RelationshipKind.QueueRequests,
        )

    private data class PlayIntent(
        val sequence: Long,
        val request:
            VexStreamTrackPlayRequest,
    )

    private val playSequence =
        AtomicLong(
            0L,
        )
    private val playRequests =
        Channel<PlayIntent>(
            capacity =
                Channel.CONFLATED,
        )
    private val queueRequests =
        Channel<
            VexStreamTrackQueueRequest
        >(
            capacity =
                Channel.UNLIMITED,
        )

    init {
        launchForCollect(
            relationshipId =
                playRequestsRelationship,
            source =
                ::playRequestsForExecution,
            restartOnSourceFailure =
                false,
        ) { intent ->
            resolvePlayForExecution(
                intent,
            )
        }
        launchForCollect(
            relationshipId =
                queueRequestsRelationship,
            source =
                ::queueRequestsForExecution,
            restartOnSourceFailure =
                false,
        ) { request ->
            resolveQueueForExecution(
                request,
            )
        }
    }

    /**
     * Play is a selection intent. While source preparation is running, a newer Play replaces any
     * pending selection and prevents the superseded preparation from committing to Media3.
     */
    fun play(
        request: VexStreamTrackPlayRequest,
    ): Boolean {
        val intent =
            PlayIntent(
                sequence =
                    playSequence.incrementAndGet(),
                request =
                    request,
            )
        return playRequests
            .trySend(
                intent,
            )
            .isSuccess
    }

    /**
     * Queue actions are additive intent, not selection. Preserve every admitted request in order.
     */
    fun queue(
        request: VexStreamTrackQueueRequest,
    ): Boolean =
        queueRequests
            .trySend(
                request,
            )
            .isSuccess

    fun shutdown() {
        playRequests.close()
        queueRequests.close()
    }

    private fun playRequestsForExecution():
        Flow<PlayIntent> =
        playRequests.receiveAsFlow()

    private fun queueRequestsForExecution():
        Flow<VexStreamTrackQueueRequest> =
        queueRequests.receiveAsFlow()

    private suspend fun resolvePlayForExecution(
        intent: PlayIntent,
    ) {
        prepareVexStreamTrackArtworkBestEffort(
            resolver = resolver,
            state =
                intent.request
                    .selected,
        )

        val resolved =
            resolveVexStreamTrackPlay(
                request =
                    intent.request,
                resolver =
                    resolver,
            )
                ?: return

        if (
            playSequence.get() !=
            intent.sequence
        ) {
            return
        }

        handoff(
            providerDispatcher,
        ) {
            if (
                playSequence.get() ==
                intent.sequence
            ) {
                commitPlay(
                    resolved,
                )
            }
        }
    }

    private suspend fun resolveQueueForExecution(
        request:
            VexStreamTrackQueueRequest,
    ) {
        prepareVexStreamTrackArtworkBestEffort(
            resolver = resolver,
            state = request.track,
        )

        val item =
            resolver.mediaItem(
                request.track,
            )
                ?: return

        handoff(
            providerDispatcher,
        ) {
            commitQueue(
                request,
                item,
            )
        }
    }
}

internal suspend fun prepareVexStreamTrackArtworkBestEffort(
    resolver: VexStreamTrackMediaItemResolver,
    state: VexStreamTrackState,
) {
    try {
        resolver.prepareArtwork(
            state,
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        // Artwork is ancillary. Playback source resolution must remain available.
    }
}

internal fun resolveVexStreamTrackPlay(
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

internal data class ResolvedTrackPlay(
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
