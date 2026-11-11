package com.vextreme.vexlife.android.vexstream.repeat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.VexStreamMusicViewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner

/** Compose-facing membrane over the one process-shared Repeat Family. */
internal interface VexStreamRepeatExperience {
    val projection:
        VexStreamRepeatProjection?

    fun activate(
        control:
            VexStreamRepeatControl,
    )
}

private class CurrentVexStreamRepeatExperience(
    override val projection:
        VexStreamRepeatProjection?,
    private val family:
        VexStreamRepeatFamily,
    private val connection:
        VexStreamPlayerPartner.Connection.Request?,
    private val executionRoot:
        VexExecutionRoot,
) :
    VexStreamRepeatExperience {
    override fun activate(
        control:
            VexStreamRepeatControl,
    ) {
        val requestConnection =
            connection
                ?: return

        activateVexStreamRepeat(
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
internal fun rememberVexStreamRepeatExperience():
    VexStreamRepeatExperience {
    val owner:
        VexStreamMusicViewModel =
        viewModel()
    val family =
        owner.repeatFamily
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
        CurrentVexStreamRepeatExperience(
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

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
