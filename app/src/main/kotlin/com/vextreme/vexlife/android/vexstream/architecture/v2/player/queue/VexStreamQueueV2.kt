package com.vextreme.vexlife.android.vexstream.architecture.v2.player.queue

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
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.VexStreamPlayerQueueSnapshot

/**
 * Compile-time coordinate for Queue truth.
 *
 * Queue gets its own realm because "what comes before/after the current song?" is orthogonal to
 * "is playback requested?". The generic prevents Queue relationships from becoming Playback edges.
 */
sealed interface VexStreamQueueRealm :
    VexRealm

/**
 * Queue truth intentionally has only Empty and Positioned.
 *
 * Beginning/Middle/End are data facts of Positioned, not separate Case types. This keeps the Case
 * vocabulary about meaningful situations while conditions such as hasNext choose local Controls.
 */
sealed interface VexStreamQueueCase :
    VexCase<VexStreamQueueRealm>,
    VexStreamQueueArchitecture.CaseContract {

    /** No media items exist in the real player queue. */
    data object Empty :
        VexStreamQueueCase

    /**
     * A non-empty queue has one player-reported position.
     *
     * hasPrevious/hasNext are retained from the actual player rather than re-derived from indices so
     * the player keeps authority over traversal semantics.
     */
    data class Positioned(
        /** Number of media items currently held by the real queue owner. */
        val itemCount: Int,
        /** Current real-player index, permitted to be -1 if the player reports no selected item yet. */
        val currentIndex: Int,
        /** Exact player-owned answer for whether Previous is meaningful. */
        val hasPrevious: Boolean,
        /** Exact player-owned answer for whether Next is meaningful. */
        val hasNext: Boolean,
    ) : VexStreamQueueCase {
        init {
            require(
                itemCount > 0,
            ) {
                "Positioned queue must contain at least one item"
            }
            require(
                currentIndex in -1 until itemCount,
            ) {
                "currentIndex must be -1 or an existing queue index"
            }
        }
    }
}

/**
 * Queue concern root.
 *
 * A developer investigating Next/Previous/queue selection can start here without understanding
 * Playback or Media3 first.
 */
object VexStreamQueueArchitecture :
    VexRealmArchitecture<VexStreamQueueRealm> {

    /** Absolute directory coordinate VexStream -> Music -> Player -> Queue. */
    override val architectureRef: String =
        "vexstream.music.player.queue"

    /** Queue is the sibling of Playback beneath Player, not a field inside Playback. */
    override val parent: VexArchitectureContract =
        VexStreamPlayerArchitecture

    /** Debug route selection is external; the admitted V2 Queue realm itself remains enabled. */
    override val enabled: Boolean =
        true

    /** Local IDE contract for Queue Cases only. */
    interface CaseContract

    /** Local IDE contract for Queue Controls only. */
    interface ControlContract

    /** Local IDE contract for Queue Placements only. */
    interface PlacementContract

    /** Local IDE contract for Queue's Android/player realization only. */
    interface AndroidContract

    /** Request vocabulary exposed by the root. */
    val controls: VexStreamQueueControl
        get() =
            VexStreamQueueControl

    /** Local relational neighborhoods exposed by the root. */
    val placements: VexStreamQueuePlacement
        get() =
            VexStreamQueuePlacement

    /** Player/Android translation exposed after semantic meaning is understood. */
    val android: VexStreamQueueAndroid
        get() =
            VexStreamQueueAndroid

    /** Derived map used only for validation/orientation. */
    val map: VexArchitectureMap<VexStreamQueueRealm> by lazy {
        architectureMap(
            placements =
                placements.all,
            controls =
                controls.all,
        )
    }
}

/**
 * Queue request vocabulary.
 *
 * Next and Previous are self-transitions of Positioned because the queue remains non-empty while its
 * current data changes. A Case does not need a new type merely because an index changed.
 */
object VexStreamQueueControl :
    VexStreamQueueArchitecture.ControlContract {

    /**
     * Move forward only when the real player says a next item exists.
     */
    val Next: VexTransition<
        VexStreamQueueRealm,
        VexStreamQueueCase.Positioned,
        VexStreamQueueCase.Positioned
    > =
        VexStreamQueueArchitecture.transition(
            name = "next",
            conditionName =
                "has-next",
        ) { current ->
            current.hasNext
        }

    /**
     * Move backward only when the real player says a previous item exists.
     */
    val Previous: VexTransition<
        VexStreamQueueRealm,
        VexStreamQueueCase.Positioned,
        VexStreamQueueCase.Positioned
    > =
        VexStreamQueueArchitecture.transition(
            name = "previous",
            conditionName =
                "has-previous",
        ) { current ->
            current.hasPrevious
        }

    /**
     * Select one existing queue item and request it to play.
     *
     * The index itself remains request data at the Player boundary; the architectural relationship
     * only states that selecting within a non-empty queue is meaningful.
     */
    val PlaySelected: VexTransition<
        VexStreamQueueRealm,
        VexStreamQueueCase.Positioned,
        VexStreamQueueCase.Positioned
    > =
        VexStreamQueueArchitecture.transition(
            name = "play-selected",
            conditionName =
                "has-items",
        ) { current ->
            current.itemCount > 0
        }

    /** Human Next Control resolves only to the conditional Next relationship. */
    val NextControl: VexControl<VexStreamQueueRealm> =
        VexStreamQueueArchitecture.control(
            name = "next",
            Next,
        )

    /** Human Previous Control resolves only to the conditional Previous relationship. */
    val PreviousControl: VexControl<VexStreamQueueRealm> =
        VexStreamQueueArchitecture.control(
            name = "previous",
            Previous,
        )

    /** Human queue-row selection resolves to PlaySelected; selected index stays request data. */
    val PlaySelectedControl: VexControl<VexStreamQueueRealm> =
        VexStreamQueueArchitecture.control(
            name = "play-selected",
            PlaySelected,
        )

    /** Map input containing only request surfaces this V2 slice actually implements. */
    val all: List<VexControl<VexStreamQueueRealm>> =
        listOf(
            NextControl,
            PreviousControl,
            PlaySelectedControl,
        )
}

/**
 * Local Queue relationships.
 */
object VexStreamQueuePlacement :
    VexStreamQueueArchitecture.PlacementContract {

    /**
     * Empty has no Next/Previous/PlaySelected exits.
     *
     * Adding/selecting media is deliberately a separate future Queue/Music Path question.
     */
    val Empty =
        VexStreamQueueArchitecture.placement<
            VexStreamQueueRealm,
            VexStreamQueueCase.Empty
        > {
        }

    /**
     * Positioned retains the same Case type across navigation/selection while its data changes.
     *
     * Self-transition arrival declarations are intentional: the same semantic relationship that
     * leaves Positioned also lands in a newly observed Positioned truth.
     */
    val Positioned =
        VexStreamQueueArchitecture.placement<
            VexStreamQueueRealm,
            VexStreamQueueCase.Positioned
        > {
            arrivedBy(
                VexStreamQueueControl.Next,
            )
            arrivedBy(
                VexStreamQueueControl.Previous,
            )
            arrivedBy(
                VexStreamQueueControl.PlaySelected,
            )

            can(
                VexStreamQueueControl.Next,
            )
            can(
                VexStreamQueueControl.Previous,
            )
            can(
                VexStreamQueueControl.PlaySelected,
            )
        }

    /** Map input derived from the two local Queue neighborhoods. */
    val all: List<VexPlacementView<VexStreamQueueRealm>> =
        listOf(
            Empty,
            Positioned,
        )
}

/**
 * Android/player translation + realization for Queue.
 *
 * Queue does not inspect Playback. Next therefore stays one relationship regardless of whether
 * playback is currently requested.
 */
object VexStreamQueueAndroid :
    VexStreamQueueArchitecture.AndroidContract {

    /** Translate one actual-player queue snapshot into V2 Queue truth. */
    fun current(
        snapshot: VexStreamPlayerQueueSnapshot,
    ): VexCurrent<
        VexStreamQueueRealm,
        out VexStreamQueueCase
    > =
        if (snapshot.itemCount <= 0) {
            vexCurrent(
                case =
                    VexStreamQueueCase.Empty,
                placement =
                    VexStreamQueuePlacement.Empty,
            )
        } else {
            vexCurrent(
                case =
                    VexStreamQueueCase.Positioned(
                        itemCount =
                            snapshot.itemCount,
                        currentIndex =
                            snapshot.currentIndex,
                        hasPrevious =
                            snapshot.hasPrevious,
                        hasNext =
                            snapshot.hasNext,
                    ),
                placement =
                    VexStreamQueuePlacement.Positioned,
            )
        }

    /** Re-read the real player immediately before Control resolution. */
    fun current(
        player: VexStreamPlayer,
    ): VexCurrent<
        VexStreamQueueRealm,
        out VexStreamQueueCase
    > =
        current(
            player.observeQueue(),
        )

    /**
     * Resolve one Queue Control against current truth, then ask the real Player to realize it.
     *
     * index is used only by PlaySelected and remains ordinary request data rather than a new Case.
     */
    fun request(
        control: VexControl<VexStreamQueueRealm>,
        player: VexStreamPlayer,
        index: Int? = null,
    ): Boolean {
        return when (
            current(
                player,
            ).availableTransition(
                control,
            )
        ) {
            VexStreamQueueControl.Next -> {
                player.next()
                true
            }

            VexStreamQueueControl.Previous -> {
                player.previous()
                true
            }

            VexStreamQueueControl.PlaySelected -> {
                val selectedIndex =
                    index ?: return false

                player.playQueueIndex(
                    selectedIndex,
                )
                true
            }

            else ->
                false
        }
    }
}

// [VXG RealForever]
