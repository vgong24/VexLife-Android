package com.vextreme.vexlife.android.media.vexstream

import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vextreme.vexlife.android.VexApplication
import com.vextreme.vexlife.android.behavior.VexService
import com.vextreme.vexlife.android.vexstream.player.Media3VexStreamPlayerConnection
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerObservationReceiver
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackFamily
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackState
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackTrace

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
class VexStreamPlaybackService :
    MediaSessionService(),
    VexService<
        VexStreamPlaybackService,
        VexStreamPlaybackState
    > {
    private var mediaSession: MediaSession? = null
    private var playbackFamily:
        VexStreamPlaybackFamily? =
        null

    override val current:
        VexStreamPlaybackState
        get() =
            requireNotNull(
                playbackFamily,
            ) {
                "VexStream PlaybackService current truth is available only during its live realization"
            }
                .currentState

    private var currentPlayerConnection:
        Media3VexStreamPlayerConnection? =
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
        this.playbackFamily =
            playbackFamily

        val processExecutionRoot =
            vexApplication
                .dependencies
                .resolve(
                    vexApplication
                        .dependencyModule
                        .processExecutionRoot,
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

        val playerReceiver =
            vexApplication
                .dependencies
                .resolve(
                    vexApplication
                        .dependencyModule
                        .playerObservationReceiver,
                )
        val playerConnection =
            Media3VexStreamPlayerConnection(
                player =
                    player,
                receiver =
                    playerReceiver,
                executionRoot =
                    processExecutionRoot,
            )
        currentPlayerConnection =
            playerConnection
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
        currentPlayerConnection
            ?.close()
        currentPlayerConnection =
            null
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null

        /*
         * Closing the Player Partner Connection admits Unavailable through the same outside-truth
         * circuit used during normal operation; Service teardown does not manufacture a second
         * semantic Playback path.
         */
        playbackFamily =
            null

        super.onDestroy()
    }
}

// [VXG RealForever]
