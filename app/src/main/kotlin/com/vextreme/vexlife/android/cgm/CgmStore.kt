package com.vextreme.vexlife.android.cgm

import com.vextreme.vexlife.android.behavior.VexPartner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal typealias CgmObservation =
    VexPartner.Connection.Observation<
        CgmPartner,
        CgmPartner.Signal
    >

/**
 * Durable-domain boundary for admitted CGM Partner observations.
 *
 * Store owns what survives disconnect/restart; it does not own Partner transport or Family meaning.
 * Physical persistence is a realization beneath this contract.
 *
 * STORE != FAMILY STATE
 * STORE != PARTNER
 * STORE != DAO / ROOM ENTITY
 * ENTITY_RESTORED != PARTNER_SIGNAL_RECEIVED
 */
internal interface CgmStore {
    data class Snapshot(
        val observations:
            List<CgmObservation> =
            emptyList(),
    ) {
        val latestObservedGlucose:
            CgmPartner.Signal.Glucose?
            get() =
                observations
                    .asReversed()
                    .firstNotNullOfOrNull { observation ->
                        observation.signal as?
                            CgmPartner.Signal.Glucose
                    }

        val latestObservedTemperature:
            CgmPartner.Signal.Temperature?
            get() =
                observations
                    .asReversed()
                    .firstNotNullOfOrNull { observation ->
                        observation.signal as?
                            CgmPartner.Signal.Temperature
                    }
    }

    /**
     * Current durable truth as an observable source.
     *
     * A Room realization emits restored rows here after process restart. Restoration does not call
     * Decoder or Receiver again; it reconstructs historical Observation values already admitted.
     */
    val snapshots:
        Flow<Snapshot>

    /**
     * Admit one historical Partner Observation durably.
     *
     * Completion means this Store realization has completed its own admission effect. Callers must
     * not treat an earlier Connection callback as equivalent to this completion.
     */
    suspend fun commit(
        observation:
            CgmObservation,
    )

    /** Read the current durable Store truth without manufacturing a new Partner receipt. */
    suspend fun readCurrent():
        Snapshot
}

/** Preview/unit realization of [CgmStore]. */
internal class InMemoryCgmStore :
    CgmStore {
    private val lock =
        Any()

    private val mutableSnapshots =
        MutableStateFlow(
            CgmStore.Snapshot(),
        )

    override val snapshots =
        mutableSnapshots
            .asStateFlow()

    override suspend fun commit(
        observation:
            CgmObservation,
    ) {
        synchronized(
            lock,
        ) {
            mutableSnapshots.value =
                CgmStore.Snapshot(
                    observations =
                        mutableSnapshots
                            .value
                            .observations +
                            observation,
                )
        }
    }

    override suspend fun readCurrent():
        CgmStore.Snapshot =
        mutableSnapshots.value
}

// [VXG RealForever][VEX-CGM-LABS][STORE-PATIENT-ZERO]
