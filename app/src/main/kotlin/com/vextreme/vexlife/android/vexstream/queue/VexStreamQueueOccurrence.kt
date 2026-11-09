package com.vextreme.vexlife.android.vexstream.queue

import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.vexStreamPlayerQueueOccurrenceAt
import com.vextreme.vexlife.android.vexstream.player.vexStreamPlayerQueueOccurrences

internal typealias VexStreamQueueOccurrence =
    VexStreamPlayerPartner.QueueOccurrence

internal fun vexStreamQueueOccurrences(
    mediaIds:
        List<
            String
        >,
):
    List<
        VexStreamQueueOccurrence
    > =
    vexStreamPlayerQueueOccurrences(
        mediaIds,
    )

internal fun vexStreamQueueOccurrenceAt(
    mediaIds:
        List<
            String
        >,
    index:
        Int,
):
    VexStreamQueueOccurrence? =
    vexStreamPlayerQueueOccurrenceAt(
        mediaIds =
            mediaIds,
        index =
            index,
    )

// [VXG RealForever][VEXSTREAM][QUEUE][PLAYER-PARTNER]
