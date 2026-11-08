package com.vextreme.vexlife.android.vexstream.playback

import android.app.Application
import android.content.ComponentName
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.VexApplication
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService

/**
 * Android configuration-lifetime owner for the concrete Playback Connection.
 *
 * The ViewModel owns only the MediaController connection lifetime. Playback semantic truth belongs
 * to the process-shared [VexStreamPlaybackFamily] resolved from [VexApplication].
 */
internal class VexStreamPlaybackConnectionViewModel(
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

    private val executionRoot =
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
        )
            .setApplicationLooper(
                Looper.getMainLooper(),
            )
            .buildAsync()

    var controller by
        mutableStateOf<
            MediaController?
        >(
            null,
        )
        private set

    var connection by
        mutableStateOf<
            VexStreamPlaybackConnection?
        >(
            null,
        )
        private set

    init {
        controllerFuture.addListener(
            {
                runCatching {
                    controllerFuture.get()
                }.onSuccess { connected ->
                    if (
                        !cleared
                    ) {
                        connection =
                            Media3VexStreamPlaybackConnection(
                                controller =
                                    connected,
                                family =
                                    family,
                                executionRoot =
                                    executionRoot,
                            )
                        controller =
                            connected
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
        connection = null
        controller = null
        MediaController.releaseFuture(
            controllerFuture,
        )
        super.onCleared()
    }
}

@Composable
internal fun rememberVexStreamMediaController():
    MediaController? {
    val owner:
        VexStreamPlaybackConnectionViewModel =
        viewModel()

    return owner.controller
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][PROCESS-FAMILY]
