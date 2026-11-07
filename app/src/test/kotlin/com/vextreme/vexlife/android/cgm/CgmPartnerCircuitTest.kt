package com.vextreme.vexlife.android.cgm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CgmPartnerCircuitTest {
    @Test
    fun oneSyntheticPacketDecodesObservesStoresAndReachesFamilyProjection() {
        val lab =
            CgmPartnerLab(
                nowEpochMillis = {
                    1_000L
                },
            )

        val observations =
            lab.receiveSyntheticPacket()

        assertEquals(
            2,
            observations.size,
        )
        assertEquals(
            2,
            lab.store
                .current
                .observations
                .size,
        )
        assertEquals(
            CgmPartner.Connection.Id(
                value =
                    "cgm.synthetic.connection",
            ),
            observations
                .first()
                .connectionId,
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
            lab.family
                .currentState as
                CgmFamily.State.LatestAdmitted

        /*
         * The exact Partner statement remains recognizable; Family does not reconstruct a second
         * glucose object merely to express semantic State.
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
    }

    @Test
    fun laterPartnerReceiptFlowsThroughStoreBeforeFamilyInterpretsTheNewState() {
        val lab =
            CgmPartnerLab(
                nowEpochMillis = {
                    2_000L
                },
            )

        lab.receiveSyntheticPacket()
        lab.receiveSyntheticPacket()

        assertEquals(
            4,
            lab.store
                .current
                .observations
                .size,
        )

        val state =
            lab.family
                .currentState as
                CgmFamily.State.LatestAdmitted

        assertEquals(
            2L,
            state.glucose
                .sequence,
        )
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
            lab.store
                .current
                .latestObservedTemperature !=
                null,
        )
    }
}

// [VXG RealForever][VEX-CGM-LABS][PARTNER-CIRCUIT-PATIENT-ZERO]
