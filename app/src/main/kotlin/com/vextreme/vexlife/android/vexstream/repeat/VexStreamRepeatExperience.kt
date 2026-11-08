package com.vextreme.vexlife.android.vexstream.repeat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackConnectionViewModel
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner

/** Compose-facing membrane over retained Player Partner Repeat truth and Repeat Controls. */
internal interface VexStreamRepeatExperience {
    val projection:
        VexStreamRepeatProjection

    fun activate(
        control:
            VexStreamRepeatControl,
    )
}

private class CurrentVexStreamRepeatExperience(
    override val projection:
        VexStreamRepeatProjection,
    private val connection:
        VexStreamRepeatConnection?,
) :
    VexStreamRepeatExperience {
    override fun activate(
        control:
            VexStreamRepeatControl,
    ) {
        connection?.activate(
            control,
        )
    }
}

@Composable
internal fun rememberVexStreamRepeatExperience():
    VexStreamRepeatExperience {
    val owner:
        VexStreamPlaybackConnectionViewModel =
        viewModel()
    val repeat by
        owner
            .playerStore
            .repeatSignals
            .collectAsStateWithLifecycle(
                initialValue =
                    (
                        owner.playerStore.current as?
                            VexStreamPlayerPartner.Current.Available
                    )
                        ?.repeat,
            )
    val projection =
        remember(
            repeat,
        ) {
            repeat
                .toVexStreamRepeatProjection()
        }
    val connection =
        owner.repeatConnection

    return remember(
        projection,
        connection,
    ) {
        CurrentVexStreamRepeatExperience(
            projection =
                projection,
            connection =
                connection,
        )
    }
}

// [VXG RealForever][VEXSTREAM][REPEAT][PLAYER-PARTNER]
