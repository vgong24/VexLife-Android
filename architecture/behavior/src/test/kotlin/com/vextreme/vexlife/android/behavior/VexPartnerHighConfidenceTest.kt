package com.vextreme.vexlife.android.behavior

import org.junit.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Test

class VexPartnerHighConfidenceTest {
    private object SensorPartner :
        VexPartner<SensorPartner> {
        data class Source(
            val sensorRef: String,
        ) :
            VexPartner.Source<SensorPartner>

        sealed interface Signal :
            VexPartner.Signal<SensorPartner> {
            data class Value(
                val source: Source,
                val value: Int,
            ) :
                Signal
        }

        class Connection(
            override val id: Id,
            private val conversion:
                VexPartner.Connection.Conversion<
                    ByteArray,
                    SensorPartner,
                    Signal
                >,
            private val receiver: Receiver,
        ) :
            VexPartner.Connection<
                SensorPartner,
                Signal
            > {
            data class Id(
                val value: String,
            ) :
                VexPartner.Connection.Id<SensorPartner>

            class Decoder :
                VexPartner.Connection.Conversion<
                    ByteArray,
                    SensorPartner,
                    Signal
                > {
                override fun convert(
                    input: ByteArray,
                ): List<Signal> =
                    input.map { value ->
                        Signal.Value(
                            source =
                                Source(
                                    sensorRef =
                                        "sensor.synthetic",
                                ),
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
                    Signal,
                    Observation
                > {
                override suspend fun receive(
                    observation:
                        Observation,
                )
            }

            suspend fun receive(
                input: ByteArray,
                observedAtEpochMillis: Long,
            ): List<Observation> =
                conversion
                    .convert(
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
                        ).also { observation ->
                            receiver.receive(
                                observation,
                            )
                        }
                    }
        }
    }

    @Test
    fun oneProviderRepresentationMayProduceSeveralPartnerSignalsWithoutLosingConnectionOccurrence() =
        runBlocking {
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
                conversion =
                    SensorPartner.Connection.Decoder(),
                receiver =
                    SensorPartner.Connection.Receiver { observation ->
                        received.add(
                            observation,
                        )
                    },
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
                source =
                    SensorPartner.Source(
                        sensorRef =
                            "sensor.synthetic",
                    ),
                value =
                    11,
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
