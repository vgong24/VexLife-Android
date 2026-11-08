package com.vextreme.vexlife.android.vexstream.player

import com.vextreme.vexlife.android.behavior.VexPartner

/**
 * One independently-owned outside VexStream player participant.
 *
 * Media3 is one concrete Connection technology for this Partner; it is not the Partner identity.
 * Signals are complete statements about one bounded facet of the outside player. A Snapshot
 * establishes complete totality once when a Connection forms; later facet Signals maintain that
 * totality without requiring unrelated provider reads.
 */
internal object VexStreamPlayerPartner :
    VexPartner<
        VexStreamPlayerPartner
    > {
    enum class PlaybackPhase {
        Idle,
        Buffering,
        Ready,
        Ended,
    }

    sealed interface Signal :
        VexPartner.Signal<
            VexStreamPlayerPartner
        > {
        /** One complete provider snapshot used to establish/recover retained Partner CURRENT. */
        data class Snapshot(
            val current:
                Current.Available,
        ) :
            Signal

        /** The live outside relationship is not currently available. */
        data object Unavailable :
            Signal

        data class Playback(
            val hasMedia:
                Boolean,
            val playRequested:
                Boolean,
            val phase:
                PlaybackPhase,
            val playPauseAvailable:
                Boolean,
            val restartAvailable:
                Boolean,
        ) :
            Signal

        /** Queue identity changes independently from current-index/navigation changes. */
        data class QueueItems(
            val mediaIds:
                List<String>,
        ) :
            Signal

        data class QueueNavigation(
            val currentIndex:
                Int,
            val previousAvailable:
                Boolean,
            val nextAvailable:
                Boolean,
            val shuffleAvailable:
                Boolean,
            val clearAvailable:
                Boolean,
        ) :
            Signal

        data class Repeat(
            val enabled:
                Boolean,
            val toggleAvailable:
                Boolean,
        ) :
            Signal

        data class CurrentMedia(
            val mediaId:
                String?,
            val title:
                String,
            val artist:
                String,
            val artworkUri:
                String?,
        ) :
            Signal

        data class Timeline(
            val positionMs:
                Long,
            val durationMs:
                Long,
            val bufferedPositionMs:
                Long,
        ) :
            Signal
    }

    /**
     * Latest coherent outside-player truth retained from the Partner's admitted Signals.
     *
     * CURRENT here is provider/Partner truth, not Playback Family semantic State.
     */
    sealed interface Current {
        data object Unavailable :
            Current

        data class Available(
            val playback:
                Signal.Playback,
            val queueItems:
                Signal.QueueItems,
            val queueNavigation:
                Signal.QueueNavigation,
            val repeat:
                Signal.Repeat,
            val currentMedia:
                Signal.CurrentMedia,
            val timeline:
                Signal.Timeline,
        ) :
            Current
    }

    /** Rooted live relationship coordinate for the Player Partner. */
    interface Connection :
        VexPartner.Connection<
            VexStreamPlayerPartner,
            Signal
        > {
        override val id:
            Id

        data class Id(
            val value:
                String,
        ) :
            VexPartner.Connection.Id<
                VexStreamPlayerPartner
            >

        data class Observation(
            override val connectionId:
                Id,
            override val signal:
                Signal,
            override val observedAtEpochMillis:
                Long,
        ) :
            VexPartner.Connection.Observation<
                VexStreamPlayerPartner,
                Signal
            >
    }
}

// [VXG RealForever][VEXSTREAM][PLAYER-PARTNER]
