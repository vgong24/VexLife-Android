package com.vextreme.vexlife.android.cgm

import com.vextreme.vexlife.android.behavior.VexPartner

/**
 * Patient-zero external CGM participant.
 *
 * This file intentionally keeps the Partner-side tree source-readable rather than scattering
 * Signal / Decoder / Observation / Receiver as package peers.
 *
 * It is NOT a claim that every future Partner must have these exact children. The reusable
 * VexPartner root is being pressured by this CGM example first, then connected-pen / pump examples.
 */
internal object CgmPartner :
    VexPartner<CgmPartner> {

    /**
     * Identity supplied by the external acquisition relationship itself.
     *
     * Device + acquisition epoch stay together so a reused sequence number after sensor/session
     * replacement cannot silently join an older history.
     */
    data class Source(
        val deviceRef: String,
        val acquisitionEpoch: String,
    )

    enum class Trend {
        Falling,
        Steady,
        Rising,
    }

    sealed interface Signal :
        VexPartner.Signal<CgmPartner> {

        data class Glucose(
            val source: Source,
            val sequence: Long,
            val capturedAtEpochMillis: Long,
            val mgDl: Int,
            val reportedTrend: Trend,
        ) :
            Signal

        data class Temperature(
            val source: Source,
            val sequence: Long,
            val capturedAtEpochMillis: Long,
            val celsiusHundredths: Int,
        ) :
            Signal
    }

    /**
     * One live realization of the Partner relationship.
     *
     * The synthetic wire format is intentionally tiny and disposable. BLE/HTTP may later realize
     * this same Partner vocabulary without changing what Signal means.
     */
    internal class Connection(
        override val id: Id,
        private val decoder: Decoder,
        private val receiver: Receiver,
    ) :
        VexPartner.Connection<
            CgmPartner,
            Signal
        > {

        data class Id(
            val value: String,
        ) :
            VexPartner.Connection.Id<CgmPartner>

        /**
         * Synthetic packet grammar:
         *
         *     g|device|epoch|sequence|capturedAtMs|mgDl|trend
         *     t|device|epoch|sequence|capturedAtMs|celsiusHundredths
         *
         * Records in one transport packet are separated by ';'. One packet may therefore yield
         * multiple independent Signals without inventing one giant "measurement" payload.
         */
        class Decoder :
            VexPartner.Connection.Decoder<
                ByteArray,
                CgmPartner,
                Signal
            > {
            override fun decode(
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

                if (parts.isEmpty()) {
                    return null
                }

                return when (parts[0]) {
                    "g" ->
                        parts
                            .takeIf {
                                it.size ==
                                    7
                            }
                            ?.let {
                                Signal.Glucose(
                                    source =
                                        Source(
                                            deviceRef =
                                                it[1],
                                            acquisitionEpoch =
                                                it[2],
                                        ),
                                    sequence =
                                        it[3]
                                            .toLong(),
                                    capturedAtEpochMillis =
                                        it[4]
                                            .toLong(),
                                    mgDl =
                                        it[5]
                                            .toInt(),
                                    reportedTrend =
                                        when (
                                            it[6]
                                                .lowercase()
                                        ) {
                                            "falling" ->
                                                Trend.Falling

                                            "steady" ->
                                                Trend.Steady

                                            "rising" ->
                                                Trend.Rising

                                            else ->
                                                return null
                                        },
                                )
                            }

                    "t" ->
                        parts
                            .takeIf {
                                it.size ==
                                    6
                            }
                            ?.let {
                                Signal.Temperature(
                                    source =
                                        Source(
                                            deviceRef =
                                                it[1],
                                            acquisitionEpoch =
                                                it[2],
                                        ),
                                    sequence =
                                        it[3]
                                            .toLong(),
                                    capturedAtEpochMillis =
                                        it[4]
                                            .toLong(),
                                    celsiusHundredths =
                                        it[5]
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
                CgmPartner,
                Signal
            >

        /**
         * Connection-side handoff into CGM durable admission.
         *
         * The patient zero commits every successfully decoded Signal. Duplicate/conflict/gap rules
         * belong to the next reconciliation pressure, not to Decoder.
         */
        class Receiver(
            private val store: CgmStore,
        ) :
            VexPartner.Connection.Receiver<
                CgmPartner,
                Signal
            > {
            override fun receive(
                observation:
                    VexPartner.Connection.Observation<
                        CgmPartner,
                        Signal
                    >,
            ) {
                store.commit(
                    observation,
                )
            }
        }

        fun receive(
            bytes: ByteArray,
            observedAtEpochMillis: Long,
        ):
            List<Observation> =
            decoder
                .decode(
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
                    ).also(
                        receiver::receive,
                    )
                }
    }
}

// [VXG RealForever][VEX-CGM-LABS][PARTNER-PATIENT-ZERO]
