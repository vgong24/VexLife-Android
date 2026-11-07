package com.vextreme.vexlife.android.vexstream.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController

/** Compose reads Queue Family State here and sends Family Controls to the provider bridge. */
internal interface VexStreamQueueExperience {
    val state:
        VexStreamQueueFamily.State?

    fun activate(
        control:
            VexStreamQueueFamily.Control,
    )
}

private class CurrentVexStreamQueueExperience(
    override val state:
        VexStreamQueueFamily.State?,
    private val connection:
        Media3VexStreamQueueConnection?,
) :
    VexStreamQueueExperience {
    override fun activate(
        control:
            VexStreamQueueFamily.Control,
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
):
    VexStreamQueueExperience {
    val connection =
        remember(
            controller,
        ) {
            controller?.let(
                ::Media3VexStreamQueueConnection,
            )
        }

    DisposableEffect(
        connection,
    ) {
        onDispose {
            connection?.close()
        }
    }

    val state:
        VexStreamQueueFamily.State? =
        if (
            connection == null
        ) {
            null
        } else {
            val observed by
                connection.states
                    .collectAsStateWithLifecycle()
            observed
        }

    return remember(
        connection,
        state,
    ) {
        CurrentVexStreamQueueExperience(
            state =
                state,
            connection =
                connection,
        )
    }
}

// [VXG RealForever][VEXSTREAM][QUEUE][EXPERIENCE]
