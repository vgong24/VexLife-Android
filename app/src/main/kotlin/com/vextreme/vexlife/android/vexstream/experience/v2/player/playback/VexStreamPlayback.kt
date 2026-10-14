package com.vextreme.vexlife.android.vexstream.experience.v2.player.playback

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
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.VexMediaPlaybackProvider
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.VexMediaPlaybackSnapshot
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.VexMediaPlayerArchitecture
import com.vextreme.vexlife.android.vexarchitecture.placement
import com.vextreme.vexlife.android.vexarchitecture.transition
import com.vextreme.vexlife.android.vexarchitecture.vexCurrent
import com.vextreme.vexlife.android.vexstream.experience.v2.player.VexStreamPlayerArchitecture

/**
 * Compile-time coordinate for VexStream Playback truth.
 *
 * Realm remains infrastructure terminology; normal feature code should mostly read the human words
 * VexStreamPlayback / Case / Control / Placement.
 */
sealed interface VexStreamPlaybackRealm : VexRealm

/** Current truths of the Playback capability. */
sealed interface VexStreamPlaybackCase :
    VexCase<VexStreamPlaybackRealm>,
    VexStreamPlayback.CaseContract {

    /**
     * The provider has no current media.
     *
     * "NoMedia" avoids incorrectly calling an empty player "Stopped".
     */
    data object NoMedia : VexStreamPlaybackCase

    /**
     * Media exists but playback is not requested.
     *
     * This is more truthful than "Paused" because the observation may represent several platform
     * situations while preserving the same user-request meaning.
     */
    data object PlaybackNotRequested : VexStreamPlaybackCase

    /**
     * Media exists and playback is requested.
     *
     * This is more truthful than "Playing" during buffering/preparing.
     */
    data object PlaybackRequested : VexStreamPlaybackCase
}

/**
 * Playback capability inside [VexStreamPlayerArchitecture].
 *
 * The class deliberately omits "Architecture": a fresh reader should understand that Playback is one
 * capability of the Player Architecture, while the generic VexRealmArchitecture contract still gives
 * the foundation typed Case/Transition behavior.
 */
object VexStreamPlayback :
    VexRealmArchitecture<VexStreamPlaybackRealm>,
    VexMediaPlayerArchitecture.PlaybackContract {

    /** Absolute identity beneath the one Player Architecture. */
    override val architectureRef: String = "vexstream.player.playback"

    /** Generic architecture parent used for bounded map/navigation tooling. */
    override val parent: VexArchitectureContract = VexStreamPlayerArchitecture

    /** The media-player architecture whose Playback capability this is. */
    override val playerArchitecture: VexMediaPlayerArchitecture.Contract =
        VexStreamPlayerArchitecture

    /** Debug version selection is external; this admitted capability itself remains enabled. */
    override val enabled: Boolean = true

    /** Local IDE-discovery contract for Playback Cases only. */
    interface CaseContract

    /** Local IDE-discovery contract for Playback Controls only. */
    interface ControlContract

    /** Local IDE-discovery contract for Playback Placements only. */
    interface PlacementContract

    /** Local IDE-discovery contract for Android/provider translation only. */
    interface AndroidContract

    /** Human request vocabulary for this capability. */
    val controls: VexStreamPlaybackControl
        get() = VexStreamPlaybackControl

    /** Local relationship neighborhoods for this capability. */
    val placements: VexStreamPlaybackPlacement
        get() = VexStreamPlaybackPlacement

    /** Translation/realization boundary from semantic meaning to a media provider. */
    val android: VexStreamPlaybackAndroid
        get() = VexStreamPlaybackAndroid

    /** Derived orientation/validation; never a second effect owner. */
    val map: VexArchitectureMap<VexStreamPlaybackRealm> by lazy {
        architectureMap(
            placements = placements.all,
            controls = controls.all,
        )
    }
}

/** Request and relationship vocabulary of Playback. */
object VexStreamPlaybackControl : VexStreamPlayback.ControlContract {
    /** PlaybackNotRequested -> PlaybackRequested. */
    val Play: VexTransition<
        VexStreamPlaybackRealm,
        VexStreamPlaybackCase.PlaybackNotRequested,
        VexStreamPlaybackCase.PlaybackRequested
    > =
        VexStreamPlayback.transition(
            name = "play",
        )

    /** PlaybackRequested -> PlaybackNotRequested. */
    val Pause: VexTransition<
        VexStreamPlaybackRealm,
        VexStreamPlaybackCase.PlaybackRequested,
        VexStreamPlaybackCase.PlaybackNotRequested
    > =
        VexStreamPlayback.transition(
            name = "pause",
        )

    /** One human Play/Pause request that resolves to the lawful relationship for the current Case. */
    val PlayPause: VexControl<VexStreamPlaybackRealm> =
        VexStreamPlayback.control(
            name = "play-pause",
            Play,
            Pause,
        )

    /** Controls admitted into the derived capability map. */
    val all: List<VexControl<VexStreamPlaybackRealm>> =
        listOf(
            PlayPause,
        )
}

/** Local neighborhoods around each Playback Case. */
object VexStreamPlaybackPlacement : VexStreamPlayback.PlacementContract {
    /** No media means Play/Pause has no lawful exit; media selection belongs outside Playback. */
    val NoMedia =
        VexStreamPlayback.placement<
            VexStreamPlaybackRealm,
            VexStreamPlaybackCase.NoMedia
        > {
        }

    /** Pause arrives here; Play can leave here. */
    val PlaybackNotRequested =
        VexStreamPlayback.placement<
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

    /** Play arrives here; Pause can leave here. */
    val PlaybackRequested =
        VexStreamPlayback.placement<
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

    /** Complete Placement set used only to derive the map. */
    val all: List<VexPlacementView<VexStreamPlaybackRealm>> =
        listOf(
            NoMedia,
            PlaybackNotRequested,
            PlaybackRequested,
        )
}

/**
 * Translation between Playback meaning and a runtime provider.
 *
 * "Android" is retained as the feature-layer role because the current consumer is Android, while the
 * provider contracts themselves remain reusable enough for Media3 or a partner SDK adapter.
 */
object VexStreamPlaybackAndroid : VexStreamPlayback.AndroidContract {
    /** Translate one provider observation into the current semantic Playback Case. */
    fun current(
        snapshot: VexMediaPlaybackSnapshot,
    ): VexCurrent<
        VexStreamPlaybackRealm,
        out VexStreamPlaybackCase
    > =
        when {
            !snapshot.hasMedia ->
                vexCurrent(
                    case = VexStreamPlaybackCase.NoMedia,
                    placement = VexStreamPlaybackPlacement.NoMedia,
                )

            snapshot.playbackRequested ->
                vexCurrent(
                    case = VexStreamPlaybackCase.PlaybackRequested,
                    placement = VexStreamPlaybackPlacement.PlaybackRequested,
                )

            else ->
                vexCurrent(
                    case = VexStreamPlaybackCase.PlaybackNotRequested,
                    placement = VexStreamPlaybackPlacement.PlaybackNotRequested,
                )
        }

    /** Re-read the real provider immediately before resolving a Control. */
    fun current(
        provider: VexMediaPlaybackProvider,
    ): VexCurrent<
        VexStreamPlaybackRealm,
        out VexStreamPlaybackCase
    > =
        current(
            provider.observePlayback(),
        )

    /**
     * Resolve semantic Play/Pause first, then cross the provider boundary only for the selected
     * relationship.
     */
    fun request(
        control: VexControl<VexStreamPlaybackRealm>,
        provider: VexMediaPlaybackProvider,
    ): Boolean =
        when (
            current(
                provider,
            ).availableTransition(
                control,
            )
        ) {
            VexStreamPlaybackControl.Play -> {
                provider.play()
                true
            }

            VexStreamPlaybackControl.Pause -> {
                provider.pause()
                true
            }

            else -> false
        }
}

// [VXG RealForever]
