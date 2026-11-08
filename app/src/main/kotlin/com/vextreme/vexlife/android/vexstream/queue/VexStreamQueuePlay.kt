package com.vextreme.vexlife.android.vexstream.queue

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerConnectionViewModel

internal data class VexStreamQueuePlayRequest(
    val occurrence:
        VexStreamQueueOccurrence,
)

/**
 * Payload boundary for "play this exact current queue occurrence now".
 *
 * The concrete Player Partner Connection rechecks provider queue identity and command capability at
 * activation time; presentation does not read Media3.
 */
internal interface VexStreamQueuePlayConnection {
    val isPlayAvailable:
        Boolean

    fun play(
        request:
            VexStreamQueuePlayRequest,
    ):
        Boolean
}

@Composable
internal fun rememberVexStreamQueuePlayConnection():
    VexStreamQueuePlayConnection? {
    val owner:
        VexStreamPlayerConnectionViewModel =
        viewModel()

    return owner.queuePlayConnection
}

internal fun vexStreamQueuePlayTargetIndex(
    request: VexStreamQueuePlayRequest,
    currentMediaIds: List<String>,
    seekAvailable: Boolean,
    playAvailable: Boolean,
): Int? {
    if (
        !seekAvailable ||
        !playAvailable
    ) {
        return null
    }

    val current =
        vexStreamQueueOccurrenceAt(
            mediaIds =
                currentMediaIds,
            index =
                request
                    .occurrence
                    .index,
        )
            ?: return null

    return request
        .occurrence
        .index
        .takeIf {
            current ==
                request.occurrence
        }
}

// [VXG RealForever][VEXSTREAM][QUEUE][PLAYER-PARTNER]
