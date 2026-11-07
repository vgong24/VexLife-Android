package com.vextreme.vexlife.android.partnerlab

import com.vextreme.vexlife.android.cgm.CgmFamily
import com.vextreme.vexlife.android.insulin.InsulinFamily
import com.vextreme.vexlife.android.insulin.InsulinPartnerLab
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PartnersCircuitLabTest {
    @Test
    fun syntheticRuleObservesCgmFamilyThenEntersInsulinFamilyThroughControl() =
        runBlocking {
            val lab =
                PartnersCircuitLab()

            try {
                lab.toggleSyntheticRule()

                /*
                 * First synthetic CGM packet is 103 mg/dL, below the arbitrary 106 fixture threshold.
                 */
                lab.receiveSyntheticCgmPacket()

                withTimeout(
                    2_000L,
                ) {
                    lab.cgm.family.states.first { state ->
                        state is
                            CgmFamily.State.LatestAdmitted &&
                            state.glucose.mgDl ==
                            103
                    }
                }

                assertTrue(
                    lab.insulin
                        .pumpRequestStatus
                        .value is
                        InsulinPartnerLab.PumpRequestStatus.Idle,
                )
                assertEquals(
                    0,
                    lab.insulin
                        .store
                        .current
                        .observationCount,
                )

                /*
                 * Second packet is 106 mg/dL. The sustained CgmFamily listener should trigger the
                 * lab rule, which enters InsulinFamily through Control and reaches the Pump Method.
                 */
                lab.receiveSyntheticCgmPacket()

                val acknowledgement =
                    withTimeout(
                        2_000L,
                    ) {
                        lab.insulin
                            .pumpRequestStatus
                            .first { status ->
                                status is
                                    InsulinPartnerLab.PumpRequestStatus.Acknowledged
                            }
                    } as
                        InsulinPartnerLab.PumpRequestStatus.Acknowledged

                assertEquals(
                    "pump-request-1",
                    acknowledgement.requestRef,
                )

                /*
                 * Partner acknowledgement still does not equal durable delivery truth.
                 */
                assertEquals(
                    0,
                    lab.insulin
                        .store
                        .current
                        .pumpObservations
                        .size,
                )
                assertTrue(
                    lab.insulin
                        .family
                        .currentState is
                        InsulinFamily.State.Empty,
                )
            } finally {
                lab.close()
            }
        }

    @Test
    fun laterPumpSignalClosesTheLoopThroughStoreAndInsulinFamily() =
        runBlocking {
            val lab =
                PartnersCircuitLab()

            try {
                lab.toggleSyntheticRule()
                lab.receiveSyntheticCgmPacket()
                lab.receiveSyntheticCgmPacket()

                withTimeout(
                    2_000L,
                ) {
                    lab.insulin
                        .pumpRequestStatus
                        .first { status ->
                            status is
                                InsulinPartnerLab.PumpRequestStatus.Acknowledged
                        }
                }

                lab.receiveSyntheticPumpOutcome()

                val state =
                    withTimeout(
                        2_000L,
                    ) {
                        lab.insulin
                            .family
                            .states
                            .first { state ->
                                state is
                                    InsulinFamily.State.Admitted &&
                                    state.latestPumpDelivery !=
                                    null
                            }
                    } as
                        InsulinFamily.State.Admitted

                assertEquals(
                    100,
                    state
                        .latestPumpDelivery
                        ?.unitsHundredths,
                )
                val automationStatus =
                    withTimeout(
                        2_000L,
                    ) {
                        lab.automationStatus
                            .first { status ->
                                status is
                                    PartnersCircuitLab.AutomationStatus.PumpOutcomeObserved
                            }
                    }

                assertTrue(
                    automationStatus is
                        PartnersCircuitLab.AutomationStatus.PumpOutcomeObserved,
                )
            } finally {
                lab.close()
            }
        }

    @Test
    fun disablingSyntheticRulePreventsCgmChangesFromRequestingPump() =
        runBlocking {
            val lab =
                PartnersCircuitLab()

            try {
                /*
                 * Leave the rule disabled and drive CGM above the fixture threshold.
                 */
                lab.receiveSyntheticCgmPacket()
                lab.receiveSyntheticCgmPacket()
                lab.receiveSyntheticCgmPacket()

                withTimeout(
                    2_000L,
                ) {
                    lab.cgm
                        .family
                        .states
                        .first { state ->
                            state is
                                CgmFamily.State.LatestAdmitted &&
                                state.glucose.sequence ==
                                3L
                        }
                }

                assertTrue(
                    lab.insulin
                        .pumpRequestStatus
                        .value is
                        InsulinPartnerLab.PumpRequestStatus.Idle,
                )
                assertTrue(
                    lab.automationStatus
                        .value is
                        PartnersCircuitLab.AutomationStatus.Disabled,
                )
            } finally {
                lab.close()
            }
        }
}

// [VXG RealForever][VEX-PARTNER][FEEDBACK-CIRCUIT-PROOF]
