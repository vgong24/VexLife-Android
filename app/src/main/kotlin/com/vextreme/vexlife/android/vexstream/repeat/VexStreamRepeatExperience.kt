package com.vextreme.vexlife.android.vexstream.repeat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController

/** Compose reads Repeat Family State here and sends Family Controls back to the provider bridge. */
internal interface VexStreamRepeatExperience {
    val state:
        VexStreamRepeatFamily.State?

    fun activate(
        control:
            VexStreamRepeatFamily.Control,
    )
}

private class CurrentVexStreamRepeatExperience(
    override val state:
        VexStreamRepeatFamily.State?,
    private val connection:
        Media3VexStreamRepeatConnection?,
) :
    VexStreamRepeatExperience {
    override fun activate(
        control:
            VexStreamRepeatFamily.Control,
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
):
    VexStreamRepeatExperience {
    val connection =
        remember(
            controller,
        ) {
            controller?.let(
                ::Media3VexStreamRepeatConnection,
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
        VexStreamRepeatFamily.State? =
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
        CurrentVexStreamRepeatExperience(
            state =
                state,
            connection =
                connection,
        )
    }
}

// [VXG RealForever][VEXSTREAM][REPEAT][EXPERIENCE]
