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

/**
 * Naming rationale for playback Cases:
 *
 * Media3 source term: playWhenReady
 * Vex term: PlaybackRequested / PlaybackNotRequested
 *
 * Why:
 * - "Playing" would be false while Media3 is buffering/preparing even though playback is requested.
 * - "Paused" would overclaim why playback is not requested; the user may never have started it, the
 *   item may have ended, or another platform event may have cleared the request.
 * - "ReadyToPlay" sounds like a platform readiness guarantee we do not actually observe.
 *
 * The Vex names therefore preserve only the stable meaning we truly know: whether playback is
 * currently requested while a non-empty queue exists.
 *
 * QueueEmpty stays separate because "no playback" would wrongly merge an empty queue with a
 * non-empty queue whose playback is merely not requested.
 */
sealed interface VexStreamPlaybackCase :
    VexStreamPlaybackArchitecture.CaseContract {

    data object QueueEmpty :
        VexStreamPlaybackCase

    data class PlaybackNotRequested(
        val queue: VexStreamPlaybackQueuePosition,
    ) : VexStreamPlaybackCase

    data class PlaybackRequested(
        val queue: VexStreamPlaybackQueuePosition,
    ) : VexStreamPlaybackCase
}

object VexStreamPlaybackControl :
    VexStreamPlaybackArchitecture.ControlContract {

    val Play =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlaybackNotRequested,
            VexStreamPlaybackCase.PlaybackRequested
        >(
            "vexstream.playback.play",
        )

    val Pause =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlaybackRequested,
            VexStreamPlaybackCase.PlaybackNotRequested
        >(
            "vexstream.playback.pause",
        )

    /**
     * Shared queue-replacement relationship used by Play Track / Play All / Shuffle All.
     *
     * Naming rationale:
     *
     * PlayTrackFrom...                  -> rejected: names one caller, not the shared consequence.
     * ReplaceContextFrom...             -> rejected: "Context" is vague and collides with Android
     *                                      Context terminology.
     * ReplaceQueueAndRequestPlayback... -> chosen: says exactly what all three Controls cause.
     *
     * The human Control is still named PlayTrack / PlayAll / ShuffleAll. The Transition names the
     * common Before -> After consequence beneath those different intentions.
     *
     * The human Control is one intention, while its typed relationship depends on the current Case.
     * All three converge on PlaybackRequested; the exact selected track/queue data is observed from Media3
     * after the Android realization.
     */
    val ReplaceQueueAndRequestPlaybackFromQueueEmpty =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.QueueEmpty,
            VexStreamPlaybackCase.PlaybackRequested
        >(
            "vexstream.playback.replace-queue-and-request-playback.queue-empty",
        )

    val ReplaceContextFromReady =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlaybackNotRequested,
            VexStreamPlaybackCase.PlaybackRequested
        >(
            "vexstream.playback.replace-queue-and-request-playback.not-requested",
        )

    val ReplaceQueueAndRequestPlaybackFromPlaybackRequested =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlaybackRequested,
            VexStreamPlaybackCase.PlaybackRequested
        >(
            "vexstream.playback.replace-queue-and-request-playback.requested",
        )

    val NextWhilePlaybackNotRequested =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlaybackNotRequested,
            VexStreamPlaybackCase.PlaybackNotRequested
        >(
            transitionRef =
                "vexstream.playback.next.not-requested",
            conditionRef =
                "queue-has-next",
        ) { current ->
            current.queue.hasNext
        }

    val NextWhilePlaybackRequested =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlaybackRequested,
            VexStreamPlaybackCase.PlaybackRequested
        >(
            transitionRef =
                "vexstream.playback.next.requested",
            conditionRef =
                "queue-has-next",
        ) { current ->
            current.queue.hasNext
        }

    val PreviousWhilePlaybackNotRequested =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlaybackNotRequested,
            VexStreamPlaybackCase.PlaybackNotRequested
        >(
            transitionRef =
                "vexstream.playback.previous.not-requested",
            conditionRef =
                "queue-has-previous",
        ) { current ->
            current.queue.hasPrevious
        }

    val PreviousWhilePlaybackRequested =
        VexStreamPlaybackArchitecture.transition<
            VexStreamPlaybackCase.PlaybackRequested,
            VexStreamPlaybackCase.PlaybackRequested
        >(
            transitionRef =
                "vexstream.playback.previous.requested",
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
            ReplaceQueueAndRequestPlaybackFromQueueEmpty,
            ReplaceContextFromReady,
            ReplaceQueueAndRequestPlaybackFromPlaybackRequested,
        )

    val PlayAll =
        VexStreamPlaybackArchitecture.control(
            "vexstream.playback.play-all",
            ReplaceQueueAndRequestPlaybackFromQueueEmpty,
            ReplaceContextFromReady,
            ReplaceQueueAndRequestPlaybackFromPlaybackRequested,
        )

    val ShuffleAll =
        VexStreamPlaybackArchitecture.control(
            "vexstream.playback.shuffle-all",
            ReplaceQueueAndRequestPlaybackFromQueueEmpty,
            ReplaceContextFromReady,
            ReplaceQueueAndRequestPlaybackFromPlaybackRequested,
        )

    val Next =
        VexStreamPlaybackArchitecture.control(
            "vexstream.playback.next",
            NextWhilePlaybackNotRequested,
            NextWhilePlaybackRequested,
        )

    val Previous =
        VexStreamPlaybackArchitecture.control(
            "vexstream.playback.previous",
            PreviousWhilePlaybackNotRequested,
            PreviousWhilePlaybackRequested,
        )

    val all: List<VexControl> =
        listOf(
            PlayPause,
            PlayTrack,
            PlayAll,
            ShuffleAll,
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
                VexStreamPlaybackControl.ReplaceQueueAndRequestPlaybackFromQueueEmpty,
            )
        }

    val PlaybackNotRequested =
        VexStreamPlaybackArchitecture.placement<
            VexStreamPlaybackCase.PlaybackNotRequested
        > {
            arrivedBy(
                VexStreamPlaybackControl.Pause,
            )
            can(
                VexStreamPlaybackControl.ReplaceContextFromReady,
            )
            arrivedBy(
                VexStreamPlaybackControl.NextWhilePlaybackNotRequested,
            )
            arrivedBy(
                VexStreamPlaybackControl.PreviousWhilePlaybackNotRequested,
            )
            can(
                VexStreamPlaybackControl.Play,
            )
            can(
                VexStreamPlaybackControl.NextWhilePlaybackNotRequested,
            )
            can(
                VexStreamPlaybackControl.PreviousWhilePlaybackNotRequested,
            )
        }

    val PlaybackRequested =
        VexStreamPlaybackArchitecture.placement<
            VexStreamPlaybackCase.PlaybackRequested
        > {
            arrivedBy(
                VexStreamPlaybackControl.Play,
            )
            arrivedBy(
                VexStreamPlaybackControl.ReplaceQueueAndRequestPlaybackFromQueueEmpty,
            )
            arrivedBy(
                VexStreamPlaybackControl.ReplaceContextFromReady,
            )
            arrivedBy(
                VexStreamPlaybackControl.ReplaceQueueAndRequestPlaybackFromPlaybackRequested,
            )
            can(
                VexStreamPlaybackControl.ReplaceQueueAndRequestPlaybackFromPlaybackRequested,
            )
            arrivedBy(
                VexStreamPlaybackControl.NextWhilePlaybackRequested,
            )
            arrivedBy(
                VexStreamPlaybackControl.PreviousWhilePlaybackRequested,
            )
            can(
                VexStreamPlaybackControl.Pause,
            )
            can(
                VexStreamPlaybackControl.NextWhilePlaybackRequested,
            )
            can(
                VexStreamPlaybackControl.PreviousWhilePlaybackRequested,
            )
        }

    val all: List<VexPlacementView> =
        listOf(
            QueueEmpty,
            PlaybackNotRequested,
            PlaybackRequested,
        )
}

/**
 * Android facts translated into Vex playback vocabulary.
 *
 * Naming rationale:
 * Media3 "playWhenReady" becomes "playbackRequested" here because this object is the translation
 * boundary. The platform name remains in [VexStreamPlaybackAndroid.current(Player)] where it belongs.
 */
data class VexStreamPlaybackObservation(
    val queue: VexStreamPlaybackQueuePosition?,
    val playbackRequested: Boolean,
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

        return if (observation.playbackRequested) {
            vexCurrent(
                case =
                    VexStreamPlaybackCase
                        .PlaybackRequested(
                            queue = queue,
                        ),
                placement =
                    VexStreamPlaybackPlacement
                        .PlaybackRequested,
            )
        } else {
            vexCurrent(
                case =
                    VexStreamPlaybackCase
                        .PlaybackNotRequested(
                            queue = queue,
                        ),
                placement =
                    VexStreamPlaybackPlacement
                        .PlaybackNotRequested,
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
                    playbackRequested =
                        player.playWhenReady,
                ),
        )

    fun isAvailable(
        control: VexControl,
        controller: MediaController,
    ): Boolean =
        current(
            controller,
        ).availableTransition(
            control,
        ) != null

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

        return controller.replaceVexStreamQueueAndPlay(
            selected = selected,
            contextTracks = contextTracks,
            resolver = resolver,
        )
    }

    fun playCollection(
        control: VexControl,
        controller: MediaController,
        tracks: List<VexStreamTrackState>,
        resolver: VexStreamPlaybackSourceResolver,
    ): Boolean {
        val shuffled =
            when (control) {
                VexStreamPlaybackControl.PlayAll ->
                    false

                VexStreamPlaybackControl.ShuffleAll ->
                    true

                else ->
                    return false
            }

        current(
            controller,
        ).availableTransition(
            control,
        )
            ?: return false

        return controller.replaceVexStreamCollectionAndPlay(
            tracks = tracks,
            resolver = resolver,
            shuffled = shuffled,
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

            VexStreamPlaybackControl.NextWhilePlaybackNotRequested,
            VexStreamPlaybackControl.NextWhilePlaybackRequested -> {
                controller.seekToNextMediaItem()
                true
            }

            VexStreamPlaybackControl.PreviousWhilePlaybackNotRequested,
            VexStreamPlaybackControl.PreviousWhilePlaybackRequested -> {
                controller.seekToPreviousMediaItem()
                true
            }

            else ->
                false
        }
}

// [VXG RealForever]
