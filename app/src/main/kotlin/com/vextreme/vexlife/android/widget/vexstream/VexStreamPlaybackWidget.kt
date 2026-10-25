package com.vextreme.vexlife.android.widget.vexstream

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.text.Text
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.MainActivity
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.vexstream.VexStreamArtworkStore
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
import com.vextreme.vexlife.android.vexstream.playback.Media3VexStreamPlaybackConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackAct
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackControl
import com.vextreme.vexlife.android.vexstream.queue.Media3VexStreamQueueConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import com.vextreme.vexlife.android.widget.VexGlanceAppWidget
import com.vextreme.vexlife.android.widget.VexWidgetActionCallback
import com.vextreme.vexlife.android.widget.VexWidgetActionRef
import com.vextreme.vexlife.android.widget.VexWidgetProjection
import com.vextreme.vexlife.android.widget.vexWidgetActionParameters
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

private data class VexStreamPlaybackWidgetProviderRead(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val previousEnabled: Boolean,
    val playPauseEnabled: Boolean,
    val showPause: Boolean,
    val nextEnabled: Boolean,
)

internal data class VexStreamPlaybackWidgetProjection(
    val title: String,
    val artist: String,
    val artwork: Bitmap?,
    val previousEnabled: Boolean,
    val playPauseEnabled: Boolean,
    val showPause: Boolean,
    val nextEnabled: Boolean,
) : VexWidgetProjection

private val VexStreamWidgetDestinationKey =
    ActionParameters.Key<String>(
        MainActivity
            .VEXSTREAM_WIDGET_DESTINATION_EXTRA,
    )

internal object VexStreamPlaybackWidgetActions {
    val Previous =
        VexWidgetActionRef(
            "vexstream.widget.previous",
        )
    val PlayPause =
        VexWidgetActionRef(
            "vexstream.widget.play-pause",
        )
    val Next =
        VexWidgetActionRef(
            "vexstream.widget.next",
        )
}

internal sealed interface VexStreamPlaybackWidgetCommand {
    data class Playback(
        val control: VexStreamPlaybackControl,
    ) : VexStreamPlaybackWidgetCommand

    data class Queue(
        val control: VexStreamQueueControl,
    ) : VexStreamPlaybackWidgetCommand
}

internal fun vexStreamPlaybackWidgetCommand(
    actionRef: VexWidgetActionRef,
): VexStreamPlaybackWidgetCommand? =
    when (
        actionRef
    ) {
        VexStreamPlaybackWidgetActions.Previous ->
            VexStreamPlaybackWidgetCommand.Queue(
                VexStreamQueueControl.Previous,
            )

        VexStreamPlaybackWidgetActions.PlayPause ->
            VexStreamPlaybackWidgetCommand.Playback(
                VexStreamPlaybackControl.PlayPause,
            )

        VexStreamPlaybackWidgetActions.Next ->
            VexStreamPlaybackWidgetCommand.Queue(
                VexStreamQueueControl.Next,
            )

        else ->
            null
    }

internal object VexStreamPlaybackWidget :
    VexGlanceAppWidget<
        VexStreamPlaybackWidgetProjection
    >() {
    override suspend fun projection(
        context: Context,
        glanceId: GlanceId,
    ): VexStreamPlaybackWidgetProjection {
        val fallbackTitle =
            context.getString(
                R.string.vexstream_music_title,
            )

        val providerRead =
            withVexStreamWidgetController(
                context,
            ) { controller ->
                val playbackConnection =
                    Media3VexStreamPlaybackConnection(
                        controller,
                    )
                try {
                    val playbackProjection =
                        playbackConnection
                            .projections
                            .value
                    val queueProjection =
                        Media3VexStreamQueueConnection(
                            controller,
                        ).currentProjection
                    val metadata =
                        controller
                            .currentMediaItem
                            ?.mediaMetadata
                    val playPause =
                        playbackProjection[
                            VexStreamPlaybackControl.PlayPause
                        ]

                    VexStreamPlaybackWidgetProviderRead(
                        title =
                            metadata
                                ?.title
                                ?.toString()
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: fallbackTitle,
                        artist =
                            metadata
                                ?.artist
                                ?.toString()
                                .orEmpty(),
                        artworkUrl =
                            metadata
                                ?.artworkUri
                                ?.toString()
                                ?.takeIf {
                                    it.startsWith(
                                        "https://",
                                    )
                                },
                        previousEnabled =
                            queueProjection[
                                VexStreamQueueControl.Previous
                            ].isAvailable,
                        playPauseEnabled =
                            playPause
                                .isAvailable,
                        showPause =
                            playPause
                                .act is
                                VexStreamPlaybackAct.Pause,
                        nextEnabled =
                            queueProjection[
                                VexStreamQueueControl.Next
                            ].isAvailable,
                    )
                } finally {
                    playbackConnection
                        .close()
                }
            }
                ?: return VexStreamPlaybackWidgetProjection(
                    title = fallbackTitle,
                    artist = "",
                    artwork = null,
                    previousEnabled = false,
                    playPauseEnabled = false,
                    showPause = false,
                    nextEnabled = false,
                )

        val artwork =
            withContext(
                Dispatchers.IO,
            ) {
                providerRead
                    .artworkUrl
                    ?.let { artworkUrl ->
                        VexStreamArtworkStore
                            .production(
                                context.filesDir,
                            )
                            .fileForArtworkUrl(
                                artworkUrl,
                            )
                    }
                    ?.let(
                        ::decodeWidgetArtwork,
                    )
            }

        return VexStreamPlaybackWidgetProjection(
            title =
                providerRead.title,
            artist =
                providerRead.artist,
            artwork =
                artwork,
            previousEnabled =
                providerRead.previousEnabled,
            playPauseEnabled =
                providerRead.playPauseEnabled,
            showPause =
                providerRead.showPause,
            nextEnabled =
                providerRead.nextEnabled,
        )
    }

    @Composable
    override fun Content(
        projection: VexStreamPlaybackWidgetProjection,
    ) {
        val context =
            LocalContext.current
        val contentAction =
            actionStartActivity<
                MainActivity
            >(
                actionParametersOf(
                    VexStreamWidgetDestinationKey to
                        MainActivity
                            .VEXSTREAM_WIDGET_DESTINATION_FULL_PLAYER,
                ),
            )
        val primaryText =
            TextStyle(
                color =
                    ColorProvider(
                        day =
                            Color(
                                0xFF181818,
                            ),
                        night =
                            Color(
                                0xFFF7F7F7,
                            ),
                    ),
                fontSize =
                    15.sp,
            )
        val secondaryText =
            TextStyle(
                color =
                    ColorProvider(
                        day =
                            Color(
                                0xFF5A5A5A,
                            ),
                        night =
                            Color(
                                0xFFC5C5C5,
                            ),
                    ),
                fontSize =
                    12.sp,
            )

        Row(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .background(
                        day =
                            Color(
                                0xFFF7F7F7,
                            ),
                        night =
                            Color(
                                0xFF181818,
                            ),
                    )
                    .cornerRadius(
                        18.dp,
                    )
                    .padding(
                        horizontal =
                            12.dp,
                        vertical =
                            10.dp,
                    ),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    GlanceModifier
                        .size(
                            54.dp,
                        )
                        .cornerRadius(
                            12.dp,
                        )
                        .clickable(
                            contentAction,
                        ),
                contentAlignment =
                    Alignment.Center,
            ) {
                projection
                    .artwork
                    ?.let { bitmap ->
                        Image(
                            provider =
                                ImageProvider(
                                    bitmap,
                                ),
                            contentDescription =
                                context.getString(
                                    R.string
                                        .vexstream_music_now_playing,
                                ),
                            modifier =
                                GlanceModifier
                                    .fillMaxSize()
                                    .cornerRadius(
                                        12.dp,
                                    ),
                            contentScale =
                                ContentScale.Crop,
                        )
                    }
                    ?: Text(
                        text = "♪",
                        style =
                            TextStyle(
                                color =
                                    ColorProvider(
                                        day =
                                            Color(
                                                0xFF5A5A5A,
                                            ),
                                        night =
                                            Color(
                                                0xFFC5C5C5,
                                            ),
                                    ),
                                fontSize =
                                    28.sp,
                            ),
                    )
            }

            Spacer(
                modifier =
                    GlanceModifier
                        .width(
                            10.dp,
                        ),
            )

            Column(
                modifier =
                    GlanceModifier
                        .defaultWeight()
                        .clickable(
                            contentAction,
                        ),
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        projection.title,
                    style =
                        primaryText,
                    maxLines = 1,
                )
                if (
                    projection.artist
                        .isNotBlank()
                ) {
                    Spacer(
                        modifier =
                            GlanceModifier
                                .height(
                                    2.dp,
                                ),
                    )
                    Text(
                        text =
                            projection.artist,
                        style =
                            secondaryText,
                        maxLines = 1,
                    )
                }
            }

            Spacer(
                modifier =
                    GlanceModifier
                        .width(
                            6.dp,
                        ),
            )

            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                VexStreamWidgetControl(
                    symbol = "⏮",
                    contentDescription =
                        context.getString(
                            R.string
                                .vexstream_music_previous,
                        ),
                    enabled =
                        projection
                            .previousEnabled,
                    actionRef =
                        VexStreamPlaybackWidgetActions
                            .Previous,
                )
                VexStreamWidgetControl(
                    symbol =
                        if (
                            projection.showPause
                        ) {
                            "⏸"
                        } else {
                            "▶"
                        },
                    contentDescription =
                        context.getString(
                            R.string
                                .vexstream_music_play_pause,
                        ),
                    enabled =
                        projection
                            .playPauseEnabled,
                    actionRef =
                        VexStreamPlaybackWidgetActions
                            .PlayPause,
                )
                VexStreamWidgetControl(
                    symbol = "⏭",
                    contentDescription =
                        context.getString(
                            R.string
                                .vexstream_music_next,
                        ),
                    enabled =
                        projection
                            .nextEnabled,
                    actionRef =
                        VexStreamPlaybackWidgetActions
                            .Next,
                )
            }
        }
    }
}

@Composable
private fun VexStreamWidgetControl(
    symbol: String,
    contentDescription: String,
    enabled: Boolean,
    actionRef: VexWidgetActionRef,
) {
    val baseModifier =
        GlanceModifier
            .size(
                38.dp,
            )
            .cornerRadius(
                19.dp,
            )
            .semantics {
                this.contentDescription =
                    contentDescription
            }
    val modifier =
        if (
            enabled
        ) {
            baseModifier
                .clickable(
                    actionRunCallback<
                        VexStreamPlaybackWidgetActionCallback
                    >(
                        vexWidgetActionParameters(
                            actionRef,
                        ),
                    ),
                )
        } else {
            baseModifier
        }

    Box(
        modifier =
            modifier,
        contentAlignment =
            Alignment.Center,
    ) {
        Text(
            text =
                symbol,
            style =
                TextStyle(
                    color =
                        if (
                            enabled
                        ) {
                            ColorProvider(
                                day =
                                    Color(
                                        0xFF181818,
                                    ),
                                night =
                                    Color(
                                        0xFFF7F7F7,
                                    ),
                            )
                        } else {
                            ColorProvider(
                                day =
                                    Color(
                                        0xFFAAAAAA,
                                    ),
                                night =
                                    Color(
                                        0xFF666666,
                                    ),
                            )
                        },
                    fontSize =
                        19.sp,
                ),
        )
    }
}

class VexStreamPlaybackWidgetReceiver :
    GlanceAppWidgetReceiver() {
    override val glanceAppWidget:
        androidx.glance.appwidget.GlanceAppWidget =
        VexStreamPlaybackWidget
}

class VexStreamPlaybackWidgetActionCallback :
    VexWidgetActionCallback() {
    override suspend fun onWidgetAction(
        context: Context,
        glanceId: GlanceId,
        actionRef: VexWidgetActionRef,
    ) {
        val command =
            vexStreamPlaybackWidgetCommand(
                actionRef,
            ) ?: return

        withVexStreamWidgetController(
            context,
        ) { controller ->
            when (
                command
            ) {
                is VexStreamPlaybackWidgetCommand.Playback -> {
                    val connection =
                        Media3VexStreamPlaybackConnection(
                            controller,
                        )
                    try {
                        connection.activate(
                            command.control,
                        )
                    } finally {
                        connection.close()
                    }
                }

                is VexStreamPlaybackWidgetCommand.Queue ->
                    Media3VexStreamQueueConnection(
                        controller,
                    ).activate(
                        command.control,
                    )
            }
        }

        VexStreamPlaybackWidget
            .update(
                context,
                glanceId,
            )
    }
}

/**
 * MediaSessionService-owned update loop.
 *
 * Player events only enqueue one conflated widget refresh request. Rendering/MediaController reads
 * happen inside the managed VexExecution child rather than inside the Player listener callback.
 */
internal class VexStreamPlaybackWidgetUpdateExecution(
    root: VexExecutionRoot,
    private val context: Context,
) : VexExecution(
        root,
    ) {
    private val requests =
        Channel<Unit>(
            capacity =
                Channel.CONFLATED,
        )

    init {
        launchForCollect(
            source =
                ::requestsForExecution,
        ) {
            updateVexStreamPlaybackWidgets(
                context,
            )
        }
    }

    fun requestUpdate(): Boolean =
        requests
            .trySend(
                Unit,
            )
            .isSuccess

    private fun requestsForExecution():
        Flow<Unit> =
        requests
            .receiveAsFlow()
}

private suspend fun <
    Result,
> withVexStreamWidgetController(
    context: Context,
    block:
        suspend (
            MediaController,
        ) -> Result,
): Result? {
    val appContext =
        context.applicationContext
    val controllerFuture =
        MediaController.Builder(
            appContext,
            SessionToken(
                appContext,
                ComponentName(
                    appContext,
                    VexStreamPlaybackService::class.java,
                ),
            ),
        ).buildAsync()

    return try {
        val controller =
            withContext(
                Dispatchers.IO,
            ) {
                controllerFuture
                    .get(
                        CONTROLLER_CONNECT_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS,
                    )
            }
        val providerDispatcher =
            Handler(
                controller.applicationLooper,
            ).asCoroutineDispatcher(
                "VexStream widget Media3",
            )

        withContext(
            providerDispatcher,
        ) {
            block(
                controller,
            )
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    } finally {
        MediaController.releaseFuture(
            controllerFuture,
        )
    }
}

internal suspend fun updateVexStreamPlaybackWidgets(
    context: Context,
) {
    try {
        VexStreamPlaybackWidget
            .updateAll(
                context.applicationContext,
            )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        // Launcher/RemoteViews failure is not playback failure. A later player event may retry.
    }
}

private fun decodeWidgetArtwork(
    file: File,
): Bitmap? {
    val bounds =
        BitmapFactory.Options().apply {
            inJustDecodeBounds =
                true
        }
    BitmapFactory.decodeFile(
        file.absolutePath,
        bounds,
    )

    if (
        bounds.outWidth <= 0 ||
        bounds.outHeight <= 0
    ) {
        return null
    }

    var sampleSize =
        1
    while (
        bounds.outWidth /
        sampleSize >
        WIDGET_ARTWORK_MAX_PX ||
        bounds.outHeight /
        sampleSize >
        WIDGET_ARTWORK_MAX_PX
    ) {
        sampleSize *=
            2
    }

    return BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize =
                sampleSize
            inPreferredConfig =
                Bitmap.Config.RGB_565
        },
    )
}

private const val CONTROLLER_CONNECT_TIMEOUT_MS =
    2_000L
private const val WIDGET_ARTWORK_MAX_PX =
    128

// [VXG RealForever]
