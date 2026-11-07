package com.vextreme.vexlife.android.insulin

import com.vextreme.vexlife.android.behavior.VexPartner

/**
 * Sparse-event Partner pressure for VexPartner.
 *
 * Unlike CGM, a connected pen does not imply a periodic continuity frontier merely because two dose
 * identities differ. This example therefore reuses the Partner relationship without importing CGM
 * gap semantics.
 */
internal object ConnectedPenPartner :
    VexPartner<ConnectedPenPartner> {

    data class Source(
        val penRef: String,
        val cartridgeRef: String,
    ) :
        VexPartner.Source<ConnectedPenPartner>

    sealed interface Signal :
        VexPartner.Signal<ConnectedPenPartner> {

        data class DoseRecorded(
            val source: Source,
            val doseRef: String,
            val occurredAtEpochMillis: Long,
            val unitsHundredths: Int,
        ) :
            Signal

        data class CartridgeObserved(
            val source: Source,
            val remainingUnitsHundredths: Int,
        ) :
            Signal
    }

    internal class Connection(
        override val id: Id,
        private val conversion:
            VexPartner.Connection.Conversion<
                ByteArray,
                ConnectedPenPartner,
                Signal
            >,
        private val receiver: Receiver,
    ) :
        VexPartner.Connection<
            ConnectedPenPartner,
            Signal
        > {

        data class Id(
            val value: String,
        ) :
            VexPartner.Connection.Id<ConnectedPenPartner>

        /**
         * Synthetic packet grammar:
         *
         *     d|pen|cartridge|doseRef|occurredAtMs|unitsHundredths
         *     c|pen|cartridge|remainingUnitsHundredths
         *
         * ';' separates records so one transport delivery may carry several independent Signals.
         */
        class Decoder :
            VexPartner.Connection.Conversion<
                ByteArray,
                ConnectedPenPartner,
                Signal
            > {
            override fun convert(
                input: ByteArray,
            ): List<Signal> =
                input
                    .toString(
                        Charsets.UTF_8,
                    )
                    .split(';')
                    .mapNotNull(
                        ::decodeRecord,
                    )

            private fun decodeRecord(
                record: String,
            ): Signal? {
                val parts =
                    record
                        .trim()
                        .split('|')

                return when (
                    parts.firstOrNull()
                ) {
                    "d" ->
                        parts
                            .takeIf {
                                it.size ==
                                    6
                            }
                            ?.let {
                                Signal.DoseRecorded(
                                    source =
                                        Source(
                                            penRef =
                                                it[1],
                                            cartridgeRef =
                                                it[2],
                                        ),
                                    doseRef =
                                        it[3],
                                    occurredAtEpochMillis =
                                        it[4]
                                            .toLong(),
                                    unitsHundredths =
                                        it[5]
                                            .toInt(),
                                )
                            }

                    "c" ->
                        parts
                            .takeIf {
                                it.size ==
                                    4
                            }
                            ?.let {
                                Signal.CartridgeObserved(
                                    source =
                                        Source(
                                            penRef =
                                                it[1],
                                            cartridgeRef =
                                                it[2],
                                        ),
                                    remainingUnitsHundredths =
                                        it[3]
                                            .toInt(),
                                )
                            }

                    else ->
                        null
                }
            }
        }

        data class Observation(
            override val connectionId: Id,
            override val signal: Signal,
            override val observedAtEpochMillis: Long,
        ) :
            VexPartner.Connection.Observation<
                ConnectedPenPartner,
                Signal
            >

        class Receiver(
            private val store: InsulinStore,
        ) :
            VexPartner.Connection.Receiver<
                ConnectedPenPartner,
                Signal,
                Observation
            > {
            override suspend fun receive(
                observation:
                    Observation,
            ) {
                store.commitPen(
                    observation,
                )
            }
        }

        suspend fun receive(
            bytes: ByteArray,
            observedAtEpochMillis: Long,
        ):
            List<Observation> =
            conversion
                .convert(
                    bytes,
                )
                .map { signal ->
                    Observation(
                        connectionId =
                            id,
                        signal =
                            signal,
                        observedAtEpochMillis =
                            observedAtEpochMillis,
                    ).also { observation ->
                        receiver.receive(
                            observation,
                        )
                    }
                }
    }
}

// [VXG RealForever][VEX-PARTNER][CONNECTED-PEN-PRESSURE]
