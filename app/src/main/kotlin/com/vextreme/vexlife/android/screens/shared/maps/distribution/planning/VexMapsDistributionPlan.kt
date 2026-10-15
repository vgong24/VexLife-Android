package com.vextreme.vexlife.android.screens.shared.maps.distribution.planning

import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionDraft
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionStop
import com.vextreme.vexlife.android.screens.shared.maps.VexMapCoordinate
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsTravelMode

/**
 * Exact immutable inputs admitted when the human asks Distribution to prepare a run.
 *
 * This is intentionally feature-local rather than a generic request-currentness envelope.
 */
internal data class VexMapsDistributionPreparationSnapshot(
    val preparationRef: String,
    val draftRevision: Long,
    val stopRefs: List<String>,
    val returnToStart: Boolean,
    val origin: VexMapCoordinate,
    val travelMode: VexMapsTravelMode,
    val regionPackIdentity: String,
) {
    init {
        require(preparationRef.isNotBlank())
        require(draftRevision >= 0L)
        require(stopRefs.isNotEmpty())
        require(stopRefs.all { it.isNotBlank() })
        require(stopRefs.distinct().size == stopRefs.size)
        require(regionPackIdentity.isNotBlank())
    }

    fun matches(
        draft: VexMapsDistributionDraft,
        origin: VexMapCoordinate,
        travelMode: VexMapsTravelMode,
        regionPackIdentity: String,
    ): Boolean =
        draftRevision ==
            draft.revision &&
            stopRefs ==
            draft.stops
                .sortedBy {
                    it.inputOrder
                }
                .map {
                    it.stopRef
                } &&
            returnToStart ==
                draft.returnToStart &&
            this.origin ==
                origin &&
            this.travelMode ==
                travelMode &&
            this.regionPackIdentity ==
                regionPackIdentity
}

internal data class VexMapsDistributionPlan(
    val planRef: String,
    val preparation:
        VexMapsDistributionPreparationSnapshot,
    val orderedStops:
        List<VexMapsDistributionStop>,
    val estimatedDurationSeconds: Long,
) {
    init {
        require(planRef.isNotBlank())
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
        require(
            orderedStops
                .map {
                    it.stopRef
                }
                .toSet() ==
            preparation
                .stopRefs
                .toSet(),
        )
    }

    val origin: VexMapCoordinate
        get() =
            preparation.origin

    val returnToStart: Boolean
        get() =
            preparation.returnToStart

    val travelMode: VexMapsTravelMode
        get() =
            preparation.travelMode

    val sourceDraftRevision: Long
        get() =
            preparation.draftRevision

    fun isCurrentFor(
        draft:
            VexMapsDistributionDraft,
        origin:
            VexMapCoordinate,
        travelMode:
            VexMapsTravelMode,
        regionPackIdentity:
            String,
    ): Boolean =
        preparation.matches(
            draft =
                draft,
            origin =
                origin,
            travelMode =
                travelMode,
            regionPackIdentity =
                regionPackIdentity,
        )
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
