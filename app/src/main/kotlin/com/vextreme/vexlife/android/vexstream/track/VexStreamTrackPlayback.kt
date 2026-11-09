@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.vexstream.track

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.vexstream.VexStreamMusicViewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import kotlinx.coroutines.CancellationException

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
 * Feature-local Track playback surface.
 *
 * This is not a Partner Connection. It owns Track source preparation and one disposable
 * VexExecution lifetime, then addresses the actual Player Partner request Connection.
 */
internal class VexStreamTrackPlayback(
    root:
        VexExecutionRoot,
    private val resolver:
        VexStreamTrackPlaybackResolver,
    private val connection:
        VexStreamPlayerPartner.Connection.Request,
) {
    private val execution =
        Execution(
            root =
                root,
            resolver =
                resolver,
            connection =
                connection,
        )

    fun isPlayAvailable(
        request:
            VexStreamTrackPlayRequest,
    ):
        Boolean =
        vexStreamTrackSourceProjectedAvailable(
            request.selected,
        ) &&
            connection.mediaPlaybackAvailable

    fun play(
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

        execution.play(
            request,
        )
        return true
    }

    fun isQueueAvailable(
        request:
            VexStreamTrackQueueRequest,
    ):
        Boolean =
        vexStreamTrackSourceProjectedAvailable(
            request.track,
        ) &&
            connection.mediaQueueAvailable(
                request.placement
                    .toPlayerPlacement(),
            )

    fun queue(
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

        execution.queue(
            request,
        )
        return true
    }

    fun shutdown() {
        execution.shutdown()
    }

    private class Execution(
        root:
            VexExecutionRoot,
        private val resolver:
            VexStreamTrackPlaybackResolver,
        private val connection:
            VexStreamPlayerPartner.Connection.Request,
    ) :
        VexExecution(
            root,
        ) {
        private object Play :
            VexExecution.RelationshipId

        private object Queue :
            VexExecution.RelationshipId

        fun play(
            request:
                VexStreamTrackPlayRequest,
        ) {
            beginLatest(
                relationshipId =
                    Play,
                operation =
                    ::resolvePlay,
                argument =
                    request,
            )
        }

        fun queue(
            request:
                VexStreamTrackQueueRequest,
        ) {
            beginQueued(
                relationshipId =
                    Queue,
                operation =
                    ::resolveQueue,
                argument =
                    request,
            )
        }

        fun shutdown() {
            forceStopOwned(
                "VexStream Track playback disposed",
            )
        }

        private suspend fun resolvePlay(
            request:
                VexStreamTrackPlayRequest,
        ) {
            prepareVexStreamTrackArtworkBestEffort(
                resolver =
                    resolver,
                state =
                    request.selected,
            )

            val resolved =
                resolveVexStreamTrackPlay(
                    request =
                        request,
                    resolver =
                        resolver,
                )
                    ?: return

            connection.playMedia(
                items =
                    resolved.items,
                selectedIndex =
                    resolved.selectedIndex,
            )
        }

        private suspend fun resolveQueue(
            request:
                VexStreamTrackQueueRequest,
        ) {
            prepareVexStreamTrackArtworkBestEffort(
                resolver =
                    resolver,
                state =
                    request.track,
            )

            val media =
                resolver.media(
                    request.track,
                )
                    ?: return

            connection.queueMedia(
                media =
                    media,
                placement =
                    request.placement
                        .toPlayerPlacement(),
            )
        }
    }
}

@Composable
internal fun rememberVexStreamTrackPlayback(
    resolver:
        VexStreamTrackPlaybackResolver?,
):
    VexStreamTrackPlayback? {
    val owner:
        VexStreamMusicViewModel =
        viewModel()
    val connection =
        owner.playerRequestConnection
    val playback =
        remember(
            owner.executionRoot,
            resolver,
            connection,
        ) {
            if (
                resolver ==
                null ||
                connection ==
                null
            ) {
                null
            } else {
                VexStreamTrackPlayback(
                    root =
                        owner.executionRoot,
                    resolver =
                        resolver,
                    connection =
                        connection,
                )
            }
        }

    DisposableEffect(
        playback,
    ) {
        onDispose {
            playback?.shutdown()
        }
    }

    return playback
}

private fun VexStreamTrackQueuePlacement.toPlayerPlacement():
    VexStreamPlayerPartner.QueuePlacement =
    when (
        this
    ) {
        VexStreamTrackQueuePlacement.Next ->
            VexStreamPlayerPartner.QueuePlacement.Next

        VexStreamTrackQueuePlacement.End ->
            VexStreamPlayerPartner.QueuePlacement.End
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

// [VXG RealForever][VEXSTREAM][TRACK][PLAYER-PARTNER]
