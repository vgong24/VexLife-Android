package com.vextreme.vexlife.android.screens.shared.maps.distribution.planning

import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionDraft
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionStop
import com.vextreme.vexlife.android.screens.shared.maps.VexMapCoordinate
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsTravelMode

internal data class VexMapsDistributionPlan(
    val sourceDraftRevision: Long,
    val origin: VexMapCoordinate,
    val orderedStops:
        List<VexMapsDistributionStop>,
    val returnToStart: Boolean,
    val travelMode: VexMapsTravelMode,
    val estimatedDurationSeconds: Long,
) {
    init {
        require(sourceDraftRevision >= 0L)
        require(orderedStops.isNotEmpty())
        require(estimatedDurationSeconds > 0L)
        require(
            orderedStops
                .map {
                    it.stopRef
                }
                .distinct()
                .size ==
                orderedStops.size,
        )
    }

    fun isCurrentFor(
        draft:
            VexMapsDistributionDraft,
    ): Boolean =
        sourceDraftRevision ==
            draft.revision &&
            orderedStops
                .map {
                    it.stopRef
                }
                .toSet() ==
            draft.stops
                .map {
                    it.stopRef
                }
                .toSet() &&
            returnToStart ==
                draft.returnToStart
}

internal data class VexMapsDistributionLegCost(
    val durationSeconds: Long,
) {
    init {
        require(durationSeconds > 0L)
    }
}

internal fun interface VexMapsDistributionLegCostProvider {
    suspend fun cost(
        from: VexMapCoordinate,
        to: VexMapCoordinate,
        travelMode: VexMapsTravelMode,
    ): VexMapsDistributionLegCost
}

// [VXG RealForever]
