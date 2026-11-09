package com.vextreme.vexlife.android.vexstream.player

import android.app.Application
import android.content.ComponentName
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.VexApplication
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueFamily
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatFamily

/**
 * Android configuration-lifetime owner for one concrete Player Partner request/progress Connection.
 *
 * Media3 connection construction/lifetime stays in the Player Partner neighborhood. Playback,
 * Queue and Repeat remain separate semantic owners that receive Vex-owned connection surfaces.
 */
internal class VexStreamPlayerConnectionViewModel(
    application:
        Application,
) :
    AndroidViewModel(
        application,
    ) {
    private var cleared =
        false

    private val vexApplication =
        application as?
            VexApplication
            ?: error(
                "VexStream Playback requires VexApplication as the process composition root",
            )

    val family:
        VexStreamPlaybackFamily =
        vexApplication
            .dependencies
            .resolve(
                vexApplication
                    .dependencyModule
                    .playbackFamily,
            )

    val executionRoot =
        vexApplication
            .dependencies
            .resolve(
                vexApplication
                    .dependencyModule
                    .processExecutionRoot,
            )

    val playerStore:
        VexStreamPlayerStore =
        vexApplication
            .dependencies
            .resolve(
                vexApplication
                    .dependencyModule
                    .playerStore,
            )

    val queueFamily:
        VexStreamQueueFamily =
        vexApplication
            .dependencies
            .resolve(
                vexApplication
                    .dependencyModule
                    .queueFamily,
            )

    val repeatFamily:
        VexStreamRepeatFamily =
        vexApplication
            .dependencies
            .resolve(
                vexApplication
                    .dependencyModule
                    .repeatFamily,
            )

    private val controllerFuture =
        MediaController.Builder(
            application,
            SessionToken(
                application,
                ComponentName(
                    application,
                    VexStreamPlaybackService::class.java,
                ),
            ),
        ).buildAsync()

    private var playerConnection by
        mutableStateOf<
            Media3ControllerConnection?
        >(
            null,
        )

    val playerRequestConnection:
        VexStreamPlayerPartner.Connection.Request?
        get() =
            playerConnection

    val progressConnection:
        VexStreamPlayerPartner.Connection.Progress?
        get() =
            playerConnection

    init {
        controllerFuture.addListener(
            {
                runCatching {
                    controllerFuture.get()
                }.onSuccess { connected ->
                    if (
                        !cleared
                    ) {
                        playerConnection =
                            Media3ControllerConnection(
                                connected,
                            )
                    }
                }
            },
            ContextCompat.getMainExecutor(
                application,
            ),
        )
    }

    override fun onCleared() {
        cleared = true
        playerConnection = null
        MediaController.releaseFuture(
            controllerFuture,
        )
        super.onCleared()
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][PROCESS-FAMILY]
