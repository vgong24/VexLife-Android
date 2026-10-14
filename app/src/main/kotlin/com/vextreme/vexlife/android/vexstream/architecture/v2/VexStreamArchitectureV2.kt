package com.vextreme.vexlife.android.vexstream.architecture.v2

import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureContract
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.VexStreamPlayerArchitecture
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.playback.VexStreamPlaybackArchitecture
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.queue.VexStreamQueueArchitecture

/**
 * Top semantic directory for the VexStream product.
 *
 * The name stays simply "VexStream" because this node answers "which VexStream domain am I entering?"
 * rather than describing Android transport, media playback, or one screen.
 */
object VexStreamArchitecture :
    VexArchitectureContract {

    /** Durable product root identity; descendants add their own concern segment. */
    override val architectureRef: String =
        "vexstream"

    /** VexStream is the root of this directory, so it has no semantic parent here. */
    override val parent:
        VexArchitectureContract? =
        null

    /**
     * V2 is an experimental route selector, not a feature flag for whether VexStream exists.
     * Keep the architecture itself enabled; Debug Menu decides whether Legacy or V2 handles a request.
     */
    override val enabled: Boolean =
        true

    /** Music is the first domain under VexStream and the only V2 domain admitted in this practicum. */
    val music: VexArchitectureContract
        get() =
            VexStreamMusicArchitecture

    /** Children are the code-native "what can I investigate next?" index. */
    override val children: List<VexArchitectureContract>
        get() =
            listOf(
                music,
            )
}

/**
 * Music groups the human concerns discovered by asking "what must a streaming music app have?"
 *
 * Music intentionally owns no MusicCase. Library, Player, Availability and Connection are different
 * truths; creating one combination Case here would rebuild the exact mental state explosion V2 is
 * trying to avoid.
 */
object VexStreamMusicArchitecture :
    VexArchitectureContract {

    /** Stable domain identity beneath VexStream. */
    override val architectureRef: String =
        "vexstream.music"

    /** Parent makes the absolute walk VexStream -> Music discoverable in both directions. */
    override val parent: VexArchitectureContract =
        VexStreamArchitecture

    /** Music participates whenever its parent VexStream realm participates. */
    override val enabled: Boolean =
        true

    /**
     * Library answers "what music exists?".
     *
     * It is an orientation node only in this V2 slice; no LibraryCase is invented before a real
     * relationship needs one.
     */
    val library: VexArchitectureContract
        get() =
            VexStreamLibraryArchitecture

    /**
     * Player answers "what am I listening to and what happens around it?".
     *
     * Player is a composition root whose children Playback and Queue keep orthogonal truths separate.
     */
    val player: VexArchitectureContract
        get() =
            VexStreamPlayerArchitecture

    /**
     * Availability answers "can this music actually be obtained here?".
     *
     * Streaming/Offline are intentionally future child concerns rather than fields forced into Player.
     */
    val availability: VexArchitectureContract
        get() =
            VexStreamAvailabilityArchitecture

    /**
     * Connection answers "can I reach my VexStream music source?".
     *
     * It remains separate from playback so losing the computer does not erase verified local music.
     */
    val connection: VexArchitectureContract
        get() =
            VexStreamConnectionArchitecture

    /** Human-level directory for the current admitted Music concerns. */
    override val children: List<VexArchitectureContract>
        get() =
            listOf(
                library,
                player,
                availability,
                connection,
            )
}

/**
 * Orientation-only Library node.
 *
 * The absence of Case/Control/Placement is intentional evidence that fractal vocabulary does not
 * require identical machinery at every level.
 */
object VexStreamLibraryArchitecture :
    VexArchitectureContract {

    /** "library" is the human noun for the place where music can be browsed/found. */
    override val architectureRef: String =
        "vexstream.music.library"

    /** Library is one concern inside Music. */
    override val parent: VexArchitectureContract =
        VexStreamMusicArchitecture

    /** No separate Library feature flag has been earned yet. */
    override val enabled: Boolean =
        true
}

/**
 * Orientation-only Availability node.
 *
 * The node exists because Local/Hub/Unavailable truth is independent from playback truth; deeper
 * Offline/Streaming realms should be added only when their relationships are implemented.
 */
object VexStreamAvailabilityArchitecture :
    VexArchitectureContract {

    /** "availability" names the human question "can I use this music here?" rather than one transport. */
    override val architectureRef: String =
        "vexstream.music.availability"

    /** Availability is one Music concern, not a Player child. */
    override val parent: VexArchitectureContract =
        VexStreamMusicArchitecture

    /** V2 does not change product availability policy in this experiment. */
    override val enabled: Boolean =
        true
}

/**
 * Orientation-only Connection node.
 *
 * Connection is separated because "can I reach the source?" is a different truth from "can I play a
 * verified local copy?" and should not be folded into one giant Player case.
 */
object VexStreamConnectionArchitecture :
    VexArchitectureContract {

    /** "connection" is the ordinary human noun already used by the current VexStream UI. */
    override val architectureRef: String =
        "vexstream.music.connection"

    /** Connection is one Music concern. */
    override val parent: VexArchitectureContract =
        VexStreamMusicArchitecture

    /** Existing VexStream connection behavior remains canonical while this V2 slice focuses on Player. */
    override val enabled: Boolean =
        true
}

/**
 * Absolute root-to-leaf example used by tests/debugging to prove the semantic directory is walkable.
 */
val VexStreamArchitecture.playerWalk: List<VexArchitectureContract>
    get() =
        listOf(
            this,
            music,
            VexStreamPlayerArchitecture,
            VexStreamPlaybackArchitecture,
            VexStreamQueueArchitecture,
        )

// [VXG RealForever]
