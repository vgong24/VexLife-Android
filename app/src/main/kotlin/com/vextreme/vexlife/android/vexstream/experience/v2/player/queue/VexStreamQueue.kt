package com.vextreme.vexlife.android.vexstream.experience.v2.player.queue

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
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.VexMediaPlayerArchitecture
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.queue.VexMediaQueueContract
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.queue.VexMediaQueueProvider
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.queue.VexMediaQueueStatus
import com.vextreme.vexlife.android.vexarchitecture.placement
import com.vextreme.vexlife.android.vexarchitecture.transition
import com.vextreme.vexlife.android.vexarchitecture.vexCurrent
import com.vextreme.vexlife.android.vexstream.experience.v2.player.VexStreamPlayerArchitecture

/** Compile-time coordinate for VexStream Queue truth. */
sealed interface VexStreamQueueRealm : VexRealm

/** Current truths of the Queue capability. */
sealed interface VexStreamQueueCase :
    VexCase<VexStreamQueueRealm>,
    VexStreamQueue.CaseContract {

    /** No media items currently exist in the provider queue. */
    data object Empty : VexStreamQueueCase

    /**
     * A non-empty queue with one provider-reported current position.
     *
     * Start/Middle/End remain facts rather than separate Case classes, preventing a combinatorial
     * Case family.
     */
    data class Positioned(
        /** Number of provider-owned items. */
        val itemCount: Int,
        /** Current index, or -1 when the provider reports no selected item. */
        val currentIndex: Int,
        /** Whether Previous is meaningful according to the provider. */
        val hasPrevious: Boolean,
        /** Whether Next is meaningful according to the provider. */
        val hasNext: Boolean,
    ) : VexStreamQueueCase {
        init {
            require(itemCount > 0)
            require(currentIndex in -1 until itemCount)
        }
    }
}

/**
 * Queue capability inside [VexStreamPlayerArchitecture].
 *
 * Queue is a sibling capability of Playback, not a child state inside it.
 */
object VexStreamQueue :
    VexRealmArchitecture<VexStreamQueueRealm>,
    VexMediaQueueContract {

    /** Absolute identity beneath the one Player Architecture. */
    override val architectureRef: String = "vexstream.player.queue"

    /** Generic parent used for architecture navigation/validation. */
    override val parent: VexArchitectureContract = VexStreamPlayerArchitecture

    /** Player architecture whose optional Queue capability this is. */
    override val playerArchitecture: VexMediaPlayerArchitecture =
        VexStreamPlayerArchitecture

    /** Version routing is external; this capability itself remains enabled. */
    override val enabled: Boolean = true

    /** Local IDE-discovery contract for Queue Cases only. */
    interface CaseContract

    /** Local IDE-discovery contract for Queue Controls only. */
    interface ControlContract

    /** Local IDE-discovery contract for Queue Placements only. */
    interface PlacementContract

    /** Local IDE-discovery contract for Android/provider translation only. */
    interface AndroidContract

    /** Human request vocabulary for Queue. */
    val controls: VexStreamQueueControl
        get() = VexStreamQueueControl

    /** Local relationship neighborhoods for Queue. */
    val placements: VexStreamQueuePlacement
        get() = VexStreamQueuePlacement

    /** Translation/realization boundary from Queue meaning to a provider. */
    val android: VexStreamQueueAndroid
        get() = VexStreamQueueAndroid

    /** Derived orientation/validation only. */
    val map: VexArchitectureMap<VexStreamQueueRealm> by lazy {
        architectureMap(
            placements = placements.all,
            controls = controls.all,
        )
    }
}

/** Request/relationship vocabulary for Queue. */
object VexStreamQueueControl : VexStreamQueue.ControlContract {
    /** Move forward only when the provider observation says a next item exists. */
    val Next: VexTransition<
        VexStreamQueueRealm,
        VexStreamQueueCase.Positioned,
        VexStreamQueueCase.Positioned
    > =
        VexStreamQueue.transition(
            name = "next",
            conditionName = "has-next",
        ) { current ->
            current.hasNext
        }

    /** Move backward only when the provider observation says a previous item exists. */
    val Previous: VexTransition<
        VexStreamQueueRealm,
        VexStreamQueueCase.Positioned,
        VexStreamQueueCase.Positioned
    > =
        VexStreamQueue.transition(
            name = "previous",
            conditionName = "has-previous",
        ) { current ->
            current.hasPrevious
        }

    /**
     * Select an existing queue row and request playback.
     *
     * The index remains request data instead of becoming another Case dimension.
     */
    val PlaySelected: VexTransition<
        VexStreamQueueRealm,
        VexStreamQueueCase.Positioned,
        VexStreamQueueCase.Positioned
    > =
        VexStreamQueue.transition(
            name = "play-selected",
            conditionName = "has-items",
        ) { current ->
            current.itemCount > 0
        }

    /** Human Next request. */
    val NextControl: VexControl<VexStreamQueueRealm> =
        VexStreamQueue.control(
            name = "next",
            Next,
        )

    /** Human Previous request. */
    val PreviousControl: VexControl<VexStreamQueueRealm> =
        VexStreamQueue.control(
            name = "previous",
            Previous,
        )

    /** Human queue-row selection request. */
    val PlaySelectedControl: VexControl<VexStreamQueueRealm> =
        VexStreamQueue.control(
            name = "play-selected",
            PlaySelected,
        )

    /** Complete Control set currently admitted in the comparison slice. */
    val all: List<VexControl<VexStreamQueueRealm>> =
        listOf(
            NextControl,
            PreviousControl,
            PlaySelectedControl,
        )
}

/** Local neighborhoods around Queue Cases. */
object VexStreamQueuePlacement : VexStreamQueue.PlacementContract {
    /** Empty has no Next/Previous/selection doorway. */
    val Empty =
        VexStreamQueue.placement<
            VexStreamQueueRealm,
            VexStreamQueueCase.Empty
        > {
        }

    /** Queue navigation/selection changes Positioned data while remaining the same Case kind. */
    val Positioned =
        VexStreamQueue.placement<
            VexStreamQueueRealm,
            VexStreamQueueCase.Positioned
        > {
            arrivedBy(VexStreamQueueControl.Next)
            arrivedBy(VexStreamQueueControl.Previous)
            arrivedBy(VexStreamQueueControl.PlaySelected)
            can(VexStreamQueueControl.Next)
            can(VexStreamQueueControl.Previous)
            can(VexStreamQueueControl.PlaySelected)
        }

    /** Complete Placement set used to derive the Queue map. */
    val all: List<VexPlacementView<VexStreamQueueRealm>> =
        listOf(
            Empty,
            Positioned,
        )
}

/** Translation between Queue meaning and a runtime provider. */
object VexStreamQueueAndroid : VexStreamQueue.AndroidContract {
    /** Translate the provider's current Queue status into semantic Queue truth. */
    fun current(
        status: VexMediaQueueStatus,
    ): VexCurrent<
        VexStreamQueueRealm,
        out VexStreamQueueCase
    > =
        if (status.itemCount <= 0) {
            vexCurrent(
                case = VexStreamQueueCase.Empty,
                placement = VexStreamQueuePlacement.Empty,
            )
        } else {
            vexCurrent(
                case =
                    VexStreamQueueCase.Positioned(
                        itemCount = status.itemCount,
                        currentIndex = status.currentIndex,
                        hasPrevious = status.hasPrevious,
                        hasNext = status.hasNext,
                    ),
                placement = VexStreamQueuePlacement.Positioned,
            )
        }

    /** Re-read real Queue truth immediately before Control resolution. */
    fun current(
        provider: VexMediaQueueProvider,
    ): VexCurrent<
        VexStreamQueueRealm,
        out VexStreamQueueCase
    > =
        current(
            provider.queueStatus(),
        )

    /**
     * Resolve Queue meaning before crossing the provider boundary.
     *
     * Selected index remains ordinary request data but is checked against the exact current queue
     * observation before the provider is called.
     */
    fun request(
        control: VexControl<VexStreamQueueRealm>,
        provider: VexMediaQueueProvider,
        index: Int? = null,
    ): Boolean {
        val current =
            current(
                provider,
            )
        val transition =
            current.availableTransition(
                control,
            )

        return when (transition) {
            VexStreamQueueControl.Next -> {
                provider.next()
                true
            }

            VexStreamQueueControl.Previous -> {
                provider.previous()
                true
            }

            VexStreamQueueControl.PlaySelected -> {
                val selectedIndex =
                    index ?: return false
                val positioned =
                    current.case as?
                        VexStreamQueueCase.Positioned
                        ?: return false

                if (selectedIndex !in 0 until positioned.itemCount) {
                    return false
                }

                provider.playQueueIndex(
                    selectedIndex,
                )
                true
            }

            else -> false
        }
    }
}

// [VXG RealForever]
