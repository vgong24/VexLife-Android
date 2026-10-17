package com.vextreme.vexlife.android.vexstream.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import androidx.media3.session.MediaController

/**
 * Request to play one exact current queue occurrence.
 *
 * The request carries provider-current media/occurrence/index truth rather than only an index. This
 * lets dispatch reject stale selection after queue mutation, including duplicate-media reordering.
 */
internal data class VexStreamQueuePlayRequest(
    val occurrence: VexStreamQueueOccurrence,
)

/**
 * Payload boundary for "play this current queue occurrence now".
 *
 * Queue Play is not a finite Queue Control because the selected occurrence is runtime data. UI
 * availability is informative only; [play] re-reads the provider queue and command availability.
 */
internal interface VexStreamQueuePlayConnection {
    val isAvailable: Boolean

    fun play(
        request: VexStreamQueuePlayRequest,
    ): Boolean
}

@Composable
internal fun rememberVexStreamQueuePlayConnection(
    controller: MediaController?,
): VexStreamQueuePlayConnection? =
    remember(
        controller,
    ) {
        controller?.let {
            Media3VexStreamQueuePlayConnection(
                it,
            )
        }
    }

internal class Media3VexStreamQueuePlayConnection(
    private val controller: MediaController,
) :
    VexStreamQueuePlayConnection {
    override val isAvailable: Boolean
        get() =
            controller.isCommandAvailable(
                Player.COMMAND_SEEK_TO_MEDIA_ITEM,
            ) &&
                controller.isCommandAvailable(
                    Player.COMMAND_PLAY_PAUSE,
                )

    override fun play(
        request: VexStreamQueuePlayRequest,
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
            vexStreamQueuePlayTargetIndex(
                request = request,
                currentMediaIds =
                    currentMediaIds,
                seekAvailable =
                    controller.isCommandAvailable(
                        Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                    ),
                playAvailable =
                    controller.isCommandAvailable(
                        Player.COMMAND_PLAY_PAUSE,
                    ),
            )
                ?: return false

        if (
            targetIndex !=
            controller.currentMediaItemIndex
        ) {
            controller.seekToDefaultPosition(
                targetIndex,
            )
        }
        controller.play()
        return true
    }
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
            mediaIds = currentMediaIds,
            index =
                request.occurrence.index,
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

// [VXG RealForever]
