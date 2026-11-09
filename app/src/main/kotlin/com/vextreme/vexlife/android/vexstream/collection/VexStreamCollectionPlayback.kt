@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.vexstream.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.VexStreamMusicViewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackPlaybackResolver
import com.vextreme.vexlife.android.vexstream.track.prepareVexStreamTrackArtworkBestEffort
import kotlin.random.Random

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
 * Feature-local Collection playback workflow.
 *
 * This does not earn a Family: the request resolves one rendered collection payload and does not own
 * an independently meaningful semantic CURRENT. It is also not a Partner Connection; the actual
 * outside participant remains [VexStreamPlayerPartner].
 *
 * The private [Execution] is the disposable owner of source preparation and the eventual Player
 * request while the shared [VexExecutionRoot] may outlive this remembered feature surface and host
 * sibling work. This keeps Latest custody and disposal cancellation inside VexExecution instead of a
 * feature-created CoroutineScope, SupervisorJob, retained Job field, or raw launch block.
 *
 * A failed finite attempt completes and leaves custody, so a later Collection request may be admitted
 * through the same live workflow. The foundation does not automatically retry the provider request;
 * only the feature/provider boundary can decide whether repeating a real operation is safe.
 */
internal class VexStreamCollectionPlayback(
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

    fun isAvailable(
        request:
            VexStreamCollectionPlaybackRequest,
    ):
        Boolean =
        vexStreamCollectionSourceProjectedAvailable(
            request.tracks,
        ) &&
            connection.mediaPlaybackAvailable

    fun play(
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

        execution.play(
            request,
        )
        return true
    }

    fun shutdown() {
        execution.shutdown()
    }

    /**
     * Exact execution owner for the disposable Collection workflow.
     *
     * Play is Latest because a newer collection/order request supersedes unfinished preparation for
     * an older request. This RelationshipId is local workflow coordination; it is not a Family
     * identity and is never derived from the request payload or reflected callable name.
     */
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

        fun play(
            request:
                VexStreamCollectionPlaybackRequest,
        ) {
            beginLatest(
                relationshipId =
                    Play,
                operation =
                    ::resolve,
                argument =
                    request,
            )
        }

        fun shutdown() {
            forceStopOwned(
                "VexStream Collection playback disposed",
            )
        }

        private suspend fun resolve(
            request:
                VexStreamCollectionPlaybackRequest,
        ) {
            var resolved =
                resolveVexStreamCollectionPlayback(
                    request =
                        request,
                    resolver =
                        resolver,
                )
                    ?: return

            val currentState =
                resolved.orderedStates
                    .firstOrNull()
            if (
                currentState !=
                null
            ) {
                prepareVexStreamTrackArtworkBestEffort(
                    resolver =
                        resolver,
                    state =
                        currentState,
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

            connection.playMedia(
                items =
                    resolved.items,
                selectedIndex =
                    0,
            )
        }
    }
}

@Composable
internal fun rememberVexStreamCollectionPlayback(
    resolver:
        VexStreamTrackPlaybackResolver?,
):
    VexStreamCollectionPlayback? {
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
                VexStreamCollectionPlayback(
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

        VexStreamCollectionPlaybackOrder.Shuffled -> {
            val natural =
                (0 until itemCount)
                    .toList()

            if (
                natural.size <=
                1
            ) {
                natural
            } else {
                val randomized =
                    natural.shuffled(
                        random,
                    )

                if (
                    randomized ==
                    natural
                ) {
                    randomized.drop(
                        1,
                    ) +
                        randomized.first()
                } else {
                    randomized
                }
            }
        }
    }
}

// [VXG RealForever]
