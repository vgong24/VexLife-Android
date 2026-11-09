package com.vextreme.vexlife.android.vexstream.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerConnectionViewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore

/**
 * Compose-facing membrane over Queue semantic meaning plus the exact retained Queue Store payload.
 *
 * Family owns Queue Control meaning. Player Store owns full ordered queue truth. This membrane owns
 * neither; it only keeps both rightful sources available to presentation.
 */
internal interface VexStreamQueueExperience {
    val projection:
        VexStreamQueueProjection

    val current:
        VexStreamPlayerStore.QueueSnapshot?

    fun activate(
        control:
            VexStreamQueueControl,
    )
}

private class CurrentVexStreamQueueExperience(
    override val projection:
        VexStreamQueueProjection,
    override val current:
        VexStreamPlayerStore.QueueSnapshot?,
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
        VexStreamPlayerConnectionViewModel =
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
    val queueCurrent by
        owner
            .playerStore
            .queueCurrents
            .collectAsStateWithLifecycle(
                initialValue =
                    owner.playerStore.currentQueue,
            )
    val projection =
        remember(
            state,
        ) {
            family.projectionFor(
                state,
            )
        }
    val connection =
        owner.playerRequestConnection

    return remember(
        projection,
        queueCurrent,
        family,
        connection,
        owner.executionRoot,
    ) {
        CurrentVexStreamQueueExperience(
            projection =
                projection,
            current =
                queueCurrent,
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
