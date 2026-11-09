@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.insulin

import com.vextreme.vexlife.android.execution.VexExecution
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class InsulinPartnerCircuitTest {
    @Test
    fun familyCarriesTheSameInsulinAmountFromControlThroughEffectIntoBoundMethod() {
        val lab =
            InsulinPartnerLab()

        try {
            val family =
                lab.family
            val amount =
                InsulinFamily.Parameter.InsulinAmount(
                    unitsHundredths =
                        100,
                )

            val act =
                family.activate(
                    InsulinFamily.Control.RequestPumpDelivery(
                        amount =
                            amount,
                    ),
                )

            val effect =
                act?.effect as
                    InsulinFamily.Effect.RequestPumpDelivery
            val method =
                InsulinFamily.Act.Method.RequestPumpDelivery(
                    connection =
                        lab.pumpConnection,
                    parameter =
                        effect.amount,
                )

            assertSame(
                amount,
                effect.amount,
            )
            assertSame(
                amount,
                method.parameter,
            )
            assertEquals(
                VexExecution.Approach.Queued,
                act?.approach,
            )
            assertNotNull(
                act.relationshipId,
            )

            val secondAct =
                family.activate(
                    InsulinFamily.Control.RequestPumpDelivery(
                        amount =
                            InsulinFamily.Parameter.InsulinAmount(
                                unitsHundredths =
                                    125,
                            ),
                    ),
                )

            assertEquals(
                act.relationshipId,
                secondAct
                    ?.relationshipId,
            )
            assertEquals(
                InsulinFamily.State.Empty,
                family.currentState,
            )
        } finally {
            lab.close()
        }
    }

    @Test
    fun penSignalsCommitToSharedStoreAndThenReachFamilyWithoutInventingCgmContinuity() =
        runBlocking {
            val lab =
                InsulinPartnerLab(
                    nowEpochMillis = {
                        5_000L
                    },
                )

            try {
                val receipt =
                    lab.receiveSyntheticPenPacket()

                receipt.join()
                assertEquals(
                    2,
                    lab.store
                        .current
                        .penObservations
                        .size,
                )
                assertEquals(
                    0,
                    lab.store
                        .current
                        .pumpObservations
                        .size,
                )

                val dose =
                    lab.store
                        .current
                        .latestPenDose

                assertNotNull(
                    dose,
                )
                assertEquals(
                    "dose-1",
                    dose?.doseRef,
                )

                val state =
                    withTimeout(
                        2_000L,
                    ) {
                        lab.family
                            .states
                            .first { state ->
                                state is
                                    InsulinFamily.State.Admitted &&
                                    state.latestPenDose !=
                                    null
                            }
                    } as
                        InsulinFamily.State.Admitted

                assertSame(
                    dose,
                    state.latestPenDose,
                )
                assertEquals(
                    2,
                    state.storedObservationCount,
                )
            } finally {
                lab.close()
            }
        }

    @Test
    fun pumpAcknowledgementDoesNotBecomeStoredDeliveryUntilLaterPartnerSignalArrives() =
        runBlocking {
            val lab =
                InsulinPartnerLab(
                    nowEpochMillis = {
                        9_000L
                    },
                )

            try {
                val job =
                    lab.requestSyntheticPumpDelivery(
                        unitsHundredths =
                            100,
                    )

                job.join()

                val acknowledged =
                    lab.pumpRequestStatus
                        .value as
                        InsulinPartnerLab.PumpRequestStatus.Acknowledged

                assertEquals(
                    "pump-request-1",
                    acknowledged.requestRef,
                )

                /*
                 * Method success / Partner acknowledgement is not physical or durable delivery truth.
                 */
                assertEquals(
                    0,
                    lab.store
                        .current
                        .observationCount,
                )
                assertEquals(
                    InsulinFamily.State.Empty,
                    lab.family
                        .currentState,
                )

                val receipt =
                    checkNotNull(
                        lab.receiveSyntheticPumpDelivery(),
                    )
                receipt.join()
                assertEquals(
                    2,
                    lab.store
                        .current
                        .pumpObservations
                        .size,
                )

                val delivery =
                    lab.store
                        .current
                        .latestPumpDelivery

                assertNotNull(
                    delivery,
                )
                assertEquals(
                    acknowledged.requestRef,
                    delivery?.requestRef,
                )
                assertEquals(
                    100,
                    delivery?.unitsHundredths,
                )

                val state =
                    withTimeout(
                        2_000L,
                    ) {
                        lab.family
                            .states
                            .first { state ->
                                state is
                                    InsulinFamily.State.Admitted &&
                                    state.latestPumpDelivery !=
                                    null
                            }
                    } as
                        InsulinFamily.State.Admitted

                assertSame(
                    delivery,
                    state.latestPumpDelivery,
                )
                assertTrue(
                    lab.pumpRequestStatus
                        .value is
                        InsulinPartnerLab.PumpRequestStatus.DeliveryObserved,
                )
            } finally {
                lab.close()
            }
        }

    @Test
    fun penAndPumpRemainDistinctPartnersWhileConvergingIntoOneStoreAndFamily() =
        runBlocking {
            val lab =
                InsulinPartnerLab(
                    nowEpochMillis = {
                        12_000L
                    },
                )

            try {
                lab.receiveSyntheticPenPacket()
                    .join()

                val request =
                    lab.requestSyntheticPumpDelivery(
                        unitsHundredths =
                            125,
                    )
                request.join()
                checkNotNull(
                    lab.receiveSyntheticPumpDelivery(),
                ).join()

                val snapshot =
                    lab.store
                        .current

                assertEquals(
                    2,
                    snapshot
                        .penObservations
                        .size,
                )
                assertEquals(
                    2,
                    snapshot
                        .pumpObservations
                        .size,
                )
                assertEquals(
                    4,
                    snapshot
                        .observationCount,
                )

                val state =
                    withTimeout(
                        2_000L,
                    ) {
                        lab.family
                            .states
                            .first { state ->
                                state is
                                    InsulinFamily.State.Admitted &&
                                    state.latestPenDose !=
                                    null &&
                                    state.latestPumpDelivery !=
                                    null
                            }
                    } as
                        InsulinFamily.State.Admitted

                assertNotNull(
                    state.latestPenDose,
                )
                assertNotNull(
                    state.latestPumpDelivery,
                )
                assertEquals(
                    4,
                    state.storedObservationCount,
                )

                val display =
                    lab.family
                        .displayState as
                        InsulinFamily.DisplayState.Evidence

                assertEquals(
                    state
                        .latestPenDose
                        ?.unitsHundredths,
                    display
                        .penDoseHundredths,
                )
                assertEquals(
                    state
                        .latestPumpDelivery
                        ?.unitsHundredths,
                    display
                        .pumpDeliveryHundredths,
                )
            } finally {
                lab.close()
            }
        }
}

// [VXG RealForever][VEX-PARTNER][INSULIN-CIRCUIT-PROOF]
