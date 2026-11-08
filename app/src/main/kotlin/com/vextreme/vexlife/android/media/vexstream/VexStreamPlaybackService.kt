package com.vextreme.vexlife.android.media.vexstream

import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vextreme.vexlife.android.VexApplication
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.playback.Media3VexStreamCurrentPlaybackObserver
import com.vextreme.vexlife.android.vexstream.playback.VexStreamCurrentPlaybackObserver
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackTrace
import com.vextreme.vexlife.android.widget.vexstream.VexStreamPlaybackWidgetUpdateExecution
import kotlinx.coroutines.Dispatchers

/**
 * Android-native owner for VexStream audio playback realization lifetime.
 *
 * Playback semantic State remains in the process-shared VexStreamPlaybackFamily. This Service keeps
 * Media3/Player realization alive while UI is absent and reconciles provider truth into that Family.
 *
 * Local file MediaItems are resolved by DefaultDataSource. Opaque vexstream:// media requests flow
 * through [VexStreamNetworkDataSource], which loads the remembered endpoint/token and pins the
 * desktop TLS certificate before opening media bytes.
 */
class VexStreamPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val widgetExecutionRoot =
        VexExecutionRoot(
            dispatcher =
                Dispatchers.Default,
        )
    private var currentPlaybackObserver:
        VexStreamCurrentPlaybackObserver? =
        null
    private var widgetUpdateExecution:
        VexStreamPlaybackWidgetUpdateExecution? =
        null

    override fun onCreate() {
        super.onCreate()

        val vexApplication =
            application as?
                VexApplication
                ?: error(
                    "VexStream PlaybackService requires VexApplication as the process composition root",
                )
        val playbackFamily =
            vexApplication
                .dependencies
                .resolve(
                    vexApplication
                        .dependencyModule
                        .playbackFamily,
                )

        /*
         * Resolve the one process-lived background convergence owner. A Navigation or future
         * background Service resolves the same memoized Relay; no Service owns a private copy.
         */
        vexApplication
            .dependencies
            .resolve(
                vexApplication
                    .dependencyModule
                    .backgroundServiceRelay,
            )

        val dataSourceFactory =
            DefaultDataSource.Factory(
                this,
                VexStreamNetworkDataSource.Factory(
                    this,
                ),
            )
        val mediaSourceFactory =
            DefaultMediaSourceFactory(
                dataSourceFactory,
            )
        val player =
            ExoPlayer.Builder(
                this,
            )
                .setMediaSourceFactory(
                    mediaSourceFactory,
                )
                .build()

        mediaSession =
            MediaSession.Builder(
                this,
                player,
            ).build()

        VexStreamPlaybackTrace.record(
            stage =
                "service.session.ready",
            detail =
                "looper=${player.applicationLooper.thread.name}",
        )

        val playbackObserver =
            Media3VexStreamCurrentPlaybackObserver(
                player =
                    player,
                playbackFamily =
                    playbackFamily,
            )
        currentPlaybackObserver =
            playbackObserver
        widgetUpdateExecution =
            VexStreamPlaybackWidgetUpdateExecution(
                root =
                    widgetExecutionRoot,
                context =
                    applicationContext,
                observations =
                    playbackObserver
                        .observations,
            )
        VexStreamPlaybackTrace.record(
            stage =
                "service.widget-observer.attached",
        )
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? =
        mediaSession

    override fun onDestroy() {
        VexStreamPlaybackTrace.record(
            stage =
                "service.destroy",
        )
        currentPlaybackObserver
            ?.close()
        currentPlaybackObserver =
            null
        widgetUpdateExecution =
            null

        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        widgetExecutionRoot
            .forceStopAll(
                "VexStream PlaybackService destroyed",
            )

        super.onDestroy()
    }
}

// [VXG RealForever]
