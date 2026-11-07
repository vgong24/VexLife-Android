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
