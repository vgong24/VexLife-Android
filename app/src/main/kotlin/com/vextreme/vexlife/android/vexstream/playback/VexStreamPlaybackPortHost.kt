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

/**
 * Android configuration-lifetime host for the service-owned MediaController and semantic VexPort.
 *
 * ViewModel is only the Android retention mechanism. MediaSession/Player remain playback truth
 * owners; [Media3VexStreamPlaybackPort] remains the semantic projection/effect adapter.
 */
internal class VexStreamPlaybackPortViewModel(
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

    private var media3Port:
        Media3VexStreamPlaybackPort? =
        null

    var port by
        mutableStateOf<
            VexPortFlow<
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
                        val connectedPort =
                            Media3VexStreamPlaybackPort(
                                connected,
                            )
                        media3Port =
                            connectedPort
                        port =
                            connectedPort
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
        port = null
        media3Port?.close()
        media3Port = null
        controller = null
        MediaController.releaseFuture(
            controllerFuture,
        )
        super.onCleared()
    }
}

/**
 * Retained Media3 client for queue/progress operations that have not migrated into Behavior yet.
 */
@Composable
internal fun rememberVexStreamMediaController():
    MediaController? {
    val owner:
        VexStreamPlaybackPortViewModel =
        viewModel()

    return owner.controller
}

/**
 * Retained two-way semantic Playback Port exposed only through the reusable contract.
 */
@Composable
internal fun rememberVexStreamPlaybackPort():
    VexPortFlow<
        VexStreamPlaybackState,
        VexStreamPlaybackControl,
        VexStreamPlaybackAct
    >? {
    val owner:
        VexStreamPlaybackPortViewModel =
        viewModel()

    return owner.port
}

/**
 * Lifecycle-aware Android presentation adapter for one live VexPortFlow.
 */
@Composable
internal fun rememberVexStreamPlaybackProjection(
    port:
        VexPortFlow<
            VexStreamPlaybackState,
            VexStreamPlaybackControl,
            VexStreamPlaybackAct
        >?,
): VexBehaviorProjection<
    VexStreamPlaybackState,
    VexStreamPlaybackControl,
    VexStreamPlaybackAct
>? {
    val projections =
        port?.projections
            ?: return null

    val current by
        projections.collectAsStateWithLifecycle()

    return current
}

// [VXG RealForever]
