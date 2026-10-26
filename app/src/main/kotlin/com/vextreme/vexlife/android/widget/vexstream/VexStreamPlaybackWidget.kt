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
import androidx.media3.common.Player
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
import com.vextreme.vexlife.android.vexstream.playback.toVexStreamPlaybackProjection
import com.vextreme.vexlife.android.vexstream.queue.Media3VexStreamQueueConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import com.vextreme.vexlife.android.vexstream.queue.toVexStreamQueueProjection
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
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
    context: Context,
    player: Player,
): VexStreamPlaybackWidgetProviderRead {
    val playbackProjection =
        player.toVexStreamPlaybackProjection()
    val queueProjection =
        player.toVexStreamQueueProjection()
    val metadata =
        player
            .currentMediaItem
            ?.mediaMetadata
    val playPause =
        playbackProjection[
            VexStreamPlaybackControl.PlayPause
        ]

    return VexStreamPlaybackWidgetProviderRead(
        title =
            metadata
                ?.title
                ?.toString()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: context.getString(
                    R.string.vexstream_music_title,
                ),
        artist =
            metadata
                ?.artist
                ?.toString()
                .orEmpty(),
        artworkUri =
            metadata
                ?.artworkUri
                ?.toString()
                ?.takeIf {
                    it.isNotBlank()
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
        val retained =
            runCatching {
                getAppWidgetState<
                    Preferences
                >(
                    appContext,
                    glanceId,
                ).vexStreamPlaybackWidgetProviderReadOrNull()
            }.getOrNull()
        val bootstrap =
            if (retained == null) {
                withVexStreamWidgetController(
                    appContext,
                ) { controller ->
                    vexStreamPlaybackWidgetProviderRead(
                        appContext,
                        controller,
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

        updateVexStreamPlaybackWidgets(
            context,
        )
    }
}

/**
 * MediaSessionService-owned widget projection publication loop.
 *
 * The Media3 listener derives an immutable provider projection while already on the Player looper,
 * then enqueues only that value here. Managed worker execution never dereferences Player.
 */
internal class VexStreamPlaybackWidgetUpdateExecution(
    root: VexExecutionRoot,
    private val context: Context,
) : VexExecution(
        root,
    ) {
    private val requests =
        Channel<
            VexStreamPlaybackWidgetProviderRead
        >(
            capacity =
                Channel.CONFLATED,
        )

    init {
        launchForCollect(
            source =
                ::requestsForExecution,
        ) { providerRead ->
            updateVexStreamPlaybackWidgets(
                context = context,
                providerRead = providerRead,
            )
        }
    }

    fun requestUpdate(
        providerRead:
            VexStreamPlaybackWidgetProviderRead,
    ): Boolean =
        requests
            .trySend(
                providerRead,
            )
            .isSuccess

    private fun requestsForExecution():
        Flow<
            VexStreamPlaybackWidgetProviderRead
        > =
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

internal suspend fun convergeVexStreamPlaybackWidgetProjection(
    refresh: suspend () -> Unit,
    awaitProviderConvergence: suspend () -> Unit,
) {
    refresh()
    awaitProviderConvergence()
    refresh()
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

    for (glanceId in glanceIds) {
        try {
            var targetRevision =
                0L
            var shouldRender =
                true
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

            if (!shouldRender) {
                continue
            }

            VexStreamPlaybackWidget
                .update(
                    appContext,
                    glanceId,
                )

            updateAppWidgetState(
                appContext,
                glanceId,
            ) { preferences ->
                val desiredRevision =
                    preferences[
                        VexStreamPlaybackWidgetStateKeys
                            .DesiredRevision
                    ] ?: 0L
                if (
                    desiredRevision ==
                    targetRevision
                ) {
                    preferences[
                        VexStreamPlaybackWidgetStateKeys
                            .RenderedRevision
                    ] =
                        targetRevision
                } else {
                    allConverged =
                        false
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
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
                VEXSTREAM_WIDGET_PROVIDER_CONVERGENCE_MILLIS,
            )
        },
    )
}

internal suspend fun updateVexStreamPlaybackWidgets(
    context: Context,
    providerRead:
        suspend () ->
        VexStreamPlaybackWidgetProviderRead?,
) {
    /*
     * Each pass first publishes canonical provider-derived presentation truth into widget-specific
     * Glance state, then asks the launcher to render that exact desired revision. The second pass is
     * still a bounded Media3-settlement retry, but desired-vs-rendered revision state now makes
     * currentness explicit instead of treating retry timing as convergence itself.
     */
    convergeVexStreamPlaybackWidgetProjection(
        refresh = {
            providerRead()
                ?.let { current ->
                    publishVexStreamPlaybackWidgetProviderRead(
                        context = context,
                        providerRead = current,
                    )
                }
        },
        awaitProviderConvergence = {
            delay(
                VEXSTREAM_WIDGET_PROVIDER_CONVERGENCE_MILLIS,
            )
        },
    )
}

internal suspend fun updateVexStreamPlaybackWidgets(
    context: Context,
) {
    updateVexStreamPlaybackWidgets(
        context = context,
        providerRead = {
            withVexStreamWidgetController(
                context,
            ) { controller ->
                vexStreamPlaybackWidgetProviderRead(
                    context.applicationContext,
                    controller,
                )
            }
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
internal const val VEXSTREAM_WIDGET_PROVIDER_CONVERGENCE_MILLIS =
    250L

private const val CONTROLLER_CONNECT_TIMEOUT_MS =
    2_000L
private const val WIDGET_ARTWORK_MAX_PX =
    128

// [VXG RealForever]
