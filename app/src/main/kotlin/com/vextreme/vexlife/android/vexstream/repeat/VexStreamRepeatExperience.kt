package com.vextreme.vexlife.android.vexstream.repeat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.session.MediaController

/** Compose-facing membrane for the VexStream Repeat Behavior family. */
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
internal fun rememberVexStreamRepeatExperience(
    controller:
        MediaController?,
): VexStreamRepeatExperience {
    val connection =
        remember(
            controller,
        ) {
            controller?.let {
                Media3VexStreamRepeatConnection(
                    it,
                )
            }
        }

    var projection by
        remember(
            connection,
        ) {
            mutableStateOf(
                connection
                    ?.currentProjection,
            )
        }

    DisposableEffect(
        controller,
        connection,
    ) {
        if (
            controller == null ||
            connection == null
        ) {
            projection = null
            return@DisposableEffect onDispose {}
        }

        val listener =
            object :
                Player.Listener {
                override fun onEvents(
                    player: Player,
                    events:
                        Player.Events,
                ) {
                    projection =
                        connection
                            .currentProjection
                }
            }

        controller.addListener(
            listener,
        )
        projection =
            connection
                .currentProjection

        onDispose {
            controller.removeListener(
                listener,
            )
        }
    }

    return remember(
        connection,
        projection,
    ) {
        CurrentVexStreamRepeatExperience(
            projection =
                projection,
            connection =
                connection,
        )
    }
}

// [VXG RealForever]
