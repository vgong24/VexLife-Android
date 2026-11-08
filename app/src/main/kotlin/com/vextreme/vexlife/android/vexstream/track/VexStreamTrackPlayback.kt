package com.vextreme.vexlife.android.vexstream.track

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackConnectionViewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
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
internal fun interface VexStreamTrackPlaybackResolver {
    fun media(
        state:
            VexStreamTrackState,
    ):
        VexStreamPlayerPartner.Media?

    suspend fun prepareArtwork(
        state:
            VexStreamTrackState,
    ) = Unit
}

/**
 * Provider realization needed after Track source/currentness meaning has already been resolved.
 */
internal interface VexStreamTrackPlaybackRealization {
    val isTrackPlayAvailable:
        Boolean

    fun isTrackQueueAvailable(
        request:
            VexStreamTrackQueueRequest,
    ):
        Boolean

    suspend fun realizeTrackPlay(
        resolved:
            ResolvedTrackPlay,
    ):
        Boolean

    suspend fun realizeTrackQueue(
        request:
            VexStreamTrackQueueRequest,
        media:
            VexStreamPlayerPartner.Media,
    ):
        Boolean
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
    resolver:
        VexStreamTrackPlaybackResolver?,
):
    VexStreamTrackPlaybackConnection? {
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
                PartnerVexStreamTrackPlaybackConnection(
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

internal class PartnerVexStreamTrackPlaybackConnection(
    executionRoot:
        VexExecutionRoot,
    private val resolver:
        VexStreamTrackPlaybackResolver,
    private val realization:
        VexStreamTrackPlaybackRealization,
) :
    VexStreamTrackPlaybackConnection {
    private val execution =
        VexStreamTrackPlaybackExecution(
            root =
                executionRoot,
            resolver =
                resolver,
            realization =
                realization,
        )

    override fun isPlayAvailable(
        request:
            VexStreamTrackPlayRequest,
    ):
        Boolean =
        vexStreamTrackSourceProjectedAvailable(
            request.selected,
        ) &&
            realization.isTrackPlayAvailable

    override fun play(
        request:
            VexStreamTrackPlayRequest,
    ):
        Boolean {
        if (
            !isPlayAvailable(
                request,
            )
        ) {
            return false
        }

        return execution.play(
            request,
        )
    }

    override fun isQueueAvailable(
        request:
            VexStreamTrackQueueRequest,
    ):
        Boolean =
        vexStreamTrackSourceProjectedAvailable(
            request.track,
        ) &&
            realization.isTrackQueueAvailable(
                request,
            )

    override fun queue(
        request:
            VexStreamTrackQueueRequest,
    ):
        Boolean {
        if (
            !isQueueAvailable(
                request,
            )
        ) {
            return false
        }

        return execution.queue(
            request,
        )
    }

    fun shutdown() {
        execution.shutdown()
    }
}

internal class VexStreamTrackPlaybackExecution(
    root:
        VexExecutionRoot,
    private val resolver:
        VexStreamTrackPlaybackResolver,
    private val realization:
        VexStreamTrackPlaybackRealization,
) :
    VexExecution(
        root,
    ) {
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
        forceStopOwned(
            "VexStream Track playback disposed",
        )
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

        if (
            playSequence.get() ==
            intent.sequence
        ) {
            realization.realizeTrackPlay(
                resolved,
            )
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
            resolver.media(
                request.track,
            )
                ?: return

        realization.realizeTrackQueue(
            request =
                request,
            media =
                item,
        )
    }
}

internal suspend fun prepareVexStreamTrackArtworkBestEffort(
    resolver: VexStreamTrackPlaybackResolver,
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
    resolver: VexStreamTrackPlaybackResolver,
): ResolvedTrackPlay? {
    val items =
        request.contextTracks
            .mapNotNull(
                resolver::media,
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
    val items: List<VexStreamPlayerPartner.Media>,
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
 * resolution remain activation-time work in [VexStreamTrackPlaybackResolver], but Track activation
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
