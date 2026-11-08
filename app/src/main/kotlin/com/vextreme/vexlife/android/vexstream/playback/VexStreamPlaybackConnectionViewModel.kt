package com.vextreme.vexlife.android.vexstream.playback

import android.app.Application
import android.content.ComponentName
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
import com.vextreme.vexlife.android.vexstream.player.Media3VexStreamPlayerControlConnection
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import com.vextreme.vexlife.android.vexstream.queue.PartnerVexStreamQueueConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueConnection
import com.vextreme.vexlife.android.vexstream.repeat.PartnerVexStreamRepeatConnection
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatConnection

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

    val playerStore:
        VexStreamPlayerStore =
        vexApplication
            .dependencies
            .resolve(
                vexApplication
                    .dependencyModule
                    .playerStore,
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

    var connection by
        mutableStateOf<
            VexStreamPlaybackConnection?
        >(
            null,
        )
        private set

    var queueConnection by
        mutableStateOf<
            VexStreamQueueConnection?
        >(
            null,
        )
        private set

    var repeatConnection by
        mutableStateOf<
            VexStreamRepeatConnection?
        >(
            null,
        )
        private set

    var seekConnection by
        mutableStateOf<
            VexStreamPlaybackSeekConnection?
        >(
            null,
        )
        private set

    var progressConnection by
        mutableStateOf<
            VexStreamPlayerPartner.Connection.Progress?
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
                        val playerConnection =
                            Media3VexStreamPlayerControlConnection(
                                connected,
                            )

                        connection =
                            PartnerVexStreamPlaybackConnection(
                                family =
                                    family,
                                effectConnection =
                                    playerConnection,
                            )
                        queueConnection =
                            PartnerVexStreamQueueConnection(
                                store =
                                    playerStore,
                                effectConnection =
                                    playerConnection,
                            )
                        repeatConnection =
                            PartnerVexStreamRepeatConnection(
                                store =
                                    playerStore,
                                effectConnection =
                                    playerConnection,
                            )
                        seekConnection =
                            playerConnection
                        progressConnection =
                            playerConnection
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
        queueConnection = null
        repeatConnection = null
        seekConnection = null
        progressConnection = null
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
