package com.vextreme.vexlife.android.cgm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Durable-domain boundary for the CGM patient zero.
 *
 * This first realization is intentionally in-memory so the PR can prove the Partner -> Store ->
 * Family relationship without simultaneously deciding Room schema/migrations. The observable
 * contract is deliberately Flow-shaped so a later Room-backed realization can replace the substrate
 * without turning Room Entity/DAO types into Family semantics.
 *
 * STORE != FAMILY STATE
 * STORE != PARTNER
 * STORE != DAO / ROOM ENTITY
 */
internal class CgmStore {
    data class Snapshot(
        val observations:
            List<
                com.vextreme.vexlife.android.behavior.VexPartner.Connection.Observation<
                    CgmPartner,
                    CgmPartner.Signal
                >
            > =
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

    internal fun commit(
        observation:
            com.vextreme.vexlife.android.behavior.VexPartner.Connection.Observation<
                CgmPartner,
                CgmPartner.Signal
            >,
    ):
        Snapshot =
        synchronized(
            lock,
        ) {
            val next =
                Snapshot(
                    observations =
                        mutableSnapshots
                            .value
                            .observations +
                            observation,
                )
            mutableSnapshots.value =
                next
            next
        }
}

// [VXG RealForever][VEX-CGM-LABS][STORE-PATIENT-ZERO]
