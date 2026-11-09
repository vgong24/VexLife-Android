package com.vextreme.vexlife.android.vexstream

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
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.player.Media3ControllerConnection
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueFamily
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatFamily

/**
 * Android configuration-lifetime composition owner for the VexStream Music experience.
 *
 * OWNED HERE
 * One Media3 Controller Connection occurrence and its Android configuration lifetime.
 *
 * BORROWED, NOT OWNED
 * The process-shared Player Store, Playback / Queue / Repeat Families and VexExecutionRoot.
 *
 * This is an Android composition boundary, not another semantic architecture root. It exists because
 * the Music experience needs one configuration-stable Controller connection while its semantic and
 * outside truths remain owned by their established process-level owners.
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
                "VexStream Playback requires VexApplication as the process composition root",
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


@Composable
internal fun rememberVexStreamPlayerRequestConnection():
    VexStreamPlayerPartner.Connection.Request? {
    val owner:
        VexStreamMusicViewModel =
        viewModel()

    return owner.playerRequestConnection
}

// [VXG RealForever][VEXSTREAM][MUSIC][CONFIGURATION-COMPOSITION]
