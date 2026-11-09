package com.vextreme.vexlife.android.vexstream.playback

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    val queueSize:
        Int =
        0,
    val currentIndex: Int = -1,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
) {
    val hasQueue: Boolean
        get() =
            queueSize >
                0

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

@Composable
internal fun rememberVexStreamPlaybackReadState(
    baseReadState:
        VexStreamPlaybackReadState,
    progressConnection:
        VexStreamPlaybackProgressConnection?,
):
    State<
        VexStreamPlaybackReadState
    > {
    val progress =
        remember(
            progressConnection,
            baseReadState.currentMediaId,
            baseReadState.currentIndex,
        ) {
            mutableStateOf(
                progressConnection
                    ?.readProgress()
                    ?: VexStreamPlaybackProgress
                        .Empty,
            )
        }

    LaunchedEffect(
        baseReadState.currentMediaId,
        baseReadState.currentIndex,
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
        baseReadState.currentMediaId,
        baseReadState.currentIndex,
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
        baseReadState.copy(
            currentPositionMs =
                progress.value
                    .positionMs,
            durationMs =
                progress.value
                    .durationMs,
            bufferedPositionMs =
                progress.value
                    .bufferedPositionMs,
        ),
    )
}

// [VXG RealForever]
