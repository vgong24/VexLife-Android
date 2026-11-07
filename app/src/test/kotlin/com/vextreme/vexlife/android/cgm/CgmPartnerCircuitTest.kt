package com.vextreme.vexlife.android.cgm

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CgmPartnerCircuitTest {
    @Test
    fun bleAndHttpConversionsProduceTheSamePartnerSignalsForEquivalentFacts() {
        val lab =
            CgmPartnerLab()

        try {
            val (
                ble,
                http,
            ) =
                lab.equivalentSyntheticConversions(
                    sequence =
                        42L,
                    capturedAtEpochMillis =
                        4_200L,
                    mgDl =
                        137,
                    trend =
                        "rising",
                    celsiusHundredths =
                        3_222,
                )

            assertEquals(
                ble,
                http,
            )
            assertEquals(
                listOf(
                    CgmPartner.Signal.Glucose(
                        source =
                            CgmPartner.Source(
                                deviceRef =
                                    "cgm-synthetic",
                                acquisitionEpoch =
                                    "epoch-a",
                            ),
                        sequence =
                            42L,
                        capturedAtEpochMillis =
                            4_200L,
                        mgDl =
                            137,
                        reportedTrend =
                            CgmPartner.Trend.Rising,
                    ),
                    CgmPartner.Signal.Temperature(
                        source =
                            CgmPartner.Source(
                                deviceRef =
                                    "cgm-synthetic",
                                acquisitionEpoch =
                                    "epoch-a",
                            ),
                        sequence =
                            42L,
                        capturedAtEpochMillis =
                            4_200L,
                        celsiusHundredths =
                            3_222,
                    ),
                ),
                ble,
            )
        } finally {
            lab.close()
        }
    }

    @Test
    fun bleAndHttpConnectionsShareStoreAndFamilyWhileKeepingOccurrenceProvenance() =
        runBlocking {
            val lab =
                CgmPartnerLab(
                    nowEpochMillis = {
                        8_000L
                    },
                )

            try {
                lab.receiveSyntheticBlePacket()
                    .join()
                lab.receiveSyntheticHttpResponse()
                    .join()

                val snapshot =
                    lab.store
                        .readCurrent()

                assertEquals(
                    4,
                    snapshot
                        .observations
                        .size,
                )
                assertEquals(
                    listOf(
                        "cgm.synthetic.ble",
                        "cgm.synthetic.ble",
                        "cgm.synthetic.http",
                        "cgm.synthetic.http",
                    ),
                    snapshot
                        .observations
                        .map {
                            it.connectionId.value
                        },
                )

                val latest =
                    withTimeout(
                        2_000L,
                    ) {
                        lab.family
                            .states
                            .first { state ->
                                state is
                                    CgmFamily.State.LatestAdmitted &&
                                    state.glucose.sequence ==
                                    2L
                            }
                    } as
                        CgmFamily.State.LatestAdmitted

                assertEquals(
                    2L,
                    latest.glucose.sequence,
                )
                assertEquals(
                    4,
                    latest.storedObservationCount,
                )
            } finally {
                lab.close()
            }
        }

    @Test
    fun oneSyntheticPacketDecodesQueuesStoresAndReachesFamilyProjection() =
        runBlocking {
            val lab =
                CgmPartnerLab(
                    nowEpochMillis = {
                        1_000L
                    },
                )

            try {
                val receipt =
                    lab.receiveSyntheticPacket()

                receipt.join()

                val snapshot =
                    lab.store
                        .readCurrent()
                val observations =
                    snapshot
                        .observations

                assertEquals(
                    2,
                    observations.size,
                )

                val glucose =
                    observations
                        .map {
                            it.signal
                        }
                        .filterIsInstance<
                            CgmPartner.Signal.Glucose
                        >()
                        .single()
                val temperature =
                    observations
                        .map {
                            it.signal
                        }
                        .filterIsInstance<
                            CgmPartner.Signal.Temperature
                        >()
                        .single()

                assertEquals(
                    103,
                    glucose.mgDl,
                )
                assertEquals(
                    3_101,
                    temperature.celsiusHundredths,
                )

                val state =
                    withTimeout(
                        2_000L,
                    ) {
                        lab.family
                            .states
                            .first { state ->
                                state is
                                    CgmFamily.State.LatestAdmitted &&
                                    state.glucose.sequence ==
                                    1L
                            }
                    } as
                        CgmFamily.State.LatestAdmitted

                /*
                 * In-memory Store keeps the exact admitted Partner statement recognizable.
                 */
                assertSame(
                    glucose,
                    state.glucose,
                )
                assertEquals(
                    2,
                    state.storedObservationCount,
                )

                val display =
                    lab.family
                        .displayState as
                        CgmFamily.DisplayState.Reading

                assertEquals(
                    103,
                    display.mgDl,
                )
                assertEquals(
                    CgmPartner.Trend.Steady,
                    display.trend,
                )
                assertEquals(
                    "cgm-synthetic",
                    display.sourceRef,
                )
                assertEquals(
                    1L,
                    display.sequence,
                )
            } finally {
                lab.close()
            }
        }

    @Test
    fun queuedInboundAdmissionsPreserveConnectionOrderBeforeFamilyInterpretsLatestTruth() =
        runBlocking {
            val lab =
                CgmPartnerLab(
                    nowEpochMillis = {
                        2_000L
                    },
                )

            try {
                val first =
                    lab.receiveSyntheticPacket()
                val second =
                    lab.receiveSyntheticPacket()

                first.join()
                second.join()

                val snapshot =
                    lab.store
                        .readCurrent()

                assertEquals(
                    4,
                    snapshot
                        .observations
                        .size,
                )
                assertEquals(
                    listOf(
                        1L,
                        1L,
                        2L,
                        2L,
                    ),
                    snapshot
                        .observations
                        .map { observation ->
                            when (
                                val signal =
                                    observation.signal
                            ) {
                                is CgmPartner.Signal.Glucose ->
                                    signal.sequence

                                is CgmPartner.Signal.Temperature ->
                                    signal.sequence
                            }
                        },
                )

                val state =
                    withTimeout(
                        2_000L,
                    ) {
                        lab.family
                            .states
                            .first { state ->
                                state is
                                    CgmFamily.State.LatestAdmitted &&
                                    state.glucose.sequence ==
                                    2L
                            }
                    } as
                        CgmFamily.State.LatestAdmitted

                assertEquals(
                    106,
                    state.glucose
                        .mgDl,
                )
                assertEquals(
                    4,
                    state.storedObservationCount,
                )
                assertTrue(
                    snapshot
                        .latestObservedTemperature !=
                        null,
                )
            } finally {
                lab.close()
            }
        }
}

// [VXG RealForever][VEX-CGM-LABS][PARTNER-CIRCUIT-PATIENT-ZERO]
