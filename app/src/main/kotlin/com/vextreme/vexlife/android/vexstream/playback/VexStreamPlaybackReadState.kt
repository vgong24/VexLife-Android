package com.vextreme.vexlife.android.vexstream.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueProjection
import com.vextreme.vexlife.android.vexstream.queue.toVexStreamQueueProjection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provider-derived Playback read data used by view-facing DisplayState.
 *
 * This is not the semantic [VexStreamPlaybackState]. It carries Media3 read data such as metadata,
 * queue position and progress that the view needs alongside the Behavior Projection.
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
 * One provider-observed playback snapshot.
 *
 * Media3 Player / MediaSession remains canonical truth. This value only composes the read facts and
 * existing Playback / Queue projections that remote or in-app surfaces need to render one moment.
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
 * Playback-lifetime observation interface.
 *
 * Consumers depend on the immutable observation stream rather than independently timing provider
 * reads. The observer owns only Media3 listener registration; it does not own playback State.
 */
internal interface VexStreamCurrentPlaybackObserver :
    AutoCloseable {
    val observations:
        StateFlow<
            VexStreamCurrentPlaybackObservation
        >
}

/**
 * Media3 realization of the current-playback observer.
 *
 * Player.Listener callbacks run on the Player application looper. Projecting the complete
 * observation from onEvents therefore preserves Media3's ordering/thread contract before values
 * cross into other execution surfaces.
 */
internal class Media3VexStreamCurrentPlaybackObserver(
    private val player: Player,
) :
    VexStreamCurrentPlaybackObserver {
    private val mutableObservations =
        MutableStateFlow(
            player
                .toVexStreamCurrentPlaybackObservation(),
        )

    override val observations:
        StateFlow<
            VexStreamCurrentPlaybackObservation
        > =
        mutableObservations
            .asStateFlow()

    private val listener =
        object :
            Player.Listener {
            override fun onEvents(
                player: Player,
                events: Player.Events,
            ) {
                val observed =
                    player
                        .toVexStreamCurrentPlaybackObservation()

                if (
                    mutableObservations.value !=
                    observed
                ) {
                    mutableObservations.value =
                        observed
                }
            }
        }

    init {
        player.addListener(
            listener,
        )
    }

    override fun close() {
        player.removeListener(
            listener,
        )
    }
}

internal fun Player.toVexStreamCurrentPlaybackObservation():
    VexStreamCurrentPlaybackObservation =
    VexStreamCurrentPlaybackObservation(
        readState =
            toVexStreamPlaybackReadState(),
        playbackProjection =
            toVexStreamPlaybackProjection(),
        queueProjection =
            toVexStreamQueueProjection(),
    )

@Composable
internal fun rememberVexStreamPlaybackReadState(
    controller:
        MediaController?,
): State<
    VexStreamPlaybackReadState
> {
    val projected =
        remember(
            controller,
        ) {
            mutableStateOf(
                controller
                    ?.toVexStreamPlaybackReadState()
                    ?: VexStreamPlaybackReadState
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
                VexStreamPlaybackReadState
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
                    projected.value =
                        player
                            .toVexStreamPlaybackReadState()
                }
            }

        controller.addListener(
            listener,
        )
        projected.value =
            controller
                .toVexStreamPlaybackReadState()

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
                    .toVexStreamPlaybackReadState()

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
