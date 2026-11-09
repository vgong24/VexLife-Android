package com.vextreme.vexlife.android.vexstream.queue

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerConnectionViewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.queueMoveTargetIndex

internal typealias VexStreamQueueReorderDirection =
    VexStreamPlayerPartner.QueueMove

internal data class VexStreamQueueReorderRequest(
    val occurrence:
        VexStreamQueueOccurrence,
    val direction:
        VexStreamQueueReorderDirection,
)

@Composable
internal fun rememberVexStreamQueueReorderConnection():
    VexStreamPlayerPartner.Connection.Request? {
    val owner:
        VexStreamPlayerConnectionViewModel =
        viewModel()

    return owner.playerRequestConnection
}

internal fun vexStreamQueueReorderTargetIndex(
    request:
        VexStreamQueueReorderRequest,
    currentMediaIds:
        List<
            String
        >,
    changeAvailable:
        Boolean,
):
    Int? =
    queueMoveTargetIndex(
        occurrence =
            request.occurrence,
        move =
            request.direction,
        currentMediaIds =
            currentMediaIds,
        changeAvailable =
            changeAvailable,
    )

internal fun vexStreamQueueAdjacentMoveTarget(
    currentIndex:
        Int,
    dragOffsetPx:
        Float,
    itemHeightPx:
        Int,
    lastIndex:
        Int,
):
    Int? {
    if (
        currentIndex !in
        0..lastIndex ||
        itemHeightPx <=
        0
    ) {
        return null
    }

    val threshold =
        itemHeightPx
            .toFloat() /
            2f

    return when {
        dragOffsetPx >
            threshold &&
            currentIndex <
            lastIndex ->
            currentIndex +
                1

        dragOffsetPx <
            -threshold &&
            currentIndex >
            0 ->
            currentIndex -
                1

        else ->
            null
    }
}

// [VXG RealForever][VEXSTREAM][QUEUE][PLAYER-PARTNER]
