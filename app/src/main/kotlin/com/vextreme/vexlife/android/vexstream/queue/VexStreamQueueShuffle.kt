package com.vextreme.vexlife.android.vexstream.queue

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlin.random.Random

/**
 * Whether a one-shot future-queue shuffle can change the current provider queue.
 *
 * Shuffle here is not Media3 persistent shuffle mode. Already-played items plus the current item
 * remain fixed; at least two future items and playlist-mutation authority are required.
 */
internal fun vexStreamQueueShuffleAvailable(
    mediaItemCount: Int,
    currentIndex: Int,
    canChangeMediaItems: Boolean,
): Boolean {
    require(
        mediaItemCount >= 0,
    ) {
        "mediaItemCount must not be negative"
    }

    if (
        !canChangeMediaItems ||
        mediaItemCount <= 1
    ) {
        return false
    }

    val lockedThrough =
        currentIndex.coerceIn(
            -1,
            mediaItemCount - 1,
        )
    val futureStart =
        (lockedThrough + 1)
            .coerceIn(
                0,
                mediaItemCount,
            )

    return mediaItemCount - futureStart > 1
}

/**
 * Forms one visible queue shuffle without enabling Media3's persistent shuffle traversal.
 *
 * Already-played items and the current item stay fixed. Only the future queue is randomized.
 * If randomness produces the same order, rotate once so an explicit reshuffle is perceptible when
 * at least two future items exist.
 */
internal fun vexStreamQueueShuffleTargetOrder(
    itemCount: Int,
    currentIndex: Int,
    random: Random = Random.Default,
): List<Int> {
    if (itemCount <= 0) {
        return emptyList()
    }

    val lockedThrough =
        currentIndex.coerceIn(
            -1,
            itemCount - 1,
        )
    val prefix =
        if (lockedThrough >= 0) {
            (0..lockedThrough).toList()
        } else {
            emptyList()
        }
    val future =
        ((lockedThrough + 1) until itemCount)
            .toList()

    if (future.size <= 1) {
        return prefix + future
    }

    val randomized =
        future.shuffled(
            random,
        )
    val changed =
        if (randomized == future) {
            randomized.drop(1) +
                randomized.first()
        } else {
            randomized
        }

    return prefix + changed
}

internal fun MediaController.shuffleVexStreamFutureQueue():
    Boolean {
    val count =
        mediaItemCount
    val currentIndex =
        currentMediaItemIndex

    if (
        !vexStreamQueueShuffleAvailable(
            mediaItemCount = count,
            currentIndex = currentIndex,
            canChangeMediaItems =
                isCommandAvailable(
                    Player
                        .COMMAND_CHANGE_MEDIA_ITEMS,
                ),
        )
    ) {
        return false
    }

    val lockedThrough =
        currentIndex.coerceIn(
            -1,
            count - 1,
        )
    val futureStart =
        (lockedThrough + 1)
            .coerceIn(
                0,
                count,
            )
    val targetOrder =
        vexStreamQueueShuffleTargetOrder(
            itemCount = count,
            currentIndex = currentIndex,
        )
    val shuffledFuture =
        targetOrder
            .drop(
                futureStart,
            )
            .map(
                ::getMediaItemAt,
            )

    shuffleModeEnabled = false
    replaceMediaItems(
        futureStart,
        count,
        shuffledFuture,
    )
    return true
}

// [VXG RealForever]
