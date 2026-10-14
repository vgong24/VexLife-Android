package com.vextreme.vexlife.android.vexstream.architecture.v2.player.playback

import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureContract
import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureMap
import com.vextreme.vexlife.android.vexarchitecture.VexCase
import com.vextreme.vexlife.android.vexarchitecture.VexControl
import com.vextreme.vexlife.android.vexarchitecture.VexCurrent
import com.vextreme.vexlife.android.vexarchitecture.VexPlacementView
import com.vextreme.vexlife.android.vexarchitecture.VexRealm
import com.vextreme.vexlife.android.vexarchitecture.VexRealmArchitecture
import com.vextreme.vexlife.android.vexarchitecture.VexTransition
import com.vextreme.vexlife.android.vexarchitecture.architectureMap
import com.vextreme.vexlife.android.vexarchitecture.control
import com.vextreme.vexlife.android.vexarchitecture.placement
import com.vextreme.vexlife.android.vexarchitecture.transition
import com.vextreme.vexlife.android.vexarchitecture.vexCurrent
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.VexStreamPlayer
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.VexStreamPlayerArchitecture
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.VexStreamPlayerPlaybackSnapshot

/**
 * Compile-time coordinate for Playback truth.
 *
 * The marker has no product behavior. Its only job is to make Queue Cases/Transitions impossible to
 * substitute into Playback relationships by accident.
 */
sealed interface VexStreamPlaybackRealm :
    VexRealm

/**
 * The three truths Playback itself needs.
 *
 * Queue position is deliberately absent. Playback answers "is there current media and is playback
 * requested?" while Queue independently answers "where am I in the listening order?".
 */
sealed interface VexStreamPlaybackCase :
    VexCase<VexStreamPlaybackRealm>,
    VexStreamPlaybackArchitecture.CaseContract {

    /**
     * No current media exists.
     *
     * "NoMedia" is chosen instead of "Stopped" because an empty player and a stopped current item are
     * different truths; V2 should not claim more than the player observation proves.
     */
    data object NoMedia :
        VexStreamPlaybackCase

    /**
     * Current media exists but the real player is not requesting playback.
     *
     * "PlaybackNotRequested" mirrors the stable meaning of Media3 playWhenReady=false without
     * overclaiming "Paused", which could have several causes.
     */
    data object PlaybackNotRequested :
        VexStreamPlaybackCase

    /**
     * Current media exists and the real player is requesting playback.
     *
     * "PlaybackRequested" is chosen over "Playing" because buffering/preparing can temporarily mean
     * audio is not sounding while the playback request remains true.
     */
    data object PlaybackRequested :
        VexStreamPlaybackCase
}

/**
 * Playback concern root.
 *
 * A developer who knows "play/pause behavior is wrong" should be able to enter here before knowing
 * any Screen, MediaController helper, service, or ExoPlayer class.
 */
object VexStreamPlaybackArchitecture :
    VexRealmArchitecture<VexStreamPlaybackRealm> {

    /** Absolute coordinate preserves the human directory VexStream -> Music -> Player -> Playback. */
    override val architectureRef: String =
        "vexstream.music.player.playback"

    /** Playback is one independent concern beneath the Player composition. */
    override val parent: VexArchitectureContract =
        VexStreamPlayerArchitecture

    /** V2 route selection is external to the realm; the realm itself remains admitted/enabled. */
    override val enabled: Boolean =
        true

    /** Local contract makes IDE implementation search return only Playback Cases. */
    interface CaseContract

    /** Local contract makes IDE implementation search return only Playback Controls. */
    interface ControlContract

    /** Local contract makes IDE implementation search return only Playback Placements. */
    interface PlacementContract

    /** Local contract marks the Android translation/realization boundary for Playback only. */
    interface AndroidContract

    /** Immediate request vocabulary exposed by the root. */
    val controls: VexStreamPlaybackControl
        get() =
            VexStreamPlaybackControl

    /** Immediate local relationship vocabulary exposed by the root. */
    val placements: VexStreamPlaybackPlacement
        get() =
            VexStreamPlaybackPlacement

    /** Android translator/realizer exposed after semantic meaning is understood. */
    val android: VexStreamPlaybackAndroid
        get() =
            VexStreamPlaybackAndroid

    /**
     * Derived whole-realm orientation.
     *
     * The map validates local nodes but does not own playback behavior.
     */
    val map: VexArchitectureMap<VexStreamPlaybackRealm> by lazy {
        architectureMap(
            placements =
                placements.all,
            controls =
                controls.all,
        )
    }
}

/**
 * Playback request vocabulary.
 *
 * Transition values are intentionally named by their human consequence rather than Media3 method
 * names. The platform mapping is kept in VexStreamPlaybackAndroid.
 */
object VexStreamPlaybackControl :
    VexStreamPlaybackArchitecture.ControlContract {

    /**
     * Request playback from a current-media/not-requested Case.
     *
     * Relative name "play" becomes the stable ref vexstream.music.player.playback.play.
     */
    val Play: VexTransition<
        VexStreamPlaybackRealm,
        VexStreamPlaybackCase.PlaybackNotRequested,
        VexStreamPlaybackCase.PlaybackRequested
    > =
        VexStreamPlaybackArchitecture.transition(
            name = "play",
        )

    /**
     * Clear the playback request while retaining current media.
     *
     * "Pause" remains the human Control/relationship word even though the Case truth is phrased more
     * precisely as PlaybackNotRequested.
     */
    val Pause: VexTransition<
        VexStreamPlaybackRealm,
        VexStreamPlaybackCase.PlaybackRequested,
        VexStreamPlaybackCase.PlaybackNotRequested
    > =
        VexStreamPlaybackArchitecture.transition(
            name = "pause",
        )

    /**
     * One human Play/Pause affordance that resolves to Play OR Pause from the exact current Case.
     */
    val PlayPause: VexControl<VexStreamPlaybackRealm> =
        VexStreamPlaybackArchitecture.control(
            name = "play-pause",
            Play,
            Pause,
        )

    /** One list used only to derive/validate the realm map. */
    val all: List<VexControl<VexStreamPlaybackRealm>> =
        listOf(
            PlayPause,
        )
}

/**
 * Local arrival/exit descriptions for Playback Cases.
 */
object VexStreamPlaybackPlacement :
    VexStreamPlaybackArchitecture.PlacementContract {

    /**
     * NoMedia has no Play/Pause doorway because selecting media belongs to Queue/Music Path concerns.
     */
    val NoMedia =
        VexStreamPlaybackArchitecture.placement<
            VexStreamPlaybackRealm,
            VexStreamPlaybackCase.NoMedia
        > {
        }

    /** PlaybackNotRequested can arrive by Pause and leave through Play. */
    val PlaybackNotRequested =
        VexStreamPlaybackArchitecture.placement<
            VexStreamPlaybackRealm,
            VexStreamPlaybackCase.PlaybackNotRequested
        > {
            arrivedBy(
                VexStreamPlaybackControl.Pause,
            )
            can(
                VexStreamPlaybackControl.Play,
            )
        }

    /** PlaybackRequested can arrive by Play and leave through Pause. */
    val PlaybackRequested =
        VexStreamPlaybackArchitecture.placement<
            VexStreamPlaybackRealm,
            VexStreamPlaybackCase.PlaybackRequested
        > {
            arrivedBy(
                VexStreamPlaybackControl.Play,
            )
            can(
                VexStreamPlaybackControl.Pause,
            )
        }

    /** Map input is derived from the three local neighborhoods, not another Case registry. */
    val all: List<VexPlacementView<VexStreamPlaybackRealm>> =
        listOf(
            NoMedia,
            PlaybackNotRequested,
            PlaybackRequested,
        )
}

/**
 * Android translation + realization for Playback.
 *
 * The name "Android" is retained because Media3 semantics are platform realization, not portable
 * Playback vocabulary. VexArchitecture ends at [VexStreamPlayer]; that port then reaches Media3.
 */
object VexStreamPlaybackAndroid :
    VexStreamPlaybackArchitecture.AndroidContract {

    /**
     * Translate one real-player snapshot into V2 Playback truth.
     */
    fun current(
        snapshot: VexStreamPlayerPlaybackSnapshot,
    ): VexCurrent<
        VexStreamPlaybackRealm,
        out VexStreamPlaybackCase
    > =
        when {
            !snapshot.hasMedia ->
                vexCurrent(
                    case =
                        VexStreamPlaybackCase.NoMedia,
                    placement =
                        VexStreamPlaybackPlacement.NoMedia,
                )

            snapshot.playbackRequested ->
                vexCurrent(
                    case =
                        VexStreamPlaybackCase.PlaybackRequested,
                    placement =
                        VexStreamPlaybackPlacement.PlaybackRequested,
                )

            else ->
                vexCurrent(
                    case =
                        VexStreamPlaybackCase.PlaybackNotRequested,
                    placement =
                        VexStreamPlaybackPlacement.PlaybackNotRequested,
                )
        }

    /** Re-read actual player truth at request time rather than trusting a stale UI projection. */
    fun current(
        player: VexStreamPlayer,
    ): VexCurrent<
        VexStreamPlaybackRealm,
        out VexStreamPlaybackCase
    > =
        current(
            player.observePlayback(),
        )

    /**
     * Resolve semantic Control from exact current truth, then ask the real player to realize it.
     *
     * Returning false means the Control is not currently lawful/recognized; no platform effect was
     * performed.
     */
    fun request(
        control: VexControl<VexStreamPlaybackRealm>,
        player: VexStreamPlayer,
    ): Boolean =
        when (
            current(
                player,
            ).availableTransition(
                control,
            )
        ) {
            VexStreamPlaybackControl.Play -> {
                player.play()
                true
            }

            VexStreamPlaybackControl.Pause -> {
                player.pause()
                true
            }

            else ->
                false
        }
}

// [VXG RealForever]
