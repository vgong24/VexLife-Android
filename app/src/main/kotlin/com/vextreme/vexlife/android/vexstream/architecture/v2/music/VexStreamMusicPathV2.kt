package com.vextreme.vexlife.android.vexstream.architecture.v2.music

import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.vexarchitecture.VexArchitectureContract
import com.vextreme.vexlife.android.vexstream.architecture.v2.VexStreamMusicArchitecture
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.playback.VexStreamPlaybackArchitecture
import com.vextreme.vexlife.android.vexstream.architecture.v2.player.queue.VexStreamQueueArchitecture

/**
 * Feature-local cross-concern human intention.
 *
 * "Path" was chosen instead of Process/Workflow/Command because this object answers "what semantic
 * concerns does this human intention walk through?". It does not own execution, retries, lifetime,
 * serialization or current truth.
 *
 * Path deliberately stays VexStream-local in V2. The reusable VexArchitecture module should promote
 * the word only after another feature proves the same need.
 */
sealed interface VexStreamMusicPath {

    /** Stable absolute identity beneath the Music architecture root. */
    val pathRef: String

    /** Ordinary human question that caused this Path to exist. */
    val humanQuestion: String

    /** Concern roots whose truths/effects this intention is expected to touch. */
    val concerns: List<VexArchitectureContract>

    /**
     * "Play this song" means more than Playback.Play.
     *
     * It chooses/replaces Queue context AND requests Playback, which is why V2 keeps it above both
     * child realms instead of forcing it into one of them.
     */
    data class PlaySong(
        /** Song the human selected. */
        val selectedTrackRef: VexStreamTrackRef,
        /**
         * Ordered library context that should become the listening queue around the selected song.
         *
         * Track refs are used rather than MediaItems because media/source realization belongs below
         * the semantic Path at the actual player/media boundary.
         */
        val contextTrackRefs: List<VexStreamTrackRef>,
    ) : VexStreamMusicPath {
        init {
            require(
                selectedTrackRef in contextTrackRefs,
            ) {
                "PlaySong context must contain the selected track"
            }
        }

        override val pathRef: String =
            VexStreamMusicArchitecture.architectureRef +
                ".path.play-song"

        override val humanQuestion: String =
            "How do I play this song?"

        override val concerns: List<VexArchitectureContract> =
            listOf(
                VexStreamQueueArchitecture,
                VexStreamPlaybackArchitecture,
            )
    }

    /**
     * "Play all" replaces the listening order with one collection and requests playback.
     */
    data class PlayAll(
        /** Ordered music selection to become the new queue. */
        val trackRefs: List<VexStreamTrackRef>,
    ) : VexStreamMusicPath {
        init {
            require(
                trackRefs.isNotEmpty(),
            ) {
                "PlayAll requires at least one track"
            }
        }

        override val pathRef: String =
            VexStreamMusicArchitecture.architectureRef +
                ".path.play-all"

        override val humanQuestion: String =
            "How do I play all of this music?"

        override val concerns: List<VexArchitectureContract> =
            listOf(
                VexStreamQueueArchitecture,
                VexStreamPlaybackArchitecture,
            )
    }

    /**
     * "Shuffle all" shares PlayAll's concerns but changes how the Queue order is formed.
     *
     * The distinct Path preserves the human intention instead of reducing it to a boolean argument
     * such as playAll(shuffled = true) at the semantic layer.
     */
    data class ShuffleAll(
        /** Music selection whose order will be randomized at the realization boundary. */
        val trackRefs: List<VexStreamTrackRef>,
    ) : VexStreamMusicPath {
        init {
            require(
                trackRefs.isNotEmpty(),
            ) {
                "ShuffleAll requires at least one track"
            }
        }

        override val pathRef: String =
            VexStreamMusicArchitecture.architectureRef +
                ".path.shuffle-all"

        override val humanQuestion: String =
            "How do I shuffle all of this music?"

        override val concerns: List<VexArchitectureContract> =
            listOf(
                VexStreamQueueArchitecture,
                VexStreamPlaybackArchitecture,
            )
    }
}

// [VXG RealForever]
