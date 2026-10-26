package com.vextreme.vexlife.android.widget.vexstream

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
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
import com.vextreme.vexlife.android.widget.VexWidgetProjection
import com.vextreme.vexlife.android.widget.vexWidgetActionParameters
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal data class VexStreamPlaybackWidgetProviderRead(
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val previousEnabled: Boolean,
    val playPauseEnabled: Boolean,
    val showPause: Boolean,
    val nextEnabled: Boolean,
)

internal data class VexStreamPlaybackWidgetCurrentness(
    val desiredRevision: Long,
    val renderedRevision: Long,
) {
    val isConverged: Boolean
        get() =
            desiredRevision > 0L &&
                desiredRevision ==
                renderedRevision
}

private object VexStreamPlaybackWidgetStateKeys {
    val DesiredRevision =
        longPreferencesKey(
            "vexstream.widget.desired-revision",
        )
    val RenderedRevision =
        longPreferencesKey(
            "vexstream.widget.rendered-revision",
        )
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

private fun Preferences.vexStreamPlaybackWidgetProviderReadOrNull():
    VexStreamPlaybackWidgetProviderRead? {
    val desiredRevision =
        this[
            VexStreamPlaybackWidgetStateKeys
                .DesiredRevision
        ] ?: return null
    if (desiredRevision <= 0L) {
        return null
    }

    return VexStreamPlaybackWidgetProviderRead(
        title =
            this[
                VexStreamPlaybackWidgetStateKeys
                    .Title
            ] ?: return null,
        artist =
            this[
                VexStreamPlaybackWidgetStateKeys
                    .Artist
            ].orEmpty(),
        artworkUri =
            this[
                VexStreamPlaybackWidgetStateKeys
                    .ArtworkUri
            ],
        previousEnabled =
            this[
                VexStreamPlaybackWidgetStateKeys
                    .PreviousEnabled
            ] ?: false,
        playPauseEnabled =
            this[
                VexStreamPlaybackWidgetStateKeys
                    .PlayPauseEnabled
            ] ?: false,
        showPause =
            this[
                VexStreamPlaybackWidgetStateKeys
                    .ShowPause
            ] ?: false,
        nextEnabled =
            this[
                VexStreamPlaybackWidgetStateKeys
                    .NextEnabled
            ] ?: false,
    )
}

internal fun nextVexStreamPlaybackWidgetDesiredRevision(
    current: VexStreamPlaybackWidgetProviderRead?,
    currentRevision: Long,
    next: VexStreamPlaybackWidgetProviderRead,
): Long =
    if (
        currentRevision > 0L &&
        current ==
        next
    ) {
        currentRevision
    } else {
        currentRevision + 1L
    }

internal fun selectVexStreamPlaybackWidgetProviderRead(
    retained: VexStreamPlaybackWidgetProviderRead?,
    bootstrap: VexStreamPlaybackWidgetProviderRead?,
): VexStreamPlaybackWidgetProviderRead? =
    retained
        ?: bootstrap

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

internal fun vexStreamPlaybackWidgetProviderRead(
    fallbackTitle: String,
    observation:
        VexStreamCurrentPlaybackObservation,
): VexStreamPlaybackWidgetProviderRead {
    val readState =
        observation.readState
    val playPause =
        observation
            .playbackProjection[
                VexStreamPlaybackControl.PlayPause
            ]

    return VexStreamPlaybackWidgetProviderRead(
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
            readState
                .currentArtworkUri
                ?.takeIf {
                    it.isNotBlank()
                },
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

private fun fallbackVexStreamPlaybackWidgetProviderRead(
    context: Context,
): VexStreamPlaybackWidgetProviderRead =
    VexStreamPlaybackWidgetProviderRead(
        title =
            context.getString(
                R.string.vexstream_music_title,
            ),
        artist = "",
        artworkUri = null,
        previousEnabled = false,
        playPauseEnabled = false,
        showPause = false,
        nextEnabled = false,
    )

private suspend fun VexStreamPlaybackWidgetProviderRead.toWidgetProjection(
    context: Context,
): VexStreamPlaybackWidgetProjection {
    val artwork =
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
        VexStreamPlaybackWidgetProjection
    >() {
    override val stateDefinition =
        PreferencesGlanceStateDefinition

    override suspend fun projection(
        context: Context,
        glanceId: GlanceId,
    ): VexStreamPlaybackWidgetProjection {
        val appContext =
            context.applicationContext
        val fallbackTitle =
            appContext.getString(
                R.string.vexstream_music_title,
            )
        val retainedPreferences =
            runCatching {
                getAppWidgetState<
                    Preferences
                >(
                    appContext,
                    glanceId,
                )
            }.getOrNull()
        val retained =
            retainedPreferences
                ?.vexStreamPlaybackWidgetProviderReadOrNull()
        VexStreamPlaybackTrace.record(
            stage =
                "widget.projection.read-retained",
            title =
                retained?.title,
            artist =
                retained?.artist,
            glanceId =
                glanceId.toString(),
            desiredRevision =
                retainedPreferences
                    ?.get(
                        VexStreamPlaybackWidgetStateKeys
                            .DesiredRevision,
                    ),
            renderedRevision =
                retainedPreferences
                    ?.get(
                        VexStreamPlaybackWidgetStateKeys
                            .RenderedRevision,
                    ),
            detail =
                "retained=${retained != null}",
        )
        val bootstrap =
            if (retained == null) {
                withVexStreamWidgetController(
                    appContext,
                ) { controller ->
                    val observation =
                        controller
                            .toVexStreamCurrentPlaybackObservation()
                    VexStreamPlaybackTrace.record(
                        stage =
                            "widget.projection.bootstrap",
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
                    vexStreamPlaybackWidgetProviderRead(
                        fallbackTitle =
                            fallbackTitle,
                        observation =
                            observation,
                    )
                }
            } else {
                null
            }
        val providerRead =
            selectVexStreamPlaybackWidgetProviderRead(
                retained = retained,
                bootstrap = bootstrap,
            )
                ?: fallbackVexStreamPlaybackWidgetProviderRead(
                    appContext,
                )

        VexStreamPlaybackTrace.record(
            stage =
                "widget.projection.ready",
            title =
                providerRead.title,
            artist =
                providerRead.artist,
            glanceId =
                glanceId.toString(),
            detail =
                when {
                    retained != null ->
                        "source=retained"

                    bootstrap != null ->
                        "source=bootstrap"

                    else ->
                        "source=fallback"
                },
        )

        return providerRead
            .toWidgetProjection(
                appContext,
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
        val secondaryText =
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
                Column(
                    modifier =
                        GlanceModifier
                            .clickable(
                                contentAction,
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
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
            val providerRead =
                vexStreamPlaybackWidgetProviderRead(
                    fallbackTitle =
                        context.getString(
                            R.string.vexstream_music_title,
                        ),
                    observation =
                        observation,
                )
            VexStreamPlaybackTrace.record(
                stage =
                    "widget.projection.mapped",
                mediaId =
                    observation.readState
                        .currentMediaId,
                title =
                    providerRead.title,
                artist =
                    providerRead.artist,
                detail =
                    "previous=${providerRead.previousEnabled} " +
                        "playPause=${providerRead.playPauseEnabled} " +
                        "pause=${providerRead.showPause} " +
                        "next=${providerRead.nextEnabled}",
            )
            updateVexStreamPlaybackWidgets(
                context = context,
                providerRead =
                    providerRead,
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

private suspend fun publishVexStreamPlaybackWidgetProviderRead(
    context: Context,
    providerRead: VexStreamPlaybackWidgetProviderRead,
): Boolean {
    val appContext =
        context.applicationContext
    val glanceIds =
        GlanceAppWidgetManager(
            appContext,
        ).getGlanceIds(
            VexStreamPlaybackWidget::class.java,
        )
    var allConverged =
        true

    VexStreamPlaybackTrace.record(
        stage =
            "widget.publish.begin",
        title =
            providerRead.title,
        artist =
            providerRead.artist,
        detail =
            "widgetCount=${glanceIds.size}",
    )

    for (glanceId in glanceIds) {
        var targetRevision =
            0L
        var previousDesiredRevision =
            0L
        var previousRenderedRevision =
            0L
        var shouldRender =
            true

        try {
            updateAppWidgetState(
                appContext,
                glanceId,
            ) { preferences ->
                val current =
                    preferences
                        .vexStreamPlaybackWidgetProviderReadOrNull()
                val currentRevision =
                    preferences[
                        VexStreamPlaybackWidgetStateKeys
                            .DesiredRevision
                    ] ?: 0L
                previousDesiredRevision =
                    currentRevision
                previousRenderedRevision =
                    preferences[
                        VexStreamPlaybackWidgetStateKeys
                            .RenderedRevision
                    ] ?: 0L
                targetRevision =
                    nextVexStreamPlaybackWidgetDesiredRevision(
                        current = current,
                        currentRevision =
                            currentRevision,
                        next = providerRead,
                    )
                shouldRender =
                    !VexStreamPlaybackWidgetCurrentness(
                        desiredRevision =
                            targetRevision,
                        renderedRevision =
                            preferences[
                                VexStreamPlaybackWidgetStateKeys
                                    .RenderedRevision
                            ] ?: 0L,
                    ).isConverged

                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .Title
                ] =
                    providerRead.title
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .Artist
                ] =
                    providerRead.artist
                providerRead
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
                        .PreviousEnabled
                ] =
                    providerRead.previousEnabled
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .PlayPauseEnabled
                ] =
                    providerRead.playPauseEnabled
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .ShowPause
                ] =
                    providerRead.showPause
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .NextEnabled
                ] =
                    providerRead.nextEnabled
                preferences[
                    VexStreamPlaybackWidgetStateKeys
                        .DesiredRevision
                ] =
                    targetRevision
            }

            VexStreamPlaybackTrace.record(
                stage =
                    "widget.state.persisted",
                title =
                    providerRead.title,
                artist =
                    providerRead.artist,
                glanceId =
                    glanceId.toString(),
                desiredRevision =
                    targetRevision,
                renderedRevision =
                    previousRenderedRevision,
                detail =
                    "previousDesired=$previousDesiredRevision " +
                        "shouldRender=$shouldRender",
            )

            if (!shouldRender) {
                VexStreamPlaybackTrace.record(
                    stage =
                        "widget.render.skip-converged",
                    title =
                        providerRead.title,
                    glanceId =
                        glanceId.toString(),
                    desiredRevision =
                        targetRevision,
                    renderedRevision =
                        previousRenderedRevision,
                )
                continue
            }

            VexStreamPlaybackTrace.record(
                stage =
                    "widget.render.request",
                title =
                    providerRead.title,
                glanceId =
                    glanceId.toString(),
                desiredRevision =
                    targetRevision,
                renderedRevision =
                    previousRenderedRevision,
            )
            VexStreamPlaybackWidget
                .update(
                    appContext,
                    glanceId,
                )
            VexStreamPlaybackTrace.record(
                stage =
                    "widget.render.returned",
                title =
                    providerRead.title,
                glanceId =
                    glanceId.toString(),
                desiredRevision =
                    targetRevision,
            )

            var markedRendered =
                false
            var observedDesiredRevision =
                0L
            updateAppWidgetState(
                appContext,
                glanceId,
            ) { preferences ->
                val desiredRevision =
                    preferences[
                        VexStreamPlaybackWidgetStateKeys
                            .DesiredRevision
                    ] ?: 0L
                observedDesiredRevision =
                    desiredRevision
                if (
                    desiredRevision ==
                    targetRevision
                ) {
                    preferences[
                        VexStreamPlaybackWidgetStateKeys
                            .RenderedRevision
                    ] =
                        targetRevision
                    markedRendered =
                        true
                } else {
                    allConverged =
                        false
                }
            }
            VexStreamPlaybackTrace.record(
                stage =
                    "widget.render.marked",
                title =
                    providerRead.title,
                glanceId =
                    glanceId.toString(),
                desiredRevision =
                    observedDesiredRevision,
                renderedRevision =
                    if (
                        markedRendered
                    ) {
                        targetRevision
                    } else {
                        previousRenderedRevision
                    },
                detail =
                    "marked=$markedRendered",
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            VexStreamPlaybackTrace.record(
                stage =
                    "widget.render.failure",
                title =
                    providerRead.title,
                glanceId =
                    glanceId.toString(),
                desiredRevision =
                    targetRevision,
                renderedRevision =
                    previousRenderedRevision,
                detail =
                    "${failure::class.simpleName}:${failure.message.orEmpty()}",
            )
            /*
             * Desired provider truth, when already persisted, remains available to the next pass.
             * A launcher/state failure is widget-currentness failure, not playback failure.
             */
            allConverged =
                false
        }
    }

    return allConverged
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

internal suspend fun <Id> rerenderRetainedVexStreamPlaybackWidgetProjections(
    ids: Iterable<Id>,
    hasRetainedProjection:
        suspend (Id) -> Boolean,
    render:
        suspend (Id) -> Unit,
) {
    for (id in ids) {
        if (
            hasRetainedProjection(
                id,
            )
        ) {
            render(
                id,
            )
        }
    }
}

internal suspend fun updateVexStreamPlaybackWidgets(
    context: Context,
) {
    val appContext =
        context.applicationContext
    val glanceIds =
        GlanceAppWidgetManager(
            appContext,
        ).getGlanceIds(
            VexStreamPlaybackWidget::class.java,
        )

    rerenderRetainedVexStreamPlaybackWidgetProjections(
        ids = glanceIds,
        hasRetainedProjection = { glanceId ->
            runCatching {
                VexStreamPlaybackWidget
                    .getAppWidgetState<
                        Preferences
                    >(
                        appContext,
                        glanceId,
                    )
                    .vexStreamPlaybackWidgetProviderReadOrNull() !=
                    null
            }.getOrDefault(
                false,
            )
        },
        render = { glanceId ->
            VexStreamPlaybackTrace.record(
                stage =
                    "widget.presentation-rerender.request",
                glanceId =
                    glanceId.toString(),
            )
            try {
                VexStreamPlaybackWidget
                    .update(
                        appContext,
                        glanceId,
                    )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                /*
                 * Presentation-only re-render is best-effort. The retained provider projection
                 * remains authoritative for the next ordinary provider publication.
                 */
            }
        },
    )
}

internal suspend fun updateVexStreamPlaybackWidgets(
    context: Context,
    providerRead:
        VexStreamPlaybackWidgetProviderRead,
) {
    retryVexStreamPlaybackWidgetPublication(
        publish = {
            publishVexStreamPlaybackWidgetProviderRead(
                context = context,
                providerRead = providerRead,
            )
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

internal const val VEXSTREAM_WIDGET_CONTROL_TOUCH_TARGET_DP =
    48
internal const val VEXSTREAM_WIDGET_CONTROL_SYMBOL_SP =
    24
internal const val VEXSTREAM_WIDGET_CONTROL_GAP_DP =
    6
internal const val VEXSTREAM_WIDGET_ARTWORK_SIZE_DP =
    60
internal const val VEXSTREAM_WIDGET_PUBLICATION_RETRY_MILLIS =
    250L

private const val CONTROLLER_CONNECT_TIMEOUT_MS =
    2_000L
private const val WIDGET_ARTWORK_MAX_PX =
    128

// [VXG RealForever]
