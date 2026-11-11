package com.vextreme.vexlife.android.vexstream.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.VexStreamMusicViewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner

/** Compose-facing membrane over the one process-shared Queue Family. */
internal interface VexStreamQueueExperience {
    val projection:
        VexStreamQueueProjection?

    fun activate(
        control:
            VexStreamQueueControl,
    )
}

private class CurrentVexStreamQueueExperience(
    override val projection:
        VexStreamQueueProjection?,
    private val family:
        VexStreamQueueFamily,
    private val connection:
        VexStreamPlayerPartner.Connection.Request?,
    private val executionRoot:
        VexExecutionRoot,
) :
    VexStreamQueueExperience {
    override fun activate(
        control:
            VexStreamQueueControl,
    ) {
        val requestConnection =
            connection
                ?: return

        activateVexStreamQueue(
            family =
                family,
            control =
                control,
            connection =
                requestConnection,
            executionRoot =
                executionRoot,
        )
    }
}

@Composable
internal fun rememberVexStreamQueueExperience():
    VexStreamQueueExperience {
    val owner:
        VexStreamMusicViewModel =
        viewModel()
    val family =
        owner.queueFamily
    val state by
        family
            .states
            .collectAsStateWithLifecycle(
                initialValue =
                    family.currentState,
            )
    val connection =
        owner.playerRequestConnection
    val projection =
        remember(
            state,
            connection,
        ) {
            if (
                connection ==
                null
            ) {
                null
            } else {
                family.projectionFor(
                    state,
                )
            }
        }

    return remember(
        projection,
        family,
        connection,
        owner.executionRoot,
    ) {
        CurrentVexStreamQueueExperience(
            projection =
                projection,
            family =
                family,
            connection =
                connection,
            executionRoot =
                owner.executionRoot,
        )
    }
}

// [VXG RealForever][VEXSTREAM][QUEUE][DIRECT-FAMILY]
