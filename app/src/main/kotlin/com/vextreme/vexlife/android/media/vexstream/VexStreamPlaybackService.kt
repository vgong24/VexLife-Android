package com.vextreme.vexlife.android.media.vexstream

import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vextreme.vexlife.android.VexApplication
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackTrace
import com.vextreme.vexlife.android.vexstream.player.Media3PlayerConnection
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerObservationReceiver
import com.vextreme.vexlife.android.widget.vexstream.VexStreamPlaybackWidgetUpdateExecution
import kotlinx.coroutines.Dispatchers

/**
 * Android-native owner for VexStream audio playback lifetime.
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
    private var playerConnection:
        Media3PlayerConnection? =
        null
    private var widgetUpdateExecution:
        VexStreamPlaybackWidgetUpdateExecution? =
        null

    override fun onCreate() {
        super.onCreate()

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

        val vexApplication =
            application as?
                VexApplication
                ?: error(
                    "VexStream PlaybackService requires VexApplication",
                )
        val playerStore =
            vexApplication
                .dependencies
                .resolve(
                    vexApplication
                        .dependencyModule
                        .playerStore,
                )
        val processExecutionRoot =
            vexApplication
                .dependencies
                .resolve(
                    vexApplication
                        .dependencyModule
                        .processExecutionRoot,
                )

        playerConnection =
            Media3PlayerConnection(
                player =
                    player,
                receiver =
                    VexStreamPlayerObservationReceiver(
                        playerStore,
                    ),
                executionRoot =
                    processExecutionRoot,
            )
        widgetUpdateExecution =
            VexStreamPlaybackWidgetUpdateExecution(
                root =
                    widgetExecutionRoot,
                context =
                    applicationContext,
                observations =
                    playerStore.states,
            )
        VexStreamPlaybackTrace.record(
            stage =
                "service.player-partner.attached",
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
        playerConnection
            ?.close()
        playerConnection =
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
