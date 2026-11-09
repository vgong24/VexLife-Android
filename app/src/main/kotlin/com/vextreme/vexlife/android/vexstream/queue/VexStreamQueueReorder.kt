package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.queueMoveTargetIndex

internal typealias VexStreamQueueReorderDirection =
    VexStreamPlayerPartner.QueueMove

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
