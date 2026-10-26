package com.vextreme.vexlife.android.media.vexstream

import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.widget.vexstream.VexStreamPlaybackWidgetUpdateExecution
import com.vextreme.vexlife.android.widget.vexstream.vexStreamPlaybackWidgetProviderRead
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
    private val widgetUpdateExecution by
        lazy {
            VexStreamPlaybackWidgetUpdateExecution(
                root =
                    widgetExecutionRoot,
                context =
                    applicationContext,
            )
        }
    private val widgetPlayerListener =
        object :
            Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                runCatching {
                    vexStreamPlaybackWidgetProviderRead(
                        applicationContext,
                        player,
                    )
                }.getOrNull()
                    ?.let { providerRead ->
                        widgetUpdateExecution
                            .requestUpdate(
                                providerRead,
                            )
                    }
            }
        }

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

        player.addListener(
            widgetPlayerListener,
        )

        mediaSession =
            MediaSession.Builder(
                this,
                player,
            ).build()

        widgetUpdateExecution
            .requestUpdate(
                vexStreamPlaybackWidgetProviderRead(
                    applicationContext,
                    player,
                ),
            )
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player
                .removeListener(
                    widgetPlayerListener,
                )
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
