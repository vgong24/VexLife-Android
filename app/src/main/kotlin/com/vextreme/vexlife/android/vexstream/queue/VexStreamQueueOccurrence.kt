package com.vextreme.vexlife.android.vexstream.queue

/**
 * One current occurrence in the provider-owned ordered queue.
 *
 * This is a currentness coordinate, not durable queue-item identity. [occurrence] distinguishes
 * duplicate media IDs within one observed queue snapshot; [index] binds that occurrence to its
 * current provider position. Either may become stale after queue mutation.
 */
internal data class VexStreamQueueOccurrence(
    val mediaId: String,
    val occurrence: Int,
    val index: Int,
) {
    init {
        require(
            mediaId.isNotBlank(),
        ) {
            "queue mediaId must not be blank"
        }
        require(
            occurrence >= 1,
        ) {
            "queue occurrence must be >= 1"
        }
        require(
            index >= 0,
        ) {
            "queue index must not be negative"
        }
    }
}

internal fun vexStreamQueueOccurrences(
    mediaIds: List<String>,
): List<VexStreamQueueOccurrence> {
    val seen =
        mutableMapOf<String, Int>()

    return mediaIds.mapIndexed { index, mediaId ->
        require(
            mediaId.isNotBlank(),
        ) {
            "queue mediaId must not be blank"
        }

        val occurrence =
            (seen[mediaId] ?: 0) + 1
        seen[mediaId] = occurrence

        VexStreamQueueOccurrence(
            mediaId = mediaId,
            occurrence = occurrence,
            index = index,
        )
    }
}

internal fun vexStreamQueueOccurrenceAt(
    mediaIds: List<String>,
    index: Int,
): VexStreamQueueOccurrence? =
    if (
        index !in mediaIds.indices
    ) {
        null
    } else {
        vexStreamQueueOccurrences(
            mediaIds,
        )[index]
    }

// [VXG RealForever]
