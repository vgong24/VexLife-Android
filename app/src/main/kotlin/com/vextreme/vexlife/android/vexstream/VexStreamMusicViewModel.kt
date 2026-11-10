package com.vextreme.vexlife.android.vexstream

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
import com.vextreme.vexlife.android.vexstream.player.Media3ControllerConnection
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueFamily
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatFamily

/**
 * Android configuration-lifetime composition owner for VexStream Music.
 *
 * One MediaController occurrence belongs here. Process-shared Playback / Queue / Repeat Families and
 * their execution root are borrowed from the process dependency lifetime; this ViewModel does not
 * become another semantic or provider-truth owner.
 */
internal class VexStreamMusicViewModel(
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
                "VexStream requires VexApplication as the process composition root",
            )

    val playbackFamily:
        VexStreamPlaybackFamily =
        vexApplication
            .dependencies
            .resolve(
                vexApplication
                    .dependencyModule
                    .playbackFamily,
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

    val executionRoot =
        vexApplication
            .dependencies
            .resolve(
                vexApplication
                    .dependencyModule
                    .processExecutionRoot,
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

    var controller by
        mutableStateOf<
            MediaController?
        >(
            null,
        )
        private set

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

    init {
        controllerFuture.addListener(
            {
                runCatching {
                    controllerFuture.get()
                }.onSuccess { connected ->
                    if (
                        !cleared
                    ) {
                        controller =
                            connected
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
        controller = null
        MediaController.releaseFuture(
            controllerFuture,
        )
        super.onCleared()
    }
}

// [VXG RealForever][VEXSTREAM][MUSIC][CONFIGURATION-COMPOSITION]
