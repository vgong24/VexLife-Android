package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.queuePlayTargetIndex

internal data class VexStreamQueuePlayRequest(
    val occurrence:
        VexStreamQueueOccurrence,
)

internal fun rememberVexStreamQueuePlayConnection():
    VexStreamPlayerPartner.Connection.Request? {
    val owner:
        VexStreamMusicViewModel =
        viewModel()

    return owner.playerRequestConnection
}

internal fun vexStreamQueuePlayTargetIndex(
    request:
        VexStreamQueuePlayRequest,
    currentMediaIds:
        List<
            String
        >,
    seekAvailable:
        Boolean,
    playAvailable:
        Boolean,
):
    Int? =
    queuePlayTargetIndex(
        occurrence =
            request.occurrence,
        currentMediaIds =
            currentMediaIds,
        seekAvailable =
            seekAvailable,
        playAvailable =
            playAvailable,
    )

// [VXG RealForever][VEXSTREAM][QUEUE][PLAYER-PARTNER]
