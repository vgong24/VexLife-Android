package com.vextreme.vexlife.android.vexstream.playback

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong

/**
 * View-facing Playback read data composed from the one retained Player Store truth plus bounded
 * controller timeline telemetry.
 *
 * Current media identity, metadata, queue membership and queue index come from [VexStreamPlayerStore].
 * MediaController contributes only continuously changing position/duration/buffer telemetry. The
 * telemetry carries media/index coordinates solely as a freshness fence so stale timing data cannot
 * be applied to newer Store-owned identity.
 */
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

internal data class VexStreamPlaybackTelemetry(
    val observedMediaId: String? = null,
    val observedMediaItemIndex: Int = -1,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
) {
    companion object {
        val Empty =
            VexStreamPlaybackTelemetry()
    }
}

@Composable
internal fun rememberVexStreamPlaybackTelemetry(
    controller:
        MediaController?,
): State<
    VexStreamPlaybackTelemetry
> {
    val projected =
        remember(
            controller,
        ) {
            mutableStateOf(
                controller
                    ?.toVexStreamPlaybackTelemetry()
                    ?: VexStreamPlaybackTelemetry
                        .Empty,
            )
        }

    DisposableEffect(
        controller,
    ) {
        if (
            controller == null
        ) {
            projected.value =
                VexStreamPlaybackTelemetry
                    .Empty
            return@DisposableEffect onDispose {}
        }

        val listener =
            object :
                Player.Listener {
                override fun onEvents(
                    player: Player,
                    events:
                        Player.Events,
                ) {
                    val current =
                        player
                            .toVexStreamPlaybackTelemetry()
                    projected.value =
                        current
                    VexStreamPlaybackTrace.record(
                        stage =
                            "full-player.telemetry",
                        mediaId =
                            current
                                .observedMediaId,
                        detail =
                            "index=" +
                                current
                                    .observedMediaItemIndex,
                    )
                }
            }

        controller.addListener(
            listener,
        )
        projected.value =
            controller
                .toVexStreamPlaybackTelemetry()

        onDispose {
            controller.removeListener(
                listener,
            )
        }
    }

    LaunchedEffect(
        controller,
    ) {
        if (
            controller == null
        ) {
            return@LaunchedEffect
        }

        while (true) {
            projected.value =
                controller
                    .toVexStreamPlaybackTelemetry()

            delay(
                if (
                    controller.isPlaying
                ) {
                    250L
                } else {
                    1_000L
                },
            )
        }
    }

    return projected
}

internal fun VexStreamPlayerStore.Snapshot
    .toVexStreamPlaybackReadState(
        telemetry:
            VexStreamPlaybackTelemetry,
    ):
    VexStreamPlaybackReadState {
    val available =
        this as?
            VexStreamPlayerStore.Snapshot.Available
            ?: return VexStreamPlaybackReadState.Empty
    val queueMediaIds =
        available.queue
            .items
            .mediaIds
    val currentIndex =
        available.queue
            .navigation
            .currentIndex
            .takeIf { index ->
                index in
                    queueMediaIds.indices
            }
            ?: -1
    val telemetryIsCurrent =
        telemetry.observedMediaId ==
            available.currentMedia.mediaId &&
            telemetry.observedMediaItemIndex ==
            currentIndex

    return VexStreamPlaybackReadState(
        currentMediaId =
            available.currentMedia
                .mediaId,
        currentTitle =
            available.currentMedia
                .title,
        currentArtist =
            available.currentMedia
                .artist,
        currentArtworkUri =
            available.currentMedia
                .artworkUri,
        queueMediaIds =
            queueMediaIds,
        currentIndex =
            currentIndex,
        currentPositionMs =
            if (
                telemetryIsCurrent
            ) {
                telemetry.currentPositionMs
            } else {
                0L
            },
        durationMs =
            if (
                telemetryIsCurrent
            ) {
                telemetry.durationMs
            } else {
                0L
            },
        bufferedPositionMs =
            if (
                telemetryIsCurrent
            ) {
                telemetry.bufferedPositionMs
            } else {
                0L
            },
    )
}

internal fun Player
    .toVexStreamPlaybackTelemetry():
    VexStreamPlaybackTelemetry {
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

    return VexStreamPlaybackTelemetry(
        observedMediaId =
            current?.mediaId,
        observedMediaItemIndex =
            if (
                mediaItemCount >
                0
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
