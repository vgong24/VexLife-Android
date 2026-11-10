package com.vextreme.vexlife.android.vexstream.player

import com.vextreme.vexlife.android.behavior.VexPartner
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

    /**
     * One external VexStream music/playback origin.
     *
     * Media3 Player, MediaSession and MediaController are realization/access mechanisms around this
     * same Source; recreating a live Connection does not manufacture another music origin.
     */
    data object Source :
        VexPartner.Source<
            VexStreamPlayerPartner
        >

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
        val source:
            Source
            get() =
                Source

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
            val items:
                List<Item>,
        ) :
            Signal {
            data class Item(
                val mediaId:
                    String,
                val title:
                    String,
                val artist:
                    String,
            )

            val mediaIds:
                List<String> =
                items.map(
                    Item::mediaId,
                )
        }

        data class QueueNavigation(
            val currentIndex:
                Int,
            val previousAvailable:
                Boolean,
            val nextAvailable:
                Boolean,
            val changeMediaItemsAvailable:
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

    interface Connection :
        VexPartner.Connection<
            VexStreamPlayerPartner,
            Signal
        > {
        val source:
            Source
            get() =
                Source

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
         * Finite request side of one live outside Player Connection.
         *
         * Track / Collection submission, Seek, Queue-row Play and Reorder keep their existing
         * payload/currentness boundaries. This Partner request vocabulary is intentionally limited
         * to the finite Playback / Queue / Repeat Effects owned by this migration.
         */
        interface Request :
            Connection {
            suspend fun play()

            suspend fun pause()

            suspend fun restartCurrent()

            suspend fun previous()

            suspend fun next()

            suspend fun shuffleFuture()

            suspend fun clearQueue()

            suspend fun setRepeatEnabled(
                enabled:
                    Boolean,
            )
        }

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
