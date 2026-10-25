package com.vextreme.vexlife.android.vexstream.collection

import android.os.Handler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueShuffleTargetOrder
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackMediaItemResolver
import com.vextreme.vexlife.android.vexstream.track.prepareVexStreamTrackArtworkBestEffort
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal enum class VexStreamCollectionPlaybackOrder {
    Natural,
    Shuffled,
}

/**
 * One exact request to play the rendered Track sequence the listener acted on.
 *
 * Track-ref uniqueness remains upstream collection/catalog truth; this request owns playback order,
 * not collection membership validation.
 */
internal data class VexStreamCollectionPlaybackRequest(
    val tracks: List<VexStreamTrackState>,
    val order: VexStreamCollectionPlaybackOrder,
)

/**
 * Feature-local payload boundary for rendered collection -> Media3 playback.
 *
 * Availability is a cheap projection only. Activation resolves media on managed IO, prepares only
 * the first ordered/current track's ancillary artwork, rechecks provider commands, then commits on
 * the Media3 application looper.
 */
internal interface VexStreamCollectionPlaybackConnection {
    fun isAvailable(
        request: VexStreamCollectionPlaybackRequest,
    ): Boolean

    fun play(
        request: VexStreamCollectionPlaybackRequest,
    ): Boolean
}

@Composable
internal fun rememberVexStreamCollectionPlaybackConnection(
    controller: MediaController?,
    resolver: VexStreamTrackMediaItemResolver?,
): VexStreamCollectionPlaybackConnection? {
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
                Media3VexStreamCollectionPlaybackConnection(
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

internal class Media3VexStreamCollectionPlaybackConnection(
    private val controller: MediaController,
    private val resolver: VexStreamTrackMediaItemResolver,
) :
    VexStreamCollectionPlaybackConnection {
    private val executionRoot =
        VexExecutionRoot(
            dispatcher = Dispatchers.IO,
        )
    private val executionDelegate =
        lazy {
            VexStreamCollectionPlaybackExecution(
                root = executionRoot,
                providerDispatcher =
                    Handler(
                        controller.applicationLooper,
                    ).asCoroutineDispatcher(
                        "VexStream collection Media3",
                    ),
                resolver = resolver,
                commitPlay = ::commitPlay,
            )
        }
    private val execution:
        VexStreamCollectionPlaybackExecution
        get() =
            executionDelegate.value

    override fun isAvailable(
        request: VexStreamCollectionPlaybackRequest,
    ): Boolean =
        vexStreamCollectionSourceProjectedAvailable(
            request.tracks,
        ) &&
            commandsAvailable()

    override fun play(
        request: VexStreamCollectionPlaybackRequest,
    ): Boolean {
        if (
            !isAvailable(
                request,
            )
        ) {
            return false
        }

        return execution.play(
            request,
        )
    }

    internal fun commitPlay(
        resolved: ResolvedCollectionPlayback,
    ): Boolean {
        if (
            resolved.items.isEmpty() ||
            !commandsAvailable()
        ) {
            return false
        }

        controller.setMediaItems(
            resolved.items,
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

    fun shutdown() {
        if (
            executionDelegate.isInitialized()
        ) {
            execution.shutdown()
        }
        executionRoot.forceStopAll(
            "VexStream Collection playback presentation disposed",
        )
    }

    private fun commandsAvailable(): Boolean =
        vexStreamCollectionPlaybackCommandsAvailable(
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
}

internal class VexStreamCollectionPlaybackExecution(
    root: VexExecutionRoot,
    private val providerDispatcher:
        CoroutineDispatcher,
    private val resolver:
        VexStreamTrackMediaItemResolver,
    private val commitPlay:
        (ResolvedCollectionPlayback) -> Boolean,
) :
    VexExecution(
        root,
    ) {
    private data class PlayIntent(
        val sequence: Long,
        val request:
            VexStreamCollectionPlaybackRequest,
    )

    private val playSequence =
        AtomicLong(
            0L,
        )
    private val requests =
        Channel<
            PlayIntent
        >(
            capacity =
                Channel.CONFLATED,
        )

    init {
        launchForCollect(
            source =
                ::requestsForExecution,
            restartOnSourceFailure =
                false,
        ) { intent ->
            resolveForExecution(
                intent,
            )
        }
    }

    fun play(
        request: VexStreamCollectionPlaybackRequest,
    ): Boolean =
        requests
            .trySend(
                PlayIntent(
                    sequence =
                        playSequence.incrementAndGet(),
                    request = request,
                ),
            )
            .isSuccess

    fun shutdown() {
        requests.close()
    }

    private fun requestsForExecution():
        Flow<
            PlayIntent
        > =
        requests.receiveAsFlow()

    private suspend fun resolveForExecution(
        intent: PlayIntent,
    ) {
        var resolved =
            resolveVexStreamCollectionPlayback(
                request =
                    intent.request,
                resolver = resolver,
            )
                ?: return

        val currentState =
            resolved.orderedStates
                .firstOrNull()
        if (
            currentState != null
        ) {
            prepareVexStreamTrackArtworkBestEffort(
                resolver = resolver,
                state = currentState,
            )

            resolver.mediaItem(
                currentState,
            )?.let { refreshedCurrent ->
                resolved =
                    resolved.copy(
                        items =
                            listOf(
                                refreshedCurrent,
                            ) +
                                resolved.items
                                    .drop(
                                        1,
                                    ),
                    )
            }
        }

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
}

internal data class ResolvedCollectionPlayback(
    val orderedStates: List<VexStreamTrackState>,
    val items: List<MediaItem>,
)

internal fun resolveVexStreamCollectionPlayback(
    request: VexStreamCollectionPlaybackRequest,
    resolver: VexStreamTrackMediaItemResolver,
    random: Random = Random.Default,
): ResolvedCollectionPlayback? {
    val resolved =
        request.tracks
            .mapNotNull { state ->
                resolver.mediaItem(
                    state,
                )
                    ?.let { item ->
                        state to item
                    }
            }
    if (
        resolved.isEmpty()
    ) {
        return null
    }

    val targetOrder =
        vexStreamCollectionPlaybackTargetOrder(
            itemCount = resolved.size,
            order = request.order,
            random = random,
        )

    return ResolvedCollectionPlayback(
        orderedStates =
            targetOrder.map { index ->
                resolved[
                    index
                ].first
            },
        items =
            targetOrder.map { index ->
                resolved[
                    index
                ].second
            },
    )
}

/**
 * Cheap presentation projection only; activation performs the real source resolution.
 */
internal fun vexStreamCollectionSourceProjectedAvailable(
    tracks: List<VexStreamTrackState>,
): Boolean =
    tracks.any {
        it.availability !=
            VexStreamTrackAvailability.UNAVAILABLE
    }

internal fun vexStreamCollectionPlaybackCommandsAvailable(
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

/**
 * Shuffle All is a one-shot collection permutation, not persistent Media3 shuffle traversal.
 */
internal fun vexStreamCollectionPlaybackTargetOrder(
    itemCount: Int,
    order: VexStreamCollectionPlaybackOrder,
    random: Random = Random.Default,
): List<Int> {
    require(
        itemCount >= 0,
    ) {
        "itemCount must not be negative"
    }

    return when (
        order
    ) {
        VexStreamCollectionPlaybackOrder.Natural ->
            (0 until itemCount).toList()

        VexStreamCollectionPlaybackOrder.Shuffled ->
            vexStreamQueueShuffleTargetOrder(
                itemCount = itemCount,
                currentIndex = -1,
                random = random,
            )
    }
}

// [VXG RealForever]
