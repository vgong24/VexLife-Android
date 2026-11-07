@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.insulin

import com.vextreme.vexlife.android.behavior.VexMethod
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Concrete pressure fixture for two Partners converging into one Store/Family.
 *
 * This remains a lab composition owner, not a generic Partnership object.
 */
internal class InsulinPartnerLab(
    private val nowEpochMillis:
        () -> Long =
        System::currentTimeMillis,
    val store:
        InsulinStore =
        InsulinStore(),
    val family:
        InsulinFamily =
        InsulinFamily(),
    private val executionRoot:
        VexExecutionRoot =
        VexExecutionRoot(),
) {
    sealed interface PumpRequestStatus {
        data object Idle :
            PumpRequestStatus

        data class Acknowledged(
            val requestRef: String,
            val unitsHundredths: Int,
        ) :
            PumpRequestStatus

        data class Rejected(
            val reason: String,
        ) :
            PumpRequestStatus

        data class Errored(
            val message: String,
        ) :
            PumpRequestStatus

        data class DeliveryObserved(
            val requestRef: String,
            val deliveryRef: String,
            val unitsHundredths: Int,
        ) :
            PumpRequestStatus
    }

    private val penConnection =
        ConnectedPenPartner.Connection(
            id =
                ConnectedPenPartner.Connection.Id(
                    value =
                        "pen.synthetic.connection",
                ),
            decoder =
                ConnectedPenPartner.Connection.Decoder(),
            receiver =
                ConnectedPenPartner.Connection.Receiver(
                    store =
                        store,
                ),
        )

    internal val pumpConnection =
        InsulinPumpPartner.Connection(
            id =
                InsulinPumpPartner.Connection.Id(
                    value =
                        "pump.synthetic.connection",
                ),
            decoder =
                InsulinPumpPartner.Connection.Decoder(),
            receiver =
                InsulinPumpPartner.Connection.Receiver(
                    store =
                        store,
                ),
        )

    private var nextPenDose =
        1L

    private var nextPumpDelivery =
        1L

    private val mutablePumpRequestStatus =
        MutableStateFlow<PumpRequestStatus>(
            PumpRequestStatus.Idle,
        )

    val pumpRequestStatus:
        StateFlow<PumpRequestStatus> =
        mutablePumpRequestStatus
            .asStateFlow()

    init {
        family.observeStore(
            store.current,
        )
    }

    fun receiveSyntheticPenPacket():
        List<
            ConnectedPenPartner.Connection.Observation
        > {
        val occurrence =
            nextPenDose++
        val occurredAt =
            nowEpochMillis()
        val doseHundredths =
            50 +
                (
                    occurrence *
                        25L
                )
                    .rem(
                        250L,
                    )
                    .toInt()
        val remainingHundredths =
            30_000 -
                doseHundredths

        val packet =
            (
                "d|pen-synthetic|cartridge-a|dose-" +
                    occurrence +
                    "|" +
                    occurredAt +
                    "|" +
                    doseHundredths +
                    ";" +
                    "c|pen-synthetic|cartridge-a|" +
                    remainingHundredths
            )
                .toByteArray(
                    Charsets.UTF_8,
                )

        val observations =
            penConnection.receive(
                bytes =
                    packet,
                observedAtEpochMillis =
                    nowEpochMillis(),
            )

        family.observeStore(
            store.current,
        )

        return observations
    }

    /**
     * Human/Synthetic Surface -> Family Control -> Effect -> bound Act.Method -> Pump Partner.
     *
     * Provider acknowledgement updates only the lab diagnostic status. Store and Family State do not
     * change until a later Pump Signal is received and admitted.
     */
    fun requestSyntheticPumpDelivery(
        unitsHundredths:
            Int =
            100,
    ):
        Job {
        val parameter =
            InsulinFamily.Parameter.InsulinAmount(
                unitsHundredths =
                    unitsHundredths,
            )
        val act =
            checkNotNull(
                family.activate(
                    InsulinFamily.Control.RequestPumpDelivery(
                        amount =
                            parameter,
                    ),
                ),
            )
        val effect =
            checkNotNull(
                act.effect as?
                    InsulinFamily.Effect.RequestPumpDelivery,
            )
        val method =
            InsulinFamily.Act.Method.RequestPumpDelivery(
                connection =
                    pumpConnection,
                parameter =
                    effect.amount,
            )

        return checkNotNull(
            act.launchWith(
                executionRoot,
            ),
        ) {
            "Pump request Effect requires an executable Family Act"
        }
            .method(
                method =
                    method,
                onResult = { result ->
                    mutablePumpRequestStatus.value =
                        when (
                            result
                        ) {
                            is VexMethod.Result.Success ->
                                PumpRequestStatus.Acknowledged(
                                    requestRef =
                                        result
                                            .output
                                            .requestRef,
                                    unitsHundredths =
                                        result
                                            .output
                                            .unitsHundredths,
                                )

                            is VexMethod.Result.Failure ->
                                PumpRequestStatus.Rejected(
                                    reason =
                                        result
                                            .output
                                            .reason,
                                )

                            is VexMethod.Result.Error ->
                                PumpRequestStatus.Errored(
                                    message =
                                        result
                                            .cause
                                            .message
                                            ?: result
                                                .cause
                                                .javaClass
                                                .simpleName,
                                )
                        }
                },
            )
    }

    /**
     * Simulate later physical/provider evidence after an acknowledged request.
     *
     * This is intentionally a separate action so:
     *
     *     EFFECT_REQUESTED
     *     != PARTNER_ACKNOWLEDGED
     *     != PHYSICAL_OUTCOME
     *     != DURABLE_DOMAIN_TRUTH
     */
    fun receiveSyntheticPumpDelivery():
        List<
            InsulinPumpPartner.Connection.Observation
        > {
        val acknowledged =
            pumpRequestStatus
                .value as?
                PumpRequestStatus.Acknowledged
                ?: return emptyList()

        val occurrence =
            nextPumpDelivery++
        val occurredAt =
            nowEpochMillis()
        val deliveryRef =
            "delivery-" +
                occurrence
        val remainingHundredths =
            20_000 -
                acknowledged
                    .unitsHundredths

        val packet =
            (
                "d|pump-synthetic|" +
                    deliveryRef +
                    "|" +
                    acknowledged.requestRef +
                    "|" +
                    occurredAt +
                    "|" +
                    acknowledged.unitsHundredths +
                    ";" +
                    "r|pump-synthetic|" +
                    remainingHundredths
            )
                .toByteArray(
                    Charsets.UTF_8,
                )

        val observations =
            pumpConnection.receive(
                bytes =
                    packet,
                observedAtEpochMillis =
                    nowEpochMillis(),
            )

        family.observeStore(
            store.current,
        )

        mutablePumpRequestStatus.value =
            PumpRequestStatus.DeliveryObserved(
                requestRef =
                    acknowledged.requestRef,
                deliveryRef =
                    deliveryRef,
                unitsHundredths =
                    acknowledged
                        .unitsHundredths,
            )

        return observations
    }

    fun close() {
        executionRoot.forceStopAll(
            "Insulin Partner Lab disposed",
        )
    }
}

// [VXG RealForever][VEX-PARTNER][INSULIN-PARTNER-LAB]
