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
import com.vextreme.vexlife.android.behavior.VexObservableBehaviorConnection
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService

/**
 * Android configuration-lifetime host for the service-owned MediaController and its semantic
 * Behavior connection.
 *
 * ViewModel is only the Android retention mechanism. MediaSession/Player remain playback truth
 * owners; [Media3VexStreamPlaybackConnection] remains the semantic projection/effect adapter.
 */
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

    var behaviorConnection by
        mutableStateOf<
            VexObservableBehaviorConnection<
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
                        val connection =
                            Media3VexStreamPlaybackConnection(
                                connected,
                            )
                        media3Connection =
                            connection
                        behaviorConnection =
                            connection
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
        behaviorConnection = null
        media3Connection?.close()
        media3Connection = null
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
        VexStreamPlaybackConnectionViewModel =
        viewModel()

    return owner.controller
}

/**
 * Retained two-way semantic Playback connection exposed only through the reusable contract.
 */
@Composable
internal fun rememberVexStreamPlaybackBehaviorConnection():
    VexObservableBehaviorConnection<
        VexStreamPlaybackState,
        VexStreamPlaybackControl,
        VexStreamPlaybackAct
    >? {
    val owner:
        VexStreamPlaybackConnectionViewModel =
        viewModel()

    return owner.behaviorConnection
}

/**
 * Lifecycle-aware Android presentation adapter for one observable Behavior connection.
 */
@Composable
internal fun rememberVexStreamPlaybackBehaviorProjection(
    connection:
        VexObservableBehaviorConnection<
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
        connection?.projections
            ?: return null

    val current by
        projections.collectAsStateWithLifecycle()

    return current
}

// [VXG RealForever]
