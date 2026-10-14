package com.vextreme.vexlife.android.vexstream.playback

import android.app.Application
import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.behavior.VexPortFlow
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService

internal class VexStreamPlaybackConnectionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private var cleared = false

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
        mutableStateOf<MediaController?>(
            null,
        )
        private set

    private var media3Connection:
        Media3VexStreamPlaybackConnection? =
        null

    var connection by
        mutableStateOf<
            VexPortFlow<
                VexStreamPlaybackFamily,
                VexStreamPlaybackState,
                VexStreamPlaybackControl,
                VexStreamPlaybackAct
            >?
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
                    if (!cleared) {
                        val playbackConnection =
                            Media3VexStreamPlaybackConnection(
                                connected,
                            )
                        media3Connection =
                            playbackConnection
                        connection =
                            playbackConnection
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
        media3Connection?.close()
        media3Connection = null
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

@Composable
internal fun rememberVexStreamPlaybackConnection():
    VexPortFlow<
        VexStreamPlaybackFamily,
        VexStreamPlaybackState,
        VexStreamPlaybackControl,
        VexStreamPlaybackAct
    >? {
    val owner:
        VexStreamPlaybackConnectionViewModel =
        viewModel()

    return owner.connection
}

@Composable
internal fun rememberVexStreamPlaybackProjection(
    connection:
        VexPortFlow<
            VexStreamPlaybackFamily,
            VexStreamPlaybackState,
            VexStreamPlaybackControl,
            VexStreamPlaybackAct
        >?,
): VexBehaviorProjection<
    VexStreamPlaybackFamily,
    VexStreamPlaybackState,
    VexStreamPlaybackControl,
    VexStreamPlaybackAct
>? {
    val projections =
        connection?.projections
            ?: return null

    val current by
        projections.collectAsStateWithLifecycle()

    return current
}

// [VXG RealForever]
