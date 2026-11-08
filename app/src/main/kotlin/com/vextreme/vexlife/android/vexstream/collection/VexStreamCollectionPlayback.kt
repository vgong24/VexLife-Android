package com.vextreme.vexlife.android.vexstream.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackConnectionViewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueShuffleTargetOrder
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackPlaybackResolver
import com.vextreme.vexlife.android.vexstream.track.prepareVexStreamTrackArtworkBestEffort
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
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
 * Provider realization needed after Collection source/order meaning has already been resolved.
 */
internal interface VexStreamCollectionPlaybackRealization {
    val isCollectionPlaybackAvailable:
        Boolean

    suspend fun realizeCollectionPlayback(
        resolved:
            ResolvedCollectionPlayback,
    ):
        Boolean
}

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
    resolver:
        VexStreamTrackPlaybackResolver?,
):
    VexStreamCollectionPlaybackConnection? {
    val owner:
        VexStreamPlaybackConnectionViewModel =
        viewModel()
    val realization =
        owner.playerRequestConnection
    val connection =
        remember(
            owner.executionRoot,
            resolver,
            realization,
        ) {
            if (
                resolver ==
                null ||
                realization ==
                null
            ) {
                null
            } else {
                PartnerVexStreamCollectionPlaybackConnection(
                    executionRoot =
                        owner.executionRoot,
                    resolver =
                        resolver,
                    realization =
                        realization,
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

internal class PartnerVexStreamCollectionPlaybackConnection(
    executionRoot:
        VexExecutionRoot,
    private val resolver:
        VexStreamTrackPlaybackResolver,
    private val realization:
        VexStreamCollectionPlaybackRealization,
) :
    VexStreamCollectionPlaybackConnection {
    private val execution =
        VexStreamCollectionPlaybackExecution(
            root =
                executionRoot,
            resolver =
                resolver,
            realization =
                realization,
        )

    override fun isAvailable(
        request:
            VexStreamCollectionPlaybackRequest,
    ):
        Boolean =
        vexStreamCollectionSourceProjectedAvailable(
            request.tracks,
        ) &&
            realization.isCollectionPlaybackAvailable

    override fun play(
        request:
            VexStreamCollectionPlaybackRequest,
    ):
        Boolean {
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

    fun shutdown() {
        execution.shutdown()
    }
}

internal class VexStreamCollectionPlaybackExecution(
    root:
        VexExecutionRoot,
    private val resolver:
        VexStreamTrackPlaybackResolver,
    private val realization:
        VexStreamCollectionPlaybackRealization,
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
        forceStopOwned(
            "VexStream Collection playback disposed",
        )
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

            resolver.media(
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

        if (
            playSequence.get() ==
            intent.sequence
        ) {
            realization.realizeCollectionPlayback(
                resolved,
            )
        }
    }
}

internal data class ResolvedCollectionPlayback(
    val orderedStates: List<VexStreamTrackState>,
    val items: List<VexStreamPlayerPartner.Media>,
)

internal fun resolveVexStreamCollectionPlayback(
    request: VexStreamCollectionPlaybackRequest,
    resolver: VexStreamTrackPlaybackResolver,
    random: Random = Random.Default,
): ResolvedCollectionPlayback? {
    val resolved =
        request.tracks
            .mapNotNull { state ->
                resolver.media(
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
