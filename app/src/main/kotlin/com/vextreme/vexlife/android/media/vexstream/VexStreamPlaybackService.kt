package com.vextreme.vexlife.android.media.vexstream

import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Android-native owner for VexStream audio playback lifetime.
 *
 * Local file MediaItems are resolved by DefaultDataSource. Opaque vexstream:// media requests flow
 * through [VexStreamNetworkDataSource], which loads the remembered endpoint/token and pins the
 * desktop TLS certificate before opening media bytes.
 */
class VexStreamPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

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
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null

        super.onDestroy()
    }
}

// [VXG RealForever]
