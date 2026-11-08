package com.vextreme.vexlife.android.vexstream.player

import com.vextreme.vexlife.android.behavior.VexPartner
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackEffectConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackProgressConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackSeekConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueEffectConnection
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatEffectConnection
import java.util.concurrent.atomic.AtomicLong

/**
 * One independently-owned outside VexStream player participant.
 *
 * Media3 is one concrete Connection technology for this Partner; it is not the Partner identity.
 * Signals are complete statements about one bounded facet of the outside player. Retained totality
 * belongs to the Player Store, not the Partner root itself.
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
    }

    data class QueueCurrent(
        val items:
            Signal.QueueItems,
        val navigation:
            Signal.QueueNavigation,
    )

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
        ) :
            Current {
            val queue:
                QueueCurrent
                get() =
                    QueueCurrent(
                        items =
                            queueItems,
                        navigation =
                            queueNavigation,
                    )
        }
    }

    interface Connection :
        VexPartner.Connection<
            VexStreamPlayerPartner,
            Signal
        > {
        override val id:
            Id

        /**
         * Exact process-local Connection occurrence.
         *
         * occurrence is concrete-realization freshness, not durable Partner identity.
         */
        data class Id(
            val value:
                String,
            val occurrence:
                Long,
        ) :
            VexPartner.Connection.Id<
                VexStreamPlayerPartner
            > {
            companion object {
                private val nextOccurrence =
                    AtomicLong(
                        1L,
                    )

                fun next(
                    prefix:
                        String,
                ):
                    Id {
                    require(
                        prefix.isNotBlank(),
                    ) {
                        "Player Connection prefix must not be blank"
                    }

                    val occurrence =
                        nextOccurrence
                            .getAndIncrement()

                    return Id(
                        value =
                            prefix +
                                "-" +
                                occurrence,
                        occurrence =
                            occurrence,
                    )
                }
            }
        }

        /**
         * Outbound capability of one live Player Connection.
         *
         * Existing feature-owned Effects/Seek remain their own vocabulary; this child only states
         * that one Player Connection can realize those already-resolved requests.
         */
        interface Outbound :
            Connection,
            VexStreamPlaybackEffectConnection,
            VexStreamQueueEffectConnection,
            VexStreamRepeatEffectConnection,
            VexStreamPlaybackSeekConnection,
            VexStreamPlaybackProgressConnection

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
