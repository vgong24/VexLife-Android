package com.vextreme.vexlife.android.behavior

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VexPartnerHighConfidenceTest {
    private object SensorPartner :
        VexPartner<SensorPartner> {
        sealed interface Signal :
            VexPartner.Signal<SensorPartner> {
            data class Value(
                val value: Int,
            ) :
                Signal
        }

        class Connection(
            private val id: Id,
            private val decoder: Decoder,
            private val receiver: Receiver,
        ) :
            VexPartner.Connection<
                SensorPartner,
                Signal
            > {
            data class Id(
                override val value: String,
            ) :
                VexPartner.Connection.Id<SensorPartner>

            class Decoder :
                VexPartner.Connection.Decoder<
                    ByteArray,
                    SensorPartner,
                    Signal
                > {
                override fun decode(
                    input: ByteArray,
                ): List<Signal> =
                    input.map { value ->
                        Signal.Value(
                            value =
                                value.toInt(),
                        )
                    }
            }

            data class Observation(
                override val connectionId: Id,
                override val signal: Signal,
                override val observedAtEpochMillis: Long,
            ) :
                VexPartner.Connection.Observation<
                    SensorPartner,
                    Signal
                >

            fun interface Receiver :
                VexPartner.Connection.Receiver<
                    SensorPartner,
                    Signal
                > {
                override fun receive(
                    observation:
                        VexPartner.Connection.Observation<
                            SensorPartner,
                            Signal
                        >,
                )
            }

            fun receive(
                input: ByteArray,
                observedAtEpochMillis: Long,
            ): List<Observation> =
                decoder
                    .decode(
                        input,
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

    @Test
    fun oneProviderRepresentationMayProduceSeveralPartnerSignalsWithoutLosingConnectionOccurrence() {
        val received =
            mutableListOf<
                VexPartner.Connection.Observation<
                    SensorPartner,
                    SensorPartner.Signal
                >
            >()
        val id =
            SensorPartner.Connection.Id(
                value =
                    "sensor.synthetic",
            )
        val connection =
            SensorPartner.Connection(
                id =
                    id,
                decoder =
                    SensorPartner.Connection.Decoder(),
                receiver =
                    SensorPartner.Connection.Receiver(
                        received::add,
                    ),
            )

        val observations =
            connection.receive(
                input =
                    byteArrayOf(
                        11,
                        22,
                    ),
                observedAtEpochMillis =
                    1234L,
            )

        assertEquals(
            2,
            observations.size,
        )
        assertEquals(
            2,
            received.size,
        )
        assertSame(
            observations[0],
            received[0],
        )
        assertEquals(
            id,
            observations[0].connectionId,
        )
        assertEquals(
            SensorPartner.Signal.Value(
                value = 11,
            ),
            observations[0].signal,
        )
        assertEquals(
            1234L,
            observations[0].observedAtEpochMillis,
        )
    }
}

// [VXG RealForever][VEX-PARTNER][PATIENT-ZERO]
