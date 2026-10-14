package com.vextreme.vexlife.android.media.vexstream

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureContract
import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureMap
import com.vextreme.vexlife.android.vexarchitecture.VexControl
import com.vextreme.vexlife.android.vexarchitecture.VexCurrent
import com.vextreme.vexlife.android.vexarchitecture.VexPlacementView
import com.vextreme.vexlife.android.vexarchitecture.VexTransition
import com.vextreme.vexlife.android.vexarchitecture.architectureMap
import com.vextreme.vexlife.android.vexarchitecture.control
import com.vextreme.vexlife.android.vexarchitecture.placement
import com.vextreme.vexlife.android.vexarchitecture.transition
import com.vextreme.vexlife.android.vexarchitecture.vexCurrent

object VexStreamPlaybackArchitecture :
    VexArchitectureContract {

    override val architectureRef: String =
        "vexstream.playback"

    override val enabled: Boolean =
        true

    interface CaseContract
    interface ControlContract
    interface PlacementContract
    interface AndroidContract

    val controls: VexStreamPlaybackControl
        get() = VexStreamPlaybackControl

    val placements: VexStreamPlaybackPlacement
        get() = VexStreamPlaybackPlacement

    internal val android: VexStreamPlaybackAndroid
        get() = VexStreamPlaybackAndroid

    override val map: VexArchitectureMap by lazy {
        architectureMap(
            placements =
                placements.all,
            controls =
                controls.all,
        )
    }
}

/**
 * Queue-position facts are carried only in Cases where a queue actually exists.
 *
 * The flags are observed from Media3 rather than re-derived from index arithmetic so platform queue
 * traversal semantics stay at the Android boundary.
 */
data class VexStreamPlaybackQueuePosition(
    val itemCount: Int,
    val currentIndex: Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
) {
    init {
        require(itemCount > 0)
        require(currentIndex in -1 until itemCount)
    }
}

sealed interface VexStreamPlaybackCase :
    VexStreamPlaybackArchitecture.CaseContract {

    data object QueueEmpty :
        VexStreamPlaybackCase

    data class ReadyToPlay(
        val queue: VexStreamPlaybackQueuePosition,
    ) : VexStreamPlaybackCase

    data class PlayRequested(
        val queue: VexStreamPlaybackQueuePosition,
    ) : VexStreamPlaybackCase
}

object VexStreamPlaybackControl :
    VexStreamPlaybackArchitecture.ControlContract {

    val Play =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.ReadyToPlay,
            VexStreamPlaybackCase.PlayRequested
        >(
            "vexstream.playback.play",
        )

    val Pause =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlayRequested,
            VexStreamPlaybackCase.ReadyToPlay
        >(
            "vexstream.playback.pause",
        )

    /**
     * Play Track replaces/selects the Media3 queue context and requests playback.
     *
     * The human Control is one intention, while its typed relationship depends on the current Case.
     * All three converge on PlayRequested; the exact selected track/queue data is observed from Media3
     * after the Android realization.
     */
    val PlayTrackFromQueueEmpty =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.QueueEmpty,
            VexStreamPlaybackCase.PlayRequested
        >(
            "vexstream.playback.play-track.queue-empty",
        )

    val PlayTrackFromReady =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.ReadyToPlay,
            VexStreamPlaybackCase.PlayRequested
        >(
            "vexstream.playback.play-track.ready",
        )

    val PlayTrackFromPlayRequested =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlayRequested,
            VexStreamPlaybackCase.PlayRequested
        >(
            "vexstream.playback.play-track.play-requested",
        )

    val NextWhileReady =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.ReadyToPlay,
            VexStreamPlaybackCase.ReadyToPlay
        >(
            transitionRef =
                "vexstream.playback.next.ready",
            conditionRef =
                "queue-has-next",
        ) { current ->
            current.queue.hasNext
        }

    val NextWhilePlayRequested =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlayRequested,
            VexStreamPlaybackCase.PlayRequested
        >(
            transitionRef =
                "vexstream.playback.next.play-requested",
            conditionRef =
                "queue-has-next",
        ) { current ->
            current.queue.hasNext
        }

    val PreviousWhileReady =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.ReadyToPlay,
            VexStreamPlaybackCase.ReadyToPlay
        >(
            transitionRef =
                "vexstream.playback.previous.ready",
            conditionRef =
                "queue-has-previous",
        ) { current ->
            current.queue.hasPrevious
        }

    val PreviousWhilePlayRequested =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlayRequested,
            VexStreamPlaybackCase.PlayRequested
        >(
            transitionRef =
                "vexstream.playback.previous.play-requested",
            conditionRef =
                "queue-has-previous",
        ) { current ->
            current.queue.hasPrevious
        }

    val PlayPause =
        VexStreamPlaybackArchitecture.control(
            "vexstream.playback.play-pause",
            Play,
            Pause,
        )

    val PlayTrack =
        VexStreamPlaybackArchitecture.control(
            "vexstream.playback.play-track",
            PlayTrackFromQueueEmpty,
            PlayTrackFromReady,
            PlayTrackFromPlayRequested,
        )

    val Next =
        VexStreamPlaybackArchitecture.control(
            "vexstream.playback.next",
            NextWhileReady,
            NextWhilePlayRequested,
        )

    val Previous =
        VexStreamPlaybackArchitecture.control(
            "vexstream.playback.previous",
            PreviousWhileReady,
            PreviousWhilePlayRequested,
        )

    val all: List<VexControl> =
        listOf(
            PlayPause,
            PlayTrack,
            Previous,
            Next,
        )
}

object VexStreamPlaybackPlacement :
    VexStreamPlaybackArchitecture.PlacementContract {

    val QueueEmpty =
        VexStreamPlaybackArchitecture.placement<
            VexStreamPlaybackCase.QueueEmpty
        > {
            can(
                VexStreamPlaybackControl.PlayTrackFromQueueEmpty,
            )
        }

    val ReadyToPlay =
        VexStreamPlaybackArchitecture.placement<
            VexStreamPlaybackCase.ReadyToPlay
        > {
            arrivedBy(
                VexStreamPlaybackControl.Pause,
            )
            can(
                VexStreamPlaybackControl.PlayTrackFromReady,
            )
            arrivedBy(
                VexStreamPlaybackControl.NextWhileReady,
            )
            arrivedBy(
                VexStreamPlaybackControl.PreviousWhileReady,
            )
            can(
                VexStreamPlaybackControl.Play,
            )
            can(
                VexStreamPlaybackControl.NextWhileReady,
            )
            can(
                VexStreamPlaybackControl.PreviousWhileReady,
            )
        }

    val PlayRequested =
        VexStreamPlaybackArchitecture.placement<
            VexStreamPlaybackCase.PlayRequested
        > {
            arrivedBy(
                VexStreamPlaybackControl.Play,
            )
            arrivedBy(
                VexStreamPlaybackControl.PlayTrackFromQueueEmpty,
            )
            arrivedBy(
                VexStreamPlaybackControl.PlayTrackFromReady,
            )
            arrivedBy(
                VexStreamPlaybackControl.PlayTrackFromPlayRequested,
            )
            can(
                VexStreamPlaybackControl.PlayTrackFromPlayRequested,
            )
            arrivedBy(
                VexStreamPlaybackControl.NextWhilePlayRequested,
            )
            arrivedBy(
                VexStreamPlaybackControl.PreviousWhilePlayRequested,
            )
            can(
                VexStreamPlaybackControl.Pause,
            )
            can(
                VexStreamPlaybackControl.NextWhilePlayRequested,
            )
            can(
                VexStreamPlaybackControl.PreviousWhilePlayRequested,
            )
        }

    val all: List<VexPlacementView> =
        listOf(
            QueueEmpty,
            ReadyToPlay,
            PlayRequested,
        )
}

data class VexStreamPlaybackObservation(
    val queue: VexStreamPlaybackQueuePosition?,
    val playRequested: Boolean,
)

internal object VexStreamPlaybackAndroid :
    VexStreamPlaybackArchitecture.AndroidContract {

    fun current(
        observation: VexStreamPlaybackObservation,
    ): VexCurrent<out VexStreamPlaybackCase> {
        val queue =
            observation.queue
                ?: return vexCurrent(
                    case =
                        VexStreamPlaybackCase
                            .QueueEmpty,
                    placement =
                        VexStreamPlaybackPlacement
                            .QueueEmpty,
                )

        return if (observation.playRequested) {
            vexCurrent(
                case =
                    VexStreamPlaybackCase
                        .PlayRequested(
                            queue = queue,
                        ),
                placement =
                    VexStreamPlaybackPlacement
                        .PlayRequested,
            )
        } else {
            vexCurrent(
                case =
                    VexStreamPlaybackCase
                        .ReadyToPlay(
                            queue = queue,
                        ),
                placement =
                    VexStreamPlaybackPlacement
                        .ReadyToPlay,
            )
        }
    }

    fun current(
        player: Player,
    ): VexCurrent<out VexStreamPlaybackCase> =
        current(
            observation =
                VexStreamPlaybackObservation(
                    queue =
                        player.mediaItemCount
                            .takeIf {
                                it > 0
                            }
                            ?.let { count ->
                                VexStreamPlaybackQueuePosition(
                                    itemCount = count,
                                    currentIndex =
                                        player.currentMediaItemIndex,
                                    hasPrevious =
                                        player.hasPreviousMediaItem(),
                                    hasNext =
                                        player.hasNextMediaItem(),
                                )
                            },
                    playRequested =
                        player.playWhenReady,
                ),
        )

    fun request(
        control: VexControl,
        controller: MediaController,
    ): Boolean {
        val transition =
            current(
                controller,
            ).availableTransition(
                control,
            )
                ?: return false

        return realize(
            controller = controller,
            transition = transition,
        )
    }

    /**
     * Human question: "How do I play this song?"
     *
     * Resolve the PlayTrack Control against exact current playback truth before replacing the queue.
     */
    fun playTrack(
        controller: MediaController,
        selected: VexStreamTrackState,
        contextTracks: List<VexStreamTrackState>,
        resolver: VexStreamPlaybackSourceResolver,
    ): Boolean {
        current(
            controller,
        ).availableTransition(
            VexStreamPlaybackControl.PlayTrack,
        )
            ?: return false

        return controller.replaceVexStreamContextAndPlay(
            selected = selected,
            contextTracks = contextTracks,
            resolver = resolver,
        )
    }

    private fun realize(
        controller: MediaController,
        transition: VexTransition<*, *>,
    ): Boolean =
        when (transition) {
            VexStreamPlaybackControl.Play -> {
                controller.play()
                true
            }

            VexStreamPlaybackControl.Pause -> {
                controller.pause()
                true
            }

            VexStreamPlaybackControl.NextWhileReady,
            VexStreamPlaybackControl.NextWhilePlayRequested -> {
                controller.seekToNextMediaItem()
                true
            }

            VexStreamPlaybackControl.PreviousWhileReady,
            VexStreamPlaybackControl.PreviousWhilePlayRequested -> {
                controller.seekToPreviousMediaItem()
                true
            }

            else ->
                false
        }
}

// [VXG RealForever]
