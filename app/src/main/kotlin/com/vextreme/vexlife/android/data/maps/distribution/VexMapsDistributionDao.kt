package com.vextreme.vexlife.android.data.maps.distribution

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class VexMapsDistributionDao {
    @Query(
        """
        SELECT * FROM vex_maps_distribution_draft
        WHERE draft_ref = :draftRef
        LIMIT 1
        """
    )
    abstract suspend fun readDraftEntity(
        draftRef: String,
    ): VexMapsDistributionDraftEntity?

    @Query(
        """
        SELECT * FROM vex_maps_distribution_stop
        WHERE draft_ref = :draftRef
        ORDER BY input_order ASC
        """
    )
    abstract suspend fun readStops(
        draftRef: String,
    ): List<VexMapsDistributionStopEntity>

    @Insert(
        onConflict = OnConflictStrategy.IGNORE,
    )
    protected abstract suspend fun insertDraftIfAbsent(
        entity: VexMapsDistributionDraftEntity,
    ): Long

    @Insert(
        onConflict = OnConflictStrategy.ABORT,
    )
    protected abstract suspend fun insertStop(
        entity: VexMapsDistributionStopEntity,
    )

    @Query(
        """
        DELETE FROM vex_maps_distribution_stop
        WHERE draft_ref = :draftRef
          AND stop_ref = :stopRef
        """
    )
    protected abstract suspend fun deleteStop(
        draftRef: String,
        stopRef: String,
    ): Int

    @Query(
        """
        DELETE FROM vex_maps_distribution_stop
        WHERE draft_ref = :draftRef
        """
    )
    protected abstract suspend fun deleteAllStops(
        draftRef: String,
    ): Int

    @Query(
        """
        UPDATE vex_maps_distribution_draft
        SET revision = :revision,
            return_to_start = :returnToStart,
            updated_at_epoch_millis = :updatedAtEpochMillis
        WHERE draft_ref = :draftRef
        """
    )
    protected abstract suspend fun updateDraft(
        draftRef: String,
        revision: Long,
        returnToStart: Boolean,
        updatedAtEpochMillis: Long,
    ): Int

    @Transaction
    open suspend fun readDraft(
        draftRef: String =
            VEX_MAPS_DISTRIBUTION_DRAFT_REF,
    ): VexMapsDistributionDraft {
        ensureDraft(
            draftRef = draftRef,
            updatedAtEpochMillis = 0L,
        )
        return snapshot(
            draftRef,
        )
    }

    @Transaction
    open suspend fun addStop(
        stopRef: String,
        placeRef: String?,
        name: String,
        subtitle: String?,
        latitude: Double,
        longitude: Double,
        updatedAtEpochMillis: Long,
        draftRef: String =
            VEX_MAPS_DISTRIBUTION_DRAFT_REF,
    ): VexMapsDistributionDraft {
        ensureDraft(
            draftRef =
                draftRef,
            updatedAtEpochMillis =
                updatedAtEpochMillis,
        )
        val before =
            snapshot(
                draftRef,
            )
        val inputOrder =
            before.stops
                .maxOfOrNull {
                    it.inputOrder
                }
                ?.plus(1)
                ?: 0

        insertStop(
            VexMapsDistributionStopEntity(
                stopRef = stopRef,
                draftRef = draftRef,
                placeRef = placeRef,
                name = name,
                subtitle = subtitle,
                latitude = latitude,
                longitude = longitude,
                inputOrder = inputOrder,
            ),
        )
        require(
            updateDraft(
                draftRef = draftRef,
                revision =
                    before.revision + 1L,
                returnToStart =
                    before.returnToStart,
                updatedAtEpochMillis =
                    updatedAtEpochMillis,
            ) == 1,
        ) {
            "Distribution draft row disappeared while adding a stop"
        }

        return snapshot(
            draftRef,
        )
    }

    @Transaction
    open suspend fun removeStop(
        stopRef: String,
        updatedAtEpochMillis: Long,
        draftRef: String =
            VEX_MAPS_DISTRIBUTION_DRAFT_REF,
    ): VexMapsDistributionDraft {
        ensureDraft(
            draftRef =
                draftRef,
            updatedAtEpochMillis =
                updatedAtEpochMillis,
        )
        val before =
            snapshot(
                draftRef,
            )
        val removed =
            deleteStop(
                draftRef =
                    draftRef,
                stopRef =
                    stopRef,
            )

        if (removed == 0) {
            return before
        }

        require(
            updateDraft(
                draftRef = draftRef,
                revision =
                    before.revision + 1L,
                returnToStart =
                    before.returnToStart,
                updatedAtEpochMillis =
                    updatedAtEpochMillis,
            ) == 1,
        ) {
            "Distribution draft row disappeared while removing a stop"
        }

        return snapshot(
            draftRef,
        )
    }

    @Transaction
    open suspend fun setReturnToStart(
        enabled: Boolean,
        updatedAtEpochMillis: Long,
        draftRef: String =
            VEX_MAPS_DISTRIBUTION_DRAFT_REF,
    ): VexMapsDistributionDraft {
        ensureDraft(
            draftRef =
                draftRef,
            updatedAtEpochMillis =
                updatedAtEpochMillis,
        )
        val before =
            snapshot(
                draftRef,
            )

        if (
            before.returnToStart ==
            enabled
        ) {
            return before
        }

        require(
            updateDraft(
                draftRef = draftRef,
                revision =
                    before.revision + 1L,
                returnToStart =
                    enabled,
                updatedAtEpochMillis =
                    updatedAtEpochMillis,
            ) == 1,
        ) {
            "Distribution draft row disappeared while changing return policy"
        }

        return snapshot(
            draftRef,
        )
    }

    @Transaction
    open suspend fun clearDraft(
        updatedAtEpochMillis: Long,
        draftRef: String =
            VEX_MAPS_DISTRIBUTION_DRAFT_REF,
    ): VexMapsDistributionDraft {
        ensureDraft(
            draftRef =
                draftRef,
            updatedAtEpochMillis =
                updatedAtEpochMillis,
        )
        val before =
            snapshot(
                draftRef,
            )

        if (before.stops.isEmpty()) {
            return before
        }

        deleteAllStops(
            draftRef,
        )
        require(
            updateDraft(
                draftRef = draftRef,
                revision =
                    before.revision + 1L,
                returnToStart =
                    before.returnToStart,
                updatedAtEpochMillis =
                    updatedAtEpochMillis,
            ) == 1,
        ) {
            "Distribution draft row disappeared while clearing stops"
        }

        return snapshot(
            draftRef,
        )
    }

    private suspend fun ensureDraft(
        draftRef: String,
        updatedAtEpochMillis: Long,
    ) {
        insertDraftIfAbsent(
            VexMapsDistributionDraftEntity(
                draftRef = draftRef,
                updatedAtEpochMillis =
                    updatedAtEpochMillis,
            ),
        )
    }

    private suspend fun snapshot(
        draftRef: String,
    ): VexMapsDistributionDraft {
        val entity =
            requireNotNull(
                readDraftEntity(
                    draftRef,
                ),
            )

        return VexMapsDistributionDraft(
            revision =
                entity.revision,
            returnToStart =
                entity.returnToStart,
            stops =
                readStops(
                    draftRef,
                ).map {
                    it.toDomain()
                },
        )
    }
}

// [VXG RealForever]
