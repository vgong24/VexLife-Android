package com.vextreme.vexlife.android.vexstream.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackConnectionViewModel

/**
 * Compose-facing membrane for Queue semantic meaning.
 *
 * The Player Store owns retained outside CURRENT; this Experience observes its bounded Queue
 * projection and carries Controls to the semantic Queue Connection. It owns no provider listener.
 */
internal interface VexStreamQueueExperience {
    val projection:
        VexStreamQueueProjection

    fun activate(
        control:
            VexStreamQueueControl,
    )
}

private class CurrentVexStreamQueueExperience(
    override val projection:
        VexStreamQueueProjection,
    private val connection:
        VexStreamQueueConnection?,
) :
    VexStreamQueueExperience {
    override fun activate(
        control:
            VexStreamQueueControl,
    ) {
        connection?.activate(
            control,
        )
    }
}

@Composable
internal fun rememberVexStreamQueueExperience():
    VexStreamQueueExperience {
    val owner:
        VexStreamPlaybackConnectionViewModel =
        viewModel()
    val queueCurrent by
        owner
            .playerStore
            .queueCurrents
            .collectAsStateWithLifecycle(
                initialValue =
                    owner
                        .playerStore
                        .currentQueue,
            )
    val projection =
        remember(
            queueCurrent,
        ) {
            queueCurrent
                .toVexStreamQueueProjection()
        }
    val connection =
        owner.queueConnection

    return remember(
        projection,
        connection,
    ) {
        CurrentVexStreamQueueExperience(
            projection =
                projection,
            connection =
                connection,
        )
    }
}

// [VXG RealForever][VEXSTREAM][QUEUE][PLAYER-PARTNER]
