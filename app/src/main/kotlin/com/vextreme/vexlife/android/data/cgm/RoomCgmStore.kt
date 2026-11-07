package com.vextreme.vexlife.android.data.cgm

import android.content.Context
import com.vextreme.vexlife.android.cgm.CgmObservation
import com.vextreme.vexlife.android.cgm.CgmStore
import com.vextreme.vexlife.android.data.VexLifeDatabaseFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Process-surviving [CgmStore] realization.
 *
 * Room rows are physical historical evidence. Mapping them to Observation/Signal values restores
 * admitted Store truth; it does not replay Decoder/Receiver or claim a new Partner occurrence.
 */
internal class RoomCgmStore(
    private val dao:
        CgmObservationDao,
) :
    CgmStore {
    override val snapshots:
        Flow<CgmStore.Snapshot> =
        dao.observeAll()
            .map(
                ::snapshotOf,
            )

    override suspend fun commit(
        observation:
            CgmObservation,
    ) {
        dao.insert(
            observation
                .toEntity(),
        )
    }

    override suspend fun readCurrent():
        CgmStore.Snapshot =
        snapshotOf(
            dao.readAll(),
        )

    private fun snapshotOf(
        entities:
            List<CgmObservationEntity>,
    ):
        CgmStore.Snapshot =
        CgmStore.Snapshot(
            observations =
                entities.map(
                    CgmObservationEntity::toObservation,
                ),
        )
}

/**
 * Production/debug construction entry without exposing DAO or Room Entity types to the Screen.
 */
internal object CgmStoreFactory {
    fun production(
        context:
            Context,
    ):
        CgmStore =
        RoomCgmStore(
            dao =
                VexLifeDatabaseFactory
                    .get(
                        context.applicationContext,
                    )
                    .cgmObservationDao(),
        )
}

// [VXG RealForever][VEX-CGM-LABS][ROOM-STORE]
