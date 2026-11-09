package com.vextreme.vexlife.android.vexstream.queue

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerConnectionViewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.queuePlayTargetIndex

internal data class VexStreamQueuePlayRequest(
    val occurrence:
        VexStreamQueueOccurrence,
)

@Composable
internal fun rememberVexStreamQueuePlayConnection():
    VexStreamPlayerPartner.Connection.Request? {
    val owner:
        VexStreamPlayerConnectionViewModel =
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
