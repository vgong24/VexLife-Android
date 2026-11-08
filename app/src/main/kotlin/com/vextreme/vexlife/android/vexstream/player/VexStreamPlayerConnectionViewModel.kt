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
import com.vextreme.vexlife.android.vexstream.playback.PartnerVexStreamPlaybackConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackProgressConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackSeekConnection
import com.vextreme.vexlife.android.vexstream.queue.PartnerVexStreamQueueConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueuePlayConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueReorderConnection
import com.vextreme.vexlife.android.vexstream.repeat.PartnerVexStreamRepeatConnection
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatConnection

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

    var playerRequestConnection by
        mutableStateOf<
            VexStreamPlayerPartner.Connection.Request?
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

    var queuePlayConnection by
        mutableStateOf<
            VexStreamQueuePlayConnection?
        >(
            null,
        )
        private set

    var queueReorderConnection by
        mutableStateOf<
            VexStreamQueueReorderConnection?
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

                        playerRequestConnection =
                            playerConnection
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
                        queuePlayConnection =
                            playerConnection
                        queueReorderConnection =
                            playerConnection
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
        playerRequestConnection = null
        connection = null
        queueConnection = null
        queuePlayConnection = null
        queueReorderConnection = null
        repeatConnection = null
        seekConnection = null
        progressConnection = null
        MediaController.releaseFuture(
            controllerFuture,
        )
        super.onCleared()
    }
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][PROCESS-FAMILY]
