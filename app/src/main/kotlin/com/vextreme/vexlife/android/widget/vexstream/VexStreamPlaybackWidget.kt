package com.vextreme.vexlife.android.widget.vexstream

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.text.Text
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.TextStyle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vextreme.vexlife.android.MainActivity
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.data.vexstream.VexStreamArtworkStore
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.media.vexstream.VexStreamPlaybackService
import com.vextreme.vexlife.android.vexstream.playback.Media3VexStreamPlaybackConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamCurrentPlaybackObservation
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackAct
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackControl
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackTrace
import com.vextreme.vexlife.android.vexstream.playback.toVexStreamCurrentPlaybackObservation
import com.vextreme.vexlife.android.vexstream.queue.Media3VexStreamQueueConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import com.vextreme.vexlife.android.widget.VexGlanceAppWidget
import com.vextreme.vexlife.android.widget.VexWidgetActionCallback
import com.vextreme.vexlife.android.widget.VexWidgetActionRef
import com.vextreme.vexlife.android.widget.VexWidgetPreferenceCodec
import com.vextreme.vexlife.android.widget.VexWidgetProjection
import com.vextreme.vexlife.android.widget.VexWidgetSurfaceState
import com.vextreme.vexlife.android.widget.vexWidgetActionParameters
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal data class VexStreamPlaybackWidgetSurfaceState(
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val artworkAvailable: Boolean,
    val previousEnabled: Boolean,
    val playPauseEnabled: Boolean,
    val showPause: Boolean,
    val nextEnabled: Boolean,
) : VexWidgetSurfaceState

private object VexStreamPlaybackWidgetStateKeys {
    val Title =
        stringPreferencesKey(
            "vexstream.widget.title",
        )
    val Artist =
        stringPreferencesKey(
            "vexstream.widget.artist",
        )
    val ArtworkUri =
        stringPreferencesKey(
            "vexstream.widget.artwork-uri",
        )
    val ArtworkAvailable =
        booleanPreferencesKey(
            "vexstream.widget.artwork-available",
        )
    val PreviousEnabled =
        booleanPreferencesKey(
            "vexstream.widget.previous-enabled",
        )
    val PlayPauseEnabled =
        booleanPreferencesKey(
            "vexstream.widget.play-pause-enabled",
        )
    val ShowPause =
        booleanPreferencesKey(
            "vexstream.widget.show-pause",
        )
    val NextEnabled =
        booleanPreferencesKey(
            "vexstream.widget.next-enabled",
        )
}

private object VexStreamPlaybackWidgetSurfaceStateCodec :
    VexWidgetPreferenceCodec<
        VexStreamPlaybackWidgetSurfaceState
    > {
    override fun read(
        preferences: Preferences,
    ): VexStreamPlaybackWidgetSurfaceState? =
        VexStreamPlaybackWidgetSurfaceState(
            title =
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .Title
                ] ?: return null,
            artist =
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .Artist
                ].orEmpty(),
            artworkUri =
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .ArtworkUri
                ],
            artworkAvailable =
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .ArtworkAvailable
                ] ?: false,
            previousEnabled =
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .PreviousEnabled
                ] ?: false,
            playPauseEnabled =
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .PlayPauseEnabled
                ] ?: false,
            showPause =
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .ShowPause
                ] ?: false,
            nextEnabled =
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .NextEnabled
                ] ?: false,
        )

    override fun write(
        preferences: MutablePreferences,
        state: VexStreamPlaybackWidgetSurfaceState,
    ) {
        preferences[
            VexStreamPlaybackWidgetStateKeys
                .Title
        ] =
            state.title
        preferences[
            VexStreamPlaybackWidgetStateKeys
                .Artist
        ] =
            state.artist
        state
            .artworkUri
            ?.let { artworkUri ->
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .ArtworkUri
                ] =
                    artworkUri
            }
            ?: preferences.remove(
                VexStreamPlaybackWidgetStateKeys
                    .ArtworkUri,
            )
        preferences[
            VexStreamPlaybackWidgetStateKeys
                .ArtworkAvailable
        ] =
            state.artworkAvailable
        preferences[
            VexStreamPlaybackWidgetStateKeys
                .PreviousEnabled
        ] =
            state.previousEnabled
        preferences[
            VexStreamPlaybackWidgetStateKeys
                .PlayPauseEnabled
        ] =
            state.playPauseEnabled
        preferences[
            VexStreamPlaybackWidgetStateKeys
                .ShowPause
        ] =
            state.showPause
        preferences[
            VexStreamPlaybackWidgetStateKeys
                .NextEnabled
        ] =
            state.nextEnabled
    }
}

internal data class VexStreamPlaybackWidgetProjection(
    val title: String,
    val artist: String,
    val artwork: Bitmap?,
    val previousEnabled: Boolean,
    val playPauseEnabled: Boolean,
    val showPause: Boolean,
    val nextEnabled: Boolean,
) : VexWidgetProjection

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

internal suspend fun vexStreamPlaybackWidgetSurfaceState(
    context: Context,
    fallbackTitle: String,
    observation:
        VexStreamCurrentPlaybackObservation,
): VexStreamPlaybackWidgetSurfaceState {
    val readState =
        observation.readState
    val playPause =
        observation
            .playbackProjection[
                VexStreamPlaybackControl.PlayPause
            ]
    val artworkUri =
        readState
            .currentArtworkUri
            ?.takeIf {
                it.isNotBlank()
            }
    val artworkAvailable =
        vexStreamPlaybackWidgetArtworkAvailable(
            context =
                context.applicationContext,
            artworkUri =
                artworkUri,
        )

    return VexStreamPlaybackWidgetSurfaceState(
        title =
            readState
                .currentTitle
                .takeIf {
                    it.isNotBlank()
                }
                ?: fallbackTitle,
        artist =
            readState.currentArtist,
        artworkUri =
            artworkUri,
        artworkAvailable =
            artworkAvailable,
        previousEnabled =
            observation
                .queueProjection[
                    VexStreamQueueControl.Previous
                ].isAvailable,
        playPauseEnabled =
            playPause.isAvailable,
        showPause =
            playPause.act is
                VexStreamPlaybackAct.Pause,
        nextEnabled =
            observation
                .queueProjection[
                    VexStreamQueueControl.Next
                ].isAvailable,
    )
}

private fun fallbackVexStreamPlaybackWidgetSurfaceState(
    context: Context,
): VexStreamPlaybackWidgetSurfaceState =
    VexStreamPlaybackWidgetSurfaceState(
        title =
            context.getString(
                R.string.vexstream_music_title,
            ),
        artist = "",
        artworkUri = null,
        artworkAvailable = false,
        previousEnabled = false,
        playPauseEnabled = false,
        showPause = false,
        nextEnabled = false,
    )

private suspend fun vexStreamPlaybackWidgetArtworkAvailable(
    context: Context,
    artworkUri: String?,
): Boolean =
    withContext(
        Dispatchers.IO,
    ) {
        artworkUri
            ?.let { uri ->
                VexStreamArtworkStore
                    .production(
                        context.filesDir,
                    )
                    .fileForPlaybackArtworkUri(
                        uri,
                    ) !=
                    null
            } ?: false
    }

private suspend fun VexStreamPlaybackWidgetSurfaceState.toWidgetProjection(
    context: Context,
): VexStreamPlaybackWidgetProjection {
    val artwork =
        withContext(
            Dispatchers.IO,
        ) {
            artworkUri
                ?.takeIf {
                    artworkAvailable
                }
                ?.let { uri ->
                    VexStreamArtworkStore
                        .production(
                            context.filesDir,
                        )
                        .fileForPlaybackArtworkUri(
                            uri,
                        )
                }
                ?.let(
                    ::decodeWidgetArtwork,
                )
        }

    return VexStreamPlaybackWidgetProjection(
        title = title,
        artist = artist,
        artwork = artwork,
        previousEnabled = previousEnabled,
        playPauseEnabled = playPauseEnabled,
        showPause = showPause,
        nextEnabled = nextEnabled,
    )
}

internal object VexStreamPlaybackWidget :
    VexGlanceAppWidget<
        VexStreamPlaybackWidgetSurfaceState,
        VexStreamPlaybackWidgetProjection
    >() {
    override val stateCodec:
        VexWidgetPreferenceCodec<
            VexStreamPlaybackWidgetSurfaceState
        > =
        VexStreamPlaybackWidgetSurfaceStateCodec

    override suspend fun initialSurfaceState(
        context: Context,
        glanceId: GlanceId,
    ): VexStreamPlaybackWidgetSurfaceState {
        val appContext =
            context.applicationContext
        val fallbackTitle =
            appContext.getString(
                R.string.vexstream_music_title,
            )
        val observation =
            withVexStreamWidgetController(
                appContext,
            ) { controller ->
                controller
                    .toVexStreamCurrentPlaybackObservation()
            }

        if (
            observation != null
        ) {
            VexStreamPlaybackTrace.record(
                stage =
                    "widget.surface-state.bootstrap",
                mediaId =
                    observation.readState
                        .currentMediaId,
                title =
                    observation.readState
                        .currentTitle,
                artist =
                    observation.readState
                        .currentArtist,
                glanceId =
                    glanceId.toString(),
            )

            return vexStreamPlaybackWidgetSurfaceState(
                context =
                    appContext,
                fallbackTitle =
                    fallbackTitle,
                observation =
                    observation,
            )
        }

        return fallbackVexStreamPlaybackWidgetSurfaceState(
            appContext,
        )
    }

    override suspend fun projection(
        context: Context,
        glanceId: GlanceId,
        state: VexStreamPlaybackWidgetSurfaceState,
    ): VexStreamPlaybackWidgetProjection {
        VexStreamPlaybackTrace.record(
            stage =
                "widget.projection.ready",
            title =
                state.title,
            artist =
                state.artist,
            glanceId =
                glanceId.toString(),
            detail =
                "artworkAvailable=${state.artworkAvailable}",
        )

        return state
            .toWidgetProjection(
                context.applicationContext,
            )
    }

    @Composable
    override fun Content(
        projection: VexStreamPlaybackWidgetProjection,
    ) {
        val context =
            LocalContext.current
        val contentAction =
            actionStartActivity(
                Intent(
                    context,
                    MainActivity::class.java,
                ).apply {
                    action =
                        MainActivity
                            .ACTION_OPEN_VEXSTREAM_FULL_PLAYER
                    flags =
                        VEXSTREAM_WIDGET_ENTRY_ACTIVITY_FLAGS
                },
            )
        val primaryText =
            TextStyle(
                color =
                    androidx.glance.color.ColorProvider(
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
                            10.dp,
                        vertical =
                            8.dp,
                    ),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    GlanceModifier
                        .size(
                            VEXSTREAM_WIDGET_ARTWORK_SIZE_DP.dp,
                        )
                        .cornerRadius(
                            14.dp,
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
                                        14.dp,
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
                                    androidx.glance.color.ColorProvider(
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
                        .defaultWeight(),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalAlignment =
                    Alignment.CenterHorizontally,
            ) {
                Text(
                    text =
                        vexStreamPlaybackWidgetHeadline(
                            title =
                                projection.title,
                            artist =
                                projection.artist,
                        ),
                    modifier =
                        GlanceModifier
                            .clickable(
                                contentAction,
                            ),
                    style =
                        primaryText,
                    maxLines = 1,
                )

                Spacer(
                    modifier =
                        GlanceModifier
                            .height(
                                6.dp,
                            ),
                )

                Row(
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
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
                    Spacer(
                        modifier =
                            GlanceModifier
                                .width(
                                    VEXSTREAM_WIDGET_CONTROL_GAP_DP.dp,
                                ),
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
                    Spacer(
                        modifier =
                            GlanceModifier
                                .width(
                                    VEXSTREAM_WIDGET_CONTROL_GAP_DP.dp,
                                ),
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
}

internal fun vexStreamPlaybackWidgetHeadline(
    title: String,
    artist: String,
): String =
    artist
        .takeIf {
            it.isNotBlank()
        }
        ?.let {
            "$title — $it"
        }
        ?: title

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
                VEXSTREAM_WIDGET_CONTROL_TOUCH_TARGET_DP.dp,
            )
            .cornerRadius(
                (VEXSTREAM_WIDGET_CONTROL_TOUCH_TARGET_DP / 2).dp,
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
                            androidx.glance.color.ColorProvider(
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
                            androidx.glance.color.ColorProvider(
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
                        VEXSTREAM_WIDGET_CONTROL_SYMBOL_SP.sp,
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
        VexStreamPlaybackTrace.record(
            stage =
                "widget.action.received",
            glanceId =
                glanceId.toString(),
            detail =
                "actionRef=${actionRef.value}",
        )
        val command =
            vexStreamPlaybackWidgetCommand(
                actionRef,
            ) ?: return

        VexStreamPlaybackTrace.record(
            stage =
                "widget.action.resolved",
            glanceId =
                glanceId.toString(),
            detail =
                "command=$command",
        )

        withVexStreamWidgetController(
            context,
        ) { controller ->
            VexStreamPlaybackTrace.record(
                stage =
                    "widget.action.controller-ready",
                glanceId =
                    glanceId.toString(),
                detail =
                    "command=$command",
            )
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
            VexStreamPlaybackTrace.record(
                stage =
                    "widget.action.activated",
                glanceId =
                    glanceId.toString(),
                detail =
                    "command=$command",
            )
        }
    }
}

/**
 * MediaSessionService-owned widget publication sink.
 *
 * Exactly one ordered provider observation stream feeds this execution. Widget callbacks only issue
 * Controls; they never become a second playback read/publication producer.
 */
internal class VexStreamPlaybackWidgetUpdateExecution(
    root: VexExecutionRoot,
    private val context: Context,
    private val observations:
        Flow<
            VexStreamCurrentPlaybackObservation
        >,
) : VexExecution(
        root,
    ) {
    init {
        launchForCollect(
            source =
                ::observationsForExecution,
        ) { observation ->
            VexStreamPlaybackTrace.record(
                stage =
                    "widget.observation.received",
                mediaId =
                    observation.readState
                        .currentMediaId,
                title =
                    observation.readState
                        .currentTitle,
                artist =
                    observation.readState
                        .currentArtist,
            )
            val surfaceState =
                vexStreamPlaybackWidgetSurfaceState(
                    context =
                        context,
                    fallbackTitle =
                        context.getString(
                            R.string.vexstream_music_title,
                        ),
                    observation =
                        observation,
                )
            VexStreamPlaybackTrace.record(
                stage =
                    "widget.surface-state.mapped",
                mediaId =
                    observation.readState
                        .currentMediaId,
                title =
                    surfaceState.title,
                artist =
                    surfaceState.artist,
                detail =
                    "artwork=${surfaceState.artworkAvailable} " +
                        "previous=${surfaceState.previousEnabled} " +
                        "playPause=${surfaceState.playPauseEnabled} " +
                        "pause=${surfaceState.showPause} " +
                        "next=${surfaceState.nextEnabled}",
            )
            updateVexStreamPlaybackWidgets(
                context = context,
                surfaceState =
                    surfaceState,
            )
        }
    }

    private fun observationsForExecution():
        Flow<
            VexStreamCurrentPlaybackObservation
        > =
        observations
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
    VexStreamPlaybackTrace.record(
        stage =
            "widget.controller.connect.begin",
    )
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
        VexStreamPlaybackTrace.record(
            stage =
                "widget.controller.connect.success",
            detail =
                "looper=${controller.applicationLooper.thread.name}",
        )
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
    } catch (failure: Exception) {
        VexStreamPlaybackTrace.record(
            stage =
                "widget.controller.connect.failure",
            detail =
                "${failure::class.simpleName}:${failure.message.orEmpty()}",
        )
        null
    } finally {
        VexStreamPlaybackTrace.record(
            stage =
                "widget.controller.release",
        )
        MediaController.releaseFuture(
            controllerFuture,
        )
    }
}

internal suspend fun retryVexStreamPlaybackWidgetPublication(
    publish: suspend () -> Boolean,
    awaitRetry: suspend () -> Unit,
) {
    if (publish()) {
        return
    }

    awaitRetry()
    publish()
}

internal suspend fun updateVexStreamPlaybackWidgets(
    context: Context,
) {
    val result =
        VexStreamPlaybackWidget
            .transformAll(
                context,
            ) { current ->
                current.copy(
                    artworkAvailable =
                        vexStreamPlaybackWidgetArtworkAvailable(
                            context =
                                context.applicationContext,
                            artworkUri =
                                current.artworkUri,
                        ),
                )
            }

    VexStreamPlaybackTrace.record(
        stage =
            "widget.presentation-state.transformed",
        detail =
            "instances=${result.instanceCount} " +
                "changed=${result.changedCount} " +
                "requested=${result.updateRequestedCount} " +
                "failed=${result.failedCount}",
    )
}

internal suspend fun updateVexStreamPlaybackWidgets(
    context: Context,
    surfaceState:
        VexStreamPlaybackWidgetSurfaceState,
) {
    retryVexStreamPlaybackWidgetPublication(
        publish = {
            val result =
                VexStreamPlaybackWidget
                    .publishAll(
                        context =
                            context,
                        state =
                            surfaceState,
                    )

            VexStreamPlaybackTrace.record(
                stage =
                    "widget.surface-state.published",
                title =
                    surfaceState.title,
                artist =
                    surfaceState.artist,
                detail =
                    "instances=${result.instanceCount} " +
                        "changed=${result.changedCount} " +
                        "requested=${result.updateRequestedCount} " +
                        "failed=${result.failedCount}",
            )

            result.allUpdatesRequested
        },
        awaitRetry = {
            delay(
                VEXSTREAM_WIDGET_PUBLICATION_RETRY_MILLIS,
            )
        },
    )
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

internal const val VEXSTREAM_WIDGET_ENTRY_ACTIVITY_FLAGS =
    Intent.FLAG_ACTIVITY_CLEAR_TOP or
        Intent.FLAG_ACTIVITY_SINGLE_TOP

internal const val VEXSTREAM_WIDGET_CONTROL_TOUCH_TARGET_DP =
    56
internal const val VEXSTREAM_WIDGET_CONTROL_SYMBOL_SP =
    28
internal const val VEXSTREAM_WIDGET_CONTROL_GAP_DP =
    8
internal const val VEXSTREAM_WIDGET_ARTWORK_SIZE_DP =
    64
internal const val VEXSTREAM_WIDGET_PUBLICATION_RETRY_MILLIS =
    250L

private const val CONTROLLER_CONNECT_TIMEOUT_MS =
    2_000L
private const val WIDGET_ARTWORK_MAX_PX =
    128

// [VXG RealForever]
