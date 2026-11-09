package com.vextreme.vexlife.android.insulin

import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.behavior.VexMethod
import com.vextreme.vexlife.android.execution.VexExecution

/**
 * Semantic owner for the shared insulin-domain Store patient zero.
 *
 * This is a retained architecture fixture, not disposable placeholder logic. A future real BLE,
 * radio, SDK, or network realization may replace the synthetic provider edge while preserving the
 * source-readable Family / Partner / Store relationships proved here.
 *
 * Two independent Partners contribute evidence to one insulin-domain Store:
 *
 *     ConnectedPenPartner
 *     InsulinPumpPartner
 *              \
 *               -> InsulinStore -> InsulinFamily
 *
 * That convergence belongs to the Store because both Partners contribute observations to one
 * insulin-domain truth. It is not a Compound Relay between independently complete Family currents.
 *
 * The outbound pump path reuses the established Family grammar:
 *
 *     Parameter -> Control -> Effect + Approach -> Act.Method -> VexExecution
 *               -> Pump Partner Connection
 *
 * Provider acknowledgement is a Method result only. It deliberately does NOT admit a
 * delivered-insulin State; physical/durable delivery truth returns later through a Pump Partner
 * Signal -> Observation -> Receiver -> Store -> Family path.
 */
internal class InsulinFamily :
    VexFamily<
        InsulinFamily,
        InsulinFamily.State,
        InsulinFamily.Control,
        InsulinFamily.Effect
    >(
        initialState =
            State.Empty,
    ) {

    sealed interface Parameter :
        VexFamily.Parameter<InsulinFamily> {
        data class InsulinAmount(
            val unitsHundredths: Int,
        ) :
            Parameter {
            init {
                require(
                    unitsHundredths >
                        0,
                ) {
                    "synthetic insulin amount must be positive"
                }
            }
        }
    }

    sealed interface State :
        VexFamily.State<InsulinFamily> {
        data object Empty :
            State

        data class Admitted(
            val latestPenDose:
                ConnectedPenPartner.Signal.DoseRecorded?,
            val latestPenCartridge:
                ConnectedPenPartner.Signal.CartridgeObserved?,
            val latestPumpDelivery:
                InsulinPumpPartner.Signal.DeliveryObserved?,
            val latestPumpReservoir:
                InsulinPumpPartner.Signal.ReservoirObserved?,
            val storedObservationCount:
                Int,
        ) :
            State
    }

    sealed interface Control :
        VexFamily.Control<InsulinFamily> {
        data class RequestPumpDelivery(
            val amount:
                Parameter.InsulinAmount,
        ) :
            Control
    }

    sealed interface Effect :
        VexFamily.Effect<InsulinFamily> {
        data class RequestPumpDelivery(
            val amount:
                Parameter.InsulinAmount,
        ) :
            Effect
    }

    object Act {
        sealed class Method<
            out SuccessOutput,
            out FailureOutput,
        > :
            VexFamily.Act.Method<
                InsulinFamily,
                SuccessOutput,
                FailureOutput
            > {

            /**
             * Fully-bound realization of one accepted Family Effect.
             *
             * The same Family Parameter instance is carried through Control -> Effect -> Method.
             * The Method knows the concrete Pump Connection, while the Family itself does not.
             * VexExecution supplies Job custody and consumes the Family-authored Queued relationship;
             * this Method does not choose overlap policy again.
             */
            class RequestPumpDelivery(
                private val connection:
                    InsulinPumpPartner.Connection,
                val parameter:
                    Parameter.InsulinAmount,
            ) :
                Method<
                    InsulinPumpPartner.Connection.Acknowledgement.Accepted,
                    InsulinPumpPartner.Connection.Acknowledgement.Rejected
                >() {
                override suspend fun invoke():
                    VexMethod.Result<
                        InsulinPumpPartner.Connection.Acknowledgement.Accepted,
                        InsulinPumpPartner.Connection.Acknowledgement.Rejected
                    > =
                    when (
                        val acknowledgement =
                            connection.requestDelivery(
                                unitsHundredths =
                                    parameter
                                        .unitsHundredths,
                            )
                    ) {
                        is InsulinPumpPartner.Connection.Acknowledgement.Accepted ->
                            VexMethod.Result.Success(
                                output =
                                    acknowledgement,
                            )

                        is InsulinPumpPartner.Connection.Acknowledgement.Rejected ->
                            VexMethod.Result.Failure(
                                output =
                                    acknowledgement,
                            )
                    }
            }
        }
    }

    sealed interface DisplayState :
        VexFamily.DisplayState<
            InsulinFamily,
            State
        > {
        data object Empty :
            DisplayState

        data class Evidence(
            val penDoseHundredths:
                Int?,
            val pumpDeliveryHundredths:
                Int?,
            val pumpReservoirHundredths:
                Int?,
            val storedObservationCount:
                Int,
        ) :
            DisplayState
    }

    /**
     * Admit one already-coherent Store snapshot as insulin semantic current.
     *
     * Partner Conversion/Observation and Store admission have already happened before this boundary;
     * Family does not decode transport values or decide Connection provenance here.
     */
    fun observeStore(
        snapshot:
            InsulinStore.Snapshot,
    ):
        State {
        val next =
            if (
                snapshot.observationCount ==
                    0
            ) {
                State.Empty
            } else {
                State.Admitted(
                    latestPenDose =
                        snapshot
                            .latestPenDose,
                    latestPenCartridge =
                        snapshot
                            .latestPenCartridge,
                    latestPumpDelivery =
                        snapshot
                            .latestPumpDelivery,
                    latestPumpReservoir =
                        snapshot
                            .latestPumpReservoir,
                    storedObservationCount =
                        snapshot
                            .observationCount,
                )
            }

        return observe(
            next,
        )
    }

    override fun displayStateFor(
        state:
            State,
    ):
        DisplayState =
        when (
            state
        ) {
            State.Empty ->
                DisplayState.Empty

            is State.Admitted ->
                DisplayState.Evidence(
                    penDoseHundredths =
                        state
                            .latestPenDose
                            ?.unitsHundredths,
                    pumpDeliveryHundredths =
                        state
                            .latestPumpDelivery
                            ?.unitsHundredths,
                    pumpReservoirHundredths =
                        state
                            .latestPumpReservoir
                            ?.remainingUnitsHundredths,
                    storedObservationCount =
                        state
                            .storedObservationCount,
                )
        }

    /**
     * Request meaning is currently identical for Empty and Admitted State: every accepted delivery
     * request remains meaningful and must preserve admission order, so the Family authors Queued.
     *
     * Do not add State branches until insulin current truth actually changes whether this Control is
     * lawful or changes the Effect it requests.
     */
    override fun act(
        currentState:
            State,
        control:
            Control,
    ):
        VexFamily.Act<
            InsulinFamily,
            State,
            Effect
        > =
        when (
            control
        ) {
            is Control.RequestPumpDelivery ->
                stay(
                    effect =
                        Effect.RequestPumpDelivery(
                            amount =
                                control.amount,
                        ),
                    approach =
                        VexExecution.Approach.Queued,
                )
        }
}

// [VXG RealForever][VEX-PARTNER][INSULIN-FAMILY-PRESSURE]
