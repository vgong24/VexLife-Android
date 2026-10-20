package com.vextreme.vexlife.android.vexstream.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueShuffleTargetOrder
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackMediaItemResolver
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
 * Feature-local payload boundary for rendered collection -> Media3 playback.
 *
 * Availability is a cheap projection only. Activation resolves media again, filters unavailable
 * sources, rechecks the provider commands actually used, then replaces the provider playlist.
 */
internal interface VexStreamCollectionPlaybackConnection {
    fun isAvailable(
        request: VexStreamCollectionPlaybackRequest,
    ): Boolean

    fun play(
        request: VexStreamCollectionPlaybackRequest,
    ): Boolean
}

@Composable
internal fun rememberVexStreamCollectionPlaybackConnection(
    controller: MediaController?,
    resolver: VexStreamTrackMediaItemResolver?,
): VexStreamCollectionPlaybackConnection? =
    remember(
        controller,
        resolver,
    ) {
        if (
            controller == null ||
            resolver == null
        ) {
            null
        } else {
            Media3VexStreamCollectionPlaybackConnection(
                controller = controller,
                resolver = resolver,
            )
        }
    }

internal class Media3VexStreamCollectionPlaybackConnection(
    private val controller: MediaController,
    private val resolver: VexStreamTrackMediaItemResolver,
) :
    VexStreamCollectionPlaybackConnection {
    override fun isAvailable(
        request: VexStreamCollectionPlaybackRequest,
    ): Boolean =
        vexStreamCollectionSourceProjectedAvailable(
            request.tracks,
        ) &&
            commandsAvailable()

    override fun play(
        request: VexStreamCollectionPlaybackRequest,
    ): Boolean {
        val items =
            request.tracks
                .mapNotNull(
                    resolver::mediaItem,
                )
        if (
            items.isEmpty() ||
            !commandsAvailable()
        ) {
            return false
        }

        val targetOrder =
            vexStreamCollectionPlaybackTargetOrder(
                itemCount = items.size,
                order = request.order,
            )
        val orderedItems =
            targetOrder.map(
                items::get,
            )

        controller.setMediaItems(
            orderedItems,
        )

        if (
            controller.shuffleModeEnabled
        ) {
            controller.shuffleModeEnabled =
                false
        }

        controller.prepare()
        controller.play()
        return true
    }

    private fun commandsAvailable(): Boolean =
        vexStreamCollectionPlaybackCommandsAvailable(
            changeMediaItemsAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_CHANGE_MEDIA_ITEMS,
                ),
            prepareAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_PREPARE,
                ),
            playAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_PLAY_PAUSE,
                ),
            shuffleEnabled =
                controller.shuffleModeEnabled,
            setShuffleModeAvailable =
                controller.isCommandAvailable(
                    Player.COMMAND_SET_SHUFFLE_MODE,
                ),
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

internal fun vexStreamCollectionPlaybackCommandsAvailable(
    changeMediaItemsAvailable: Boolean,
    prepareAvailable: Boolean,
    playAvailable: Boolean,
    shuffleEnabled: Boolean,
    setShuffleModeAvailable: Boolean,
): Boolean =
    changeMediaItemsAvailable &&
        prepareAvailable &&
        playAvailable &&
        (
            !shuffleEnabled ||
                setShuffleModeAvailable
        )

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

        VexStreamCollectionPlaybackOrder.Shuffled ->
            vexStreamQueueShuffleTargetOrder(
                itemCount = itemCount,
                currentIndex = -1,
                random = random,
            )
    }
}

// [VXG RealForever]
