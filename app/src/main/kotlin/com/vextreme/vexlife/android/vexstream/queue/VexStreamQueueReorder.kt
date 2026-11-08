package com.vextreme.vexlife.android.vexstream.queue

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackConnectionViewModel

internal enum class VexStreamQueueReorderDirection {
    Earlier,
    Later,
}

internal data class VexStreamQueueReorderRequest(
    val occurrence:
        VexStreamQueueOccurrence,
    val direction:
        VexStreamQueueReorderDirection,
)

internal interface VexStreamQueueReorderConnection {
    val isReorderAvailable:
        Boolean

    fun move(
        request:
            VexStreamQueueReorderRequest,
    ):
        Boolean
}

@Composable
internal fun rememberVexStreamQueueReorderConnection():
    VexStreamQueueReorderConnection? {
    val owner:
        VexStreamPlaybackConnectionViewModel =
        viewModel()

    return owner.queueReorderConnection
}

internal fun vexStreamQueueReorderTargetIndex(
    request: VexStreamQueueReorderRequest,
    currentMediaIds: List<String>,
    changeAvailable: Boolean,
): Int? {
    if (
        !changeAvailable
    ) {
        return null
    }

    val source =
        vexStreamQueueOccurrenceAt(
            mediaIds =
                currentMediaIds,
            index =
                request
                    .occurrence
                    .index,
        )
            ?: return null

    if (
        source !=
        request.occurrence
    ) {
        return null
    }

    val targetIndex =
        when (
            request.direction
        ) {
            VexStreamQueueReorderDirection.Earlier ->
                source.index - 1

            VexStreamQueueReorderDirection.Later ->
                source.index + 1
        }

    if (
        targetIndex !in
        currentMediaIds.indices
    ) {
        return null
    }

    if (
        currentMediaIds[
            targetIndex
        ] ==
        source.mediaId
    ) {
        return null
    }

    return targetIndex
}

internal fun vexStreamQueueAdjacentMoveTarget(
    currentIndex: Int,
    dragOffsetPx: Float,
    itemHeightPx: Int,
    lastIndex: Int,
): Int? {
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
            currentIndex + 1

        dragOffsetPx <
            -threshold &&
            currentIndex >
            0 ->
            currentIndex - 1

        else ->
            null
    }
}

// [VXG RealForever][VEXSTREAM][QUEUE][PLAYER-PARTNER]
