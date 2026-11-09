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

    /**
     * Vex-owned description of one playable item offered to the outside Player.
     *
     * Media3 MediaItem is a concrete Connection representation of this value.
     */
    data class Media(
        val mediaId:
            String,
        val sourceRef:
            String,
        val mimeType:
            String,
        val title:
            String,
        val artist:
            String,
        val album:
            String?,
        val artworkRef:
            String?,
        val playbackStartMs:
            Long,
        val playbackEndMs:
            Long?,
    ) {
        init {
            require(
                mediaId.isNotBlank(),
            )
            require(
                sourceRef.isNotBlank(),
            )
            require(
                mimeType.isNotBlank(),
            )
            require(
                playbackStartMs >=
                    0L,
            )
            require(
                playbackEndMs ==
                    null ||
                    playbackEndMs >
                    playbackStartMs,
            )
        }
    }

    /**
     * One exact occurrence in the externally-owned ordered Player queue.
     *
     * occurrence distinguishes duplicate media IDs inside one observed queue; index binds that
     * occurrence to its current external position. Either coordinate may become stale after mutation.
     */
    data class QueueOccurrence(
        val mediaId:
            String,
        val occurrence:
            Int,
        val index:
            Int,
    ) {
        init {
            require(
                mediaId.isNotBlank(),
            )
            require(
                occurrence >=
                    1,
            )
            require(
                index >=
                    0,
            )
        }
    }

    enum class QueuePlacement {
        Next,
        End,
    }

    enum class QueueMove {
        Earlier,
        Later,
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
            val seekAvailable:
                Boolean =
                false,
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
            val playOccurrenceAvailable:
                Boolean =
                false,
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
         * Request-capable side of one live Connection to the external Player Source.
         *
         * This vocabulary belongs to the Partner relationship itself. Feature Families translate
         * their accepted Effects into these requests through bound Act.Methods; the Partner does not
         * inherit feature-owned Effect/Connection interfaces.
         */
        interface Request :
            Connection {
            val mediaPlaybackAvailable:
                Boolean

            suspend fun playMedia(
                items:
                    List<
                        Media
                    >,
                selectedIndex:
                    Int =
                    0,
            ):
                Boolean

            fun mediaQueueAvailable(
                placement:
                    QueuePlacement,
            ):
                Boolean

            suspend fun queueMedia(
                media:
                    Media,
                placement:
                    QueuePlacement,
            ):
                Boolean

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

            fun playQueueOccurrence(
                occurrence:
                    QueueOccurrence,
            ):
                Boolean

            fun moveQueueOccurrence(
                occurrence:
                    QueueOccurrence,
                move:
                    QueueMove,
            ):
                Boolean

            fun seek(
                mediaId:
                    String,
                mediaItemIndex:
                    Int,
                positionMs:
                    Long,
            ):
                Boolean
        }

        /** Bounded live progress read from one Connection to the same external Player Source. */
        interface Progress :
            Connection {
            data class Snapshot(
                val positionMs:
                    Long =
                    0L,
                val durationMs:
                    Long =
                    0L,
                val bufferedPositionMs:
                    Long =
                    0L,
            ) {
                companion object {
                    val Empty =
                        Snapshot()
                }
            }

            val isPlaying:
                Boolean

            fun readProgress():
                Snapshot
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
