package com.vextreme.vexlife.android.insulin

import com.vextreme.vexlife.android.behavior.VexPartner

/**
 * Bidirectional Partner pressure.
 *
 * Pump is both:
 * - source of reported delivery / reservoir Signals; and
 * - target of an attempted Family Effect through a bound Family Act.Method.
 *
 * Provider acknowledgement remains deliberately separate from later physical-delivery evidence.
 */
internal object InsulinPumpPartner :
    VexPartner<InsulinPumpPartner> {

    data class Source(
        val pumpRef: String,
    )

    sealed interface Signal :
        VexPartner.Signal<InsulinPumpPartner> {

        data class DeliveryObserved(
            val source: Source,
            val deliveryRef: String,
            val requestRef: String?,
            val occurredAtEpochMillis: Long,
            val unitsHundredths: Int,
        ) :
            Signal

        data class ReservoirObserved(
            val source: Source,
            val remainingUnitsHundredths: Int,
        ) :
            Signal
    }

    internal class Connection(
        val id: Id,
        private val decoder: Decoder,
        private val receiver: Receiver,
    ) :
        VexPartner.Connection<
            InsulinPumpPartner,
            Signal
        > {

        data class Id(
            override val value: String,
        ) :
            VexPartner.Connection.Id<InsulinPumpPartner>

        sealed interface Acknowledgement {
            data class Accepted(
                val requestRef: String,
                val unitsHundredths: Int,
            ) :
                Acknowledgement

            data class Rejected(
                val reason: String,
            ) :
                Acknowledgement
        }

        private val requestLock =
            Any()

        private var nextRequest =
            1L

        /**
         * Provider-side request acknowledgement only.
         *
         * ACCEPTED != DELIVERED. This function never writes InsulinStore and never manufactures a
         * DeliveryObserved Signal.
         */
        suspend fun requestDelivery(
            unitsHundredths: Int,
        ):
            Acknowledgement =
            if (
                unitsHundredths <=
                    0
            ) {
                Acknowledgement.Rejected(
                    reason =
                        "synthetic amount must be positive",
                )
            } else {
                val requestRef =
                    synchronized(
                        requestLock,
                    ) {
                        "pump-request-" +
                            nextRequest++
                    }

                Acknowledgement.Accepted(
                    requestRef =
                        requestRef,
                    unitsHundredths =
                        unitsHundredths,
                )
            }

        /**
         * Synthetic inbound grammar:
         *
         *     d|pump|deliveryRef|requestRefOrDash|occurredAtMs|unitsHundredths
         *     r|pump|remainingUnitsHundredths
         */
        class Decoder :
            VexPartner.Connection.Decoder<
                ByteArray,
                InsulinPumpPartner,
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
                                Signal.DeliveryObserved(
                                    source =
                                        Source(
                                            pumpRef =
                                                it[1],
                                        ),
                                    deliveryRef =
                                        it[2],
                                    requestRef =
                                        it[3]
                                            .takeUnless { value ->
                                                value ==
                                                    "-"
                                            },
                                    occurredAtEpochMillis =
                                        it[4]
                                            .toLong(),
                                    unitsHundredths =
                                        it[5]
                                            .toInt(),
                                )
                            }

                    "r" ->
                        parts
                            .takeIf {
                                it.size ==
                                    3
                            }
                            ?.let {
                                Signal.ReservoirObserved(
                                    source =
                                        Source(
                                            pumpRef =
                                                it[1],
                                        ),
                                    remainingUnitsHundredths =
                                        it[2]
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
                InsulinPumpPartner,
                Signal
            >

        class Receiver(
            private val store: InsulinStore,
        ) :
            VexPartner.Connection.Receiver<
                InsulinPumpPartner,
                Signal
            > {
            override fun receive(
                observation:
                    VexPartner.Connection.Observation<
                        InsulinPumpPartner,
                        Signal
                    >,
            ) {
                store.commitPump(
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

// [VXG RealForever][VEX-PARTNER][INSULIN-PUMP-PRESSURE]
