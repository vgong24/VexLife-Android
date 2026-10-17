package com.vextreme.vexlife.android.vexstream.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import androidx.media3.session.MediaController

internal enum class VexStreamQueueReorderDirection {
    Earlier,
    Later,
}

/**
 * Request one adjacent move for one exact current queue occurrence.
 *
 * Reorder deliberately uses the current [VexStreamQueueOccurrence] coordinate instead of inventing a
 * durable item identity that Media3 does not expose here.
 */
internal data class VexStreamQueueReorderRequest(
    val occurrence: VexStreamQueueOccurrence,
    val direction: VexStreamQueueReorderDirection,
)

/**
 * Payload boundary for one admitted adjacent Queue reorder mutation.
 *
 * Every move re-reads provider queue truth. A successful return means the exact request passed the
 * currentness/capability gates and the Media3 mutation was dispatched; it does not claim a later
 * provider callback has already converged.
 */
internal interface VexStreamQueueReorderConnection {
    val isAvailable: Boolean

    fun move(
        request: VexStreamQueueReorderRequest,
    ): Boolean
}

@Composable
internal fun rememberVexStreamQueueReorderConnection(
    controller: MediaController?,
): VexStreamQueueReorderConnection? =
    remember(
        controller,
    ) {
        controller?.let {
            Media3VexStreamQueueReorderConnection(
                it,
            )
        }
    }

internal class Media3VexStreamQueueReorderConnection(
    private val controller: MediaController,
) :
    VexStreamQueueReorderConnection {
    override val isAvailable: Boolean
        get() =
            controller.mediaItemCount > 1 &&
                controller.isCommandAvailable(
                    Player.COMMAND_CHANGE_MEDIA_ITEMS,
                )

    override fun move(
        request: VexStreamQueueReorderRequest,
    ): Boolean {
        val currentMediaIds =
            (0 until controller.mediaItemCount)
                .map { index ->
                    controller
                        .getMediaItemAt(
                            index,
                        ).mediaId
                }

        val targetIndex =
            vexStreamQueueReorderTargetIndex(
                request = request,
                currentMediaIds =
                    currentMediaIds,
                changeAvailable =
                    controller.isCommandAvailable(
                        Player.COMMAND_CHANGE_MEDIA_ITEMS,
                    ),
            )
                ?: return false

        controller.moveMediaItem(
            request.occurrence.index,
            targetIndex,
        )
        return true
    }
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
            mediaIds = currentMediaIds,
            index =
                request.occurrence.index,
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

    /*
     * Media3 exposes media IDs here, not a stronger durable queue-item identity. Crossing two
     * adjacent identical IDs would leave the provider projection indistinguishable, so refuse to
     * claim we know which duplicate moved.
     */
    if (
        currentMediaIds[targetIndex] ==
        source.mediaId
    ) {
        return null
    }

    return targetIndex
}

/**
 * Returns the adjacent queue index crossed by one drag gesture sample.
 *
 * This is presentation geometry only. Provider/currentness authority remains in
 * [VexStreamQueueReorderConnection].
 */
internal fun vexStreamQueueAdjacentMoveTarget(
    currentIndex: Int,
    dragOffsetPx: Float,
    itemHeightPx: Int,
    lastIndex: Int,
): Int? {
    if (
        currentIndex !in 0..lastIndex ||
        itemHeightPx <= 0
    ) {
        return null
    }

    val threshold =
        itemHeightPx.toFloat() / 2f

    return when {
        dragOffsetPx > threshold &&
            currentIndex < lastIndex ->
            currentIndex + 1

        dragOffsetPx < -threshold &&
            currentIndex > 0 ->
            currentIndex - 1

        else -> null
    }
}

// [VXG RealForever]
