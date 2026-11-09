package com.vextreme.vexlife.android.vexstream.player

import com.vextreme.vexlife.android.behavior.VexPartner
import com.vextreme.vexlife.android.vexstream.collection.VexStreamCollectionPlaybackRealization
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackEffectConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackProgressConnection
import com.vextreme.vexlife.android.vexstream.playback.VexStreamPlaybackSeekConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueEffectConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueuePlayConnection
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueReorderConnection
import com.vextreme.vexlife.android.vexstream.repeat.VexStreamRepeatEffectConnection
import com.vextreme.vexlife.android.vexstream.track.VexStreamTrackPlaybackRealization
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
         * Request-capable side of one live Player Connection.
         *
         * Existing feature-owned Effect/Seek vocabularies remain authoritative; this role only says
         * one Player Connection can realize those already-resolved requests.
         */
        interface Request :
            Connection,
            VexStreamPlaybackEffectConnection,
            VexStreamQueueEffectConnection,
            VexStreamRepeatEffectConnection,
            VexStreamQueuePlayConnection,
            VexStreamQueueReorderConnection,
            VexStreamTrackPlaybackRealization,
            VexStreamCollectionPlaybackRealization,
            VexStreamPlaybackSeekConnection

        /**
         * Bounded live progress read from one Player Connection.
         *
         * Progress is deliberately separate from Request because reading provider time is not an
         * outbound Effect.
         */
        interface Progress :
            Connection,
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
