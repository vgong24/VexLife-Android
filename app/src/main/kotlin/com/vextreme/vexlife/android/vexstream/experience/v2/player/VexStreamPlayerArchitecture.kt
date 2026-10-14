package com.vextreme.vexlife.android.vexstream.experience.v2.player

import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureContract
import com.vextreme.vexlife.android.vexarchitecture.mediaplayer.VexMediaPlayerArchitecture
import com.vextreme.vexlife.android.vexstream.experience.v2.player.playback.VexStreamPlayback
import com.vextreme.vexlife.android.vexstream.experience.v2.player.queue.VexStreamQueue

/**
 * VexStream's architecture for the familiar human idea of a media player.
 *
 * This is the one Architecture level inside VexStreamExperience. Playback and Queue are capabilities
 * of this Architecture rather than more classes named "...Architecture", so the nesting level remains
 * obvious to a new developer.
 */
object VexStreamPlayerArchitecture : VexMediaPlayerArchitecture.Contract {
    /** Stable architecture identity inside the VexStream Experience. */
    override val architectureRef: String = "vexstream.player"

    /**
     * The Experience is deliberately not another Architecture, so Player is a top-level architecture
     * root rather than pretending Experience is an architecture parent.
     */
    override val parent: VexArchitectureContract? = null

    /** Version routing lives outside this object; the Player Architecture itself remains enabled. */
    override val enabled: Boolean = true

    /** Playback capability: what is current media being asked to do? */
    val playback: VexStreamPlayback
        get() = VexStreamPlayback

    /** Queue capability: what media surrounds the current item and where are we in that order? */
    val queue: VexStreamQueue
        get() = VexStreamQueue

    /**
     * Architecture children are the capability realms a generic VexArchitecture map can descend into.
     *
     * Their class names omit "Architecture" because, to the human reader, they are capabilities of
     * this one Player Architecture.
     */
    override val children: List<VexArchitectureContract>
        get() = listOf(playback, queue)
}

// [VXG RealForever]
