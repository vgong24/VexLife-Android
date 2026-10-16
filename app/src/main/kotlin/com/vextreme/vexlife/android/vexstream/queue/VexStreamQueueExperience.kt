package com.vextreme.vexlife.android.vexstream.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.session.MediaController

/**
 * Compose-facing membrane for the VexStream neighboring-item Queue behavior.
 *
 * Queue does not currently need an additional DisplayState classification: the Behavior Projection
 * already answers the view question used by Mini Player and Context Dock. This boundary owns no
 * Queue truth; it observes the current projection and carries Controls back to the Connection.
 */
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
internal fun rememberVexStreamQueueExperience(
    controller:
        MediaController?,
): VexStreamQueueExperience {
    val connection =
        remember(
            controller,
        ) {
            controller?.let {
                Media3VexStreamQueueConnection(
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
        CurrentVexStreamQueueExperience(
            projection =
                projection,
            connection =
                connection,
        )
    }
}

// [VXG RealForever]
