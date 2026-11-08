package com.vextreme.vexlife.android.data.cgm

import com.vextreme.vexlife.android.cgm.CgmPartner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class CgmObservationEntityTest {
    @Test
    fun glucoseObservationRoundTripsAsHistoricalValueWithoutDomainEmbedding() {
        val observation =
            CgmPartner.Connection.Observation(
                connectionId =
                    CgmPartner.Connection.Id(
                        value =
                            "connection-a",
                    ),
                signal =
                    CgmPartner.Signal.Glucose(
                        source =
                            CgmPartner.Source(
                                deviceRef =
                                    "device-a",
                                acquisitionEpoch =
                                    "epoch-a",
                            ),
                        sequence =
                            17L,
                        capturedAtEpochMillis =
                            1_700L,
                        mgDl =
                            118,
                        reportedTrend =
                            CgmPartner.Trend.Steady,
                    ),
                observedAtEpochMillis =
                    1_750L,
            )

        val entity =
            observation
                .toEntity()

        assertEquals(
            "GLUCOSE",
            entity.signalKind,
        )
        assertEquals(
            "connection-a",
            entity.connectionRef,
        )
        assertEquals(
            118,
            entity.glucoseMgDl,
        )
        assertEquals(
            null,
            entity.temperatureCelsiusHundredths,
        )

        val restored =
            entity
                .copy(
                    databaseId =
                        41L,
                )
                .toObservation()

        assertEquals(
            observation,
            restored,
        )
        assertNotSame(
            observation,
            restored,
        )
        assertNotSame(
            observation.signal,
            restored.signal,
        )
    }

    @Test
    fun temperatureObservationRoundTripsWithoutInventingGlucoseFields() {
        val observation =
            CgmPartner.Connection.Observation(
                connectionId =
                    CgmPartner.Connection.Id(
                        value =
                            "connection-b",
                    ),
                signal =
                    CgmPartner.Signal.Temperature(
                        source =
                            CgmPartner.Source(
                                deviceRef =
                                    "device-b",
                                acquisitionEpoch =
                                    "epoch-b",
                            ),
                        sequence =
                            22L,
                        capturedAtEpochMillis =
                            2_200L,
                        celsiusHundredths =
                            3_175,
                    ),
                observedAtEpochMillis =
                    2_250L,
            )

        val entity =
            observation
                .toEntity()

        assertEquals(
            "TEMPERATURE",
            entity.signalKind,
        )
        assertEquals(
            3_175,
            entity.temperatureCelsiusHundredths,
        )
        assertEquals(
            null,
            entity.glucoseMgDl,
        )
        assertEquals(
            observation,
            entity.toObservation(),
        )
    }
}

// [VXG RealForever][VEX-CGM-LABS][ROOM-ENTITY-PROOF]
