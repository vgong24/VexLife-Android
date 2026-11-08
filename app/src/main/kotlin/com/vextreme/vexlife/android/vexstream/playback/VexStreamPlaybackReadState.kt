package com.vextreme.vexlife.android.vexstream.playback

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.C
import androidx.media3.common.Player
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueProjection
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueBehavior
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueControls
import com.vextreme.vexlife.android.vexstream.queue.vexStreamQueueState
import com.vextreme.vexlife.android.vexstream.queue.toVexStreamQueueProjection
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong

/**
 * Provider-derived Playback read data used by view-facing DisplayState.
 *
 * This is not the semantic [VexStreamPlaybackState]. It carries Media3 read data such as metadata,
 * queue position and progress that the view needs alongside the Behavior Projection.
 */
internal data class VexStreamPlaybackProgress(
    val positionMs:
        Long =
        0L,
    val durationMs:
        Long =
        0L,
    val bufferedPositionMs:
        Long =
        0L,
) {
    companion object {
        val Empty =
            VexStreamPlaybackProgress()
    }
}

internal interface VexStreamPlaybackProgressConnection {
    val isPlaying:
        Boolean

    fun readProgress():
        VexStreamPlaybackProgress
}

internal data class VexStreamPlaybackReadState(
    val currentMediaId: String? = null,
    val currentTitle: String = "",
    val currentArtist: String = "",
    val currentArtworkUri: String? = null,
    val queueMediaIds:
        List<String> =
        emptyList(),
    val currentIndex: Int = -1,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
) {
    val hasQueue: Boolean
        get() =
            queueMediaIds.isNotEmpty()

    val progressFraction: Float
        get() =
            if (
                durationMs > 0L
            ) {
                (
                    currentPositionMs
                        .toFloat() /
                        durationMs
                            .toFloat()
                ).coerceIn(
                    0f,
                    1f,
                )
            } else {
                0f
            }

    val bufferedFraction: Float
        get() =
            if (
                durationMs > 0L
            ) {
                (
                    bufferedPositionMs
                        .toFloat() /
                        durationMs
                            .toFloat()
                ).coerceIn(
                    0f,
                    1f,
                )
            } else {
                0f
            }

    companion object {
        val Empty =
            VexStreamPlaybackReadState()
    }
}

/**
 * One surface-ready playback observation projected from retained Player Partner CURRENT plus the
 * process-shared Playback Family projection.
 *
 * This value owns no currentness. The Player Store owns outside CURRENT and Playback Family owns
 * semantic Playback CURRENT.
 */
internal data class VexStreamCurrentPlaybackObservation(
    val readState:
        VexStreamPlaybackReadState,
    val playbackProjection:
        VexStreamPlaybackProjection,
    val queueProjection:
        VexStreamQueueProjection,
)

/**
 * Feature-local live diagnostic trace for the current-playback projection chain.
 *
 * VexLogger remains canonical semantic user-action history. These Logcat breadcrumbs expose
 * provider/widget transport boundaries so lived debugging can identify the first stale handoff.
 */
internal object VexStreamPlaybackTrace {
    const val TAG =
        "VexStreamPlayback"

    private val nextSequence =
        AtomicLong(
            1L,
        )

    fun record(
        stage: String,
        mediaId: String? = null,
        title: String? = null,
        artist: String? = null,
        glanceId: String? = null,
        desiredRevision: Long? = null,
        renderedRevision: Long? = null,
        detail: String? = null,
    ) {
        val sequence =
            nextSequence
                .getAndIncrement()

        try {
            Log.i(
                TAG,
                message(
                    sequence = sequence,
                    stage = stage,
                    mediaId = mediaId,
                    title = title,
                    artist = artist,
                    glanceId = glanceId,
                    desiredRevision =
                        desiredRevision,
                    renderedRevision =
                        renderedRevision,
                    detail = detail,
                ),
            )
        } catch (_: Throwable) {
            // Logcat diagnostics must never affect playback or widget behavior.
        }
    }

    internal fun message(
        sequence: Long,
        stage: String,
        mediaId: String? = null,
        title: String? = null,
        artist: String? = null,
        glanceId: String? = null,
        desiredRevision: Long? = null,
        renderedRevision: Long? = null,
        detail: String? = null,
    ): String {
        require(
            stage.isNotBlank(),
        ) {
            "trace stage must not be blank"
        }

        return buildString {
            append(
                "seq=",
            )
            append(
                sequence,
            )
            append(
                " stage=",
            )
            append(
                clean(
                    stage,
                ),
            )
            mediaId?.let {
                appendField(
                    "mediaId",
                    it,
                )
            }
            title?.let {
                appendField(
                    "title",
                    it,
                )
            }
            artist?.let {
                appendField(
                    "artist",
                    it,
                )
            }
            glanceId?.let {
                appendField(
                    "glanceId",
                    it,
                )
            }
            desiredRevision?.let {
                append(
                    " desired=",
                )
                append(
                    it,
                )
            }
            renderedRevision?.let {
                append(
                    " rendered=",
                )
                append(
                    it,
                )
            }
            detail
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    appendField(
                        "detail",
                        it,
                    )
                }
        }
    }

    private fun StringBuilder.appendField(
        name: String,
        value: String,
    ) {
        append(
            " ",
        )
        append(
            name,
        )
        append(
            "=",
        )
        append(
            clean(
                value,
            ),
        )
    }

    private fun clean(
        value: String,
    ): String =
        value
            .replace(
                '\n',
                ' ',
            )
            .replace(
                '\r',
                ' ',
            )
            .replace(
                ' ',
                '~',
            )
            .take(
                180,
            )
}

/**
 * Project retained Player Partner CURRENT for consumers that do not need a live progress clock.
 *
 * Position/duration/buffered progress is intentionally not claimed by Player CURRENT because normal
 * Media3 playback time advances without a Player.Events occurrence. The existing UI progress reader
 * remains an explicit provider-read seam until a truthful maintained observation is formed.
 */
internal fun VexStreamPlayerPartner.Current.toVexStreamCurrentPlaybackObservation(
    playbackFamily:
        VexStreamPlaybackFamily,
):
    VexStreamCurrentPlaybackObservation =
    VexStreamCurrentPlaybackObservation(
        readState =
            toVexStreamPlaybackReadState(),
        playbackProjection =
            playbackFamily
                .currentPlaybackProjection,
        queueProjection =
            toVexStreamQueueProjection(),
    )

private fun VexStreamPlayerPartner.Current.toVexStreamPlaybackReadState(
    progress:
        VexStreamPlaybackProgress =
        VexStreamPlaybackProgress.Empty,
):
    VexStreamPlaybackReadState =
    when (
        this
    ) {
        VexStreamPlayerPartner.Current
            .Unavailable ->
            VexStreamPlaybackReadState
                .Empty

        is VexStreamPlayerPartner.Current
            .Available ->
            VexStreamPlaybackReadState(
                currentMediaId =
                    currentMedia.mediaId,
                currentTitle =
                    currentMedia.title,
                currentArtist =
                    currentMedia.artist,
                currentArtworkUri =
                    currentMedia.artworkUri,
                queueMediaIds =
                    queueItems.mediaIds,
                currentIndex =
                    queueNavigation.currentIndex,
                currentPositionMs =
                    progress.positionMs,
                durationMs =
                    progress.durationMs,
                bufferedPositionMs =
                    progress.bufferedPositionMs,
            )
    }

private fun VexStreamPlayerPartner.Current.toVexStreamQueueProjection():
    VexStreamQueueProjection {
    val state =
        when (
            this
        ) {
            VexStreamPlayerPartner.Current
                .Unavailable ->
                vexStreamQueueState(
                    mediaItemCount =
                        0,
                    previousAvailable =
                        false,
                    nextAvailable =
                        false,
                    shuffleAvailable =
                        false,
                    clearAvailable =
                        false,
                )

            is VexStreamPlayerPartner.Current
                .Available ->
                vexStreamQueueState(
                    mediaItemCount =
                        queueItems
                            .mediaIds
                            .size,
                    previousAvailable =
                        queueNavigation
                            .previousAvailable,
                    nextAvailable =
                        queueNavigation
                            .nextAvailable,
                    shuffleAvailable =
                        queueNavigation
                            .shuffleAvailable,
                    clearAvailable =
                        queueNavigation
                            .clearAvailable,
                )
        }

    return vexStreamQueueBehavior()
        .vexStreamQueueControls()
        .projection(
            state,
        )
}

/**
 * Temporary Widget bootstrap bridge.
 *
 * Continuous Service observation no longer uses this provider->Family shortcut. The Widget
 * bootstrap remains a named successor seam until it consumes the process Player Store directly.
 */
internal fun Player.toVexStreamCurrentPlaybackObservation(
    playbackFamily:
        VexStreamPlaybackFamily,
):
    VexStreamCurrentPlaybackObservation {
    val reconciled =
        reconcileVexStreamPlaybackObservation(
            observed =
                toVexStreamPlaybackState(),
            current =
                playbackFamily
                    .currentState,
        )

    playbackFamily.observe(
        reconciled,
    )

    return VexStreamCurrentPlaybackObservation(
        readState =
            toVexStreamPlaybackReadState(),
        playbackProjection =
            playbackFamily
                .currentPlaybackProjection,
        queueProjection =
            toVexStreamQueueProjection(),
    )
}

@Composable
internal fun rememberVexStreamPlaybackReadState():
    State<
        VexStreamPlaybackReadState
    > {
    val owner:
        VexStreamPlaybackConnectionViewModel =
        viewModel()
    val playerCurrent by
        owner
            .playerStore
            .states
            .collectAsStateWithLifecycle(
                initialValue =
                    owner
                        .playerStore
                        .current,
            )
    val progressConnection =
        owner.progressConnection
    val progress =
        remember(
            progressConnection,
        ) {
            mutableStateOf(
                progressConnection
                    ?.readProgress()
                    ?: VexStreamPlaybackProgress
                        .Empty,
            )
        }

    LaunchedEffect(
        playerCurrent,
        progressConnection,
    ) {
        progress.value =
            progressConnection
                ?.readProgress()
                ?: VexStreamPlaybackProgress
                    .Empty
    }

    LaunchedEffect(
        progressConnection,
    ) {
        if (
            progressConnection ==
            null
        ) {
            return@LaunchedEffect
        }

        while (true) {
            progress.value =
                progressConnection
                    .readProgress()

            delay(
                if (
                    progressConnection.isPlaying
                ) {
                    250L
                } else {
                    1_000L
                },
            )
        }
    }

    return rememberUpdatedState(
        playerCurrent
            .toVexStreamPlaybackReadState(
                progress.value,
            ),
    )
}

internal fun Player
    .toVexStreamPlaybackReadState():
    VexStreamPlaybackReadState {
    val current =
        currentMediaItem

    val durationMs =
        duration.takeIf {
            it != C.TIME_UNSET &&
                it > 0L
        } ?: 0L

    val positionMs =
        currentPosition
            .coerceAtLeast(
                0L,
            )
            .let {
                if (
                    durationMs > 0L
                ) {
                    it.coerceAtMost(
                        durationMs,
                    )
                } else {
                    it
                }
            }

    val bufferedMs =
        bufferedPosition
            .coerceAtLeast(
                0L,
            )
            .let {
                if (
                    durationMs > 0L
                ) {
                    it.coerceAtMost(
                        durationMs,
                    )
                } else {
                    it
                }
            }

    return VexStreamPlaybackReadState(
        currentMediaId =
            current?.mediaId,
        currentTitle =
            current
                ?.mediaMetadata
                ?.title
                ?.toString()
                .orEmpty(),
        currentArtist =
            current
                ?.mediaMetadata
                ?.artist
                ?.toString()
                .orEmpty(),
        currentArtworkUri =
            current
                ?.mediaMetadata
                ?.artworkUri
                ?.toString(),
        queueMediaIds =
            (0 until mediaItemCount)
                .map { index ->
                    getMediaItemAt(
                        index,
                    ).mediaId
                },
        currentIndex =
            if (
                mediaItemCount > 0
            ) {
                currentMediaItemIndex
            } else {
                -1
            },
        currentPositionMs =
            positionMs,
        durationMs =
            durationMs,
        bufferedPositionMs =
            bufferedMs,
    )
}

// [VXG RealForever]
