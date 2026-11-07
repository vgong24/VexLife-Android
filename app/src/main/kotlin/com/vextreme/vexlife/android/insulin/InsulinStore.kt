package com.vextreme.vexlife.android.insulin

import com.vextreme.vexlife.android.behavior.VexPartner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One durable-domain convergence boundary fed by two different Partners.
 *
 * The in-memory realization is deliberate. This example pressures ownership first:
 *
 *     ConnectedPenPartner ----\
 *                              +-> InsulinStore -> InsulinFamily
 *     InsulinPumpPartner -----/
 *
 * Store keeps Partner provenance instead of flattening every fact into one generic event/value.
 */
internal class InsulinStore {
    typealias PenObservation =
        VexPartner.Connection.Observation<
            ConnectedPenPartner,
            ConnectedPenPartner.Signal
        >

    typealias PumpObservation =
        VexPartner.Connection.Observation<
            InsulinPumpPartner,
            InsulinPumpPartner.Signal
        >

    data class Snapshot(
        val penObservations:
            List<PenObservation> =
            emptyList(),
        val pumpObservations:
            List<PumpObservation> =
            emptyList(),
    ) {
        val latestPenDose:
            ConnectedPenPartner.Signal.DoseRecorded?
            get() =
                penObservations
                    .asReversed()
                    .firstNotNullOfOrNull { observation ->
                        observation.signal as?
                            ConnectedPenPartner.Signal.DoseRecorded
                    }

        val latestPenCartridge:
            ConnectedPenPartner.Signal.CartridgeObserved?
            get() =
                penObservations
                    .asReversed()
                    .firstNotNullOfOrNull { observation ->
                        observation.signal as?
                            ConnectedPenPartner.Signal.CartridgeObserved
                    }

        val latestPumpDelivery:
            InsulinPumpPartner.Signal.DeliveryObserved?
            get() =
                pumpObservations
                    .asReversed()
                    .firstNotNullOfOrNull { observation ->
                        observation.signal as?
                            InsulinPumpPartner.Signal.DeliveryObserved
                    }

        val latestPumpReservoir:
            InsulinPumpPartner.Signal.ReservoirObserved?
            get() =
                pumpObservations
                    .asReversed()
                    .firstNotNullOfOrNull { observation ->
                        observation.signal as?
                            InsulinPumpPartner.Signal.ReservoirObserved
                    }

        val observationCount:
            Int
            get() =
                penObservations.size +
                    pumpObservations.size
    }

    private val lock =
        Any()

    private val mutableSnapshots =
        MutableStateFlow(
            Snapshot(),
        )

    val snapshots:
        StateFlow<Snapshot> =
        mutableSnapshots
            .asStateFlow()

    val current:
        Snapshot
        get() =
            snapshots.value

    internal fun commitPen(
        observation:
            PenObservation,
    ):
        Snapshot =
        synchronized(
            lock,
        ) {
            publish(
                mutableSnapshots
                    .value
                    .copy(
                        penObservations =
                            mutableSnapshots
                                .value
                                .penObservations +
                                observation,
                    ),
            )
        }

    internal fun commitPump(
        observation:
            PumpObservation,
    ):
        Snapshot =
        synchronized(
            lock,
        ) {
            publish(
                mutableSnapshots
                    .value
                    .copy(
                        pumpObservations =
                            mutableSnapshots
                                .value
                                .pumpObservations +
                                observation,
                    ),
            )
        }

    private fun publish(
        snapshot: Snapshot,
    ):
        Snapshot {
        mutableSnapshots.value =
            snapshot
        return snapshot
    }
}

// [VXG RealForever][VEX-PARTNER][INSULIN-STORE-PRESSURE]
