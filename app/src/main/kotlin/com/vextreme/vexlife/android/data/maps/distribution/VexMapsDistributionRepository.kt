package com.vextreme.vexlife.android.data.maps.distribution

data class VexMapsDistributionStop(
    val stopRef: String,
    val placeRef: String?,
    val name: String,
    val subtitle: String?,
    val latitude: Double,
    val longitude: Double,
    val inputOrder: Int,
) {
    init {
        require(stopRef.isNotBlank())
        require(placeRef == null || placeRef.isNotBlank())
        require(name.isNotBlank())
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        require(inputOrder >= 0)
    }
}

data class VexMapsDistributionDraft(
    val revision: Long = 0L,
    val returnToStart: Boolean = true,
    val stops: List<VexMapsDistributionStop> = emptyList(),
) {
    init {
        require(revision >= 0L)
        require(
            stops.map { it.stopRef }.distinct().size ==
                stops.size,
        ) {
            "Distribution stop refs must be unique"
        }
        require(
            stops.map { it.inputOrder }.distinct().size ==
                stops.size,
        ) {
            "Distribution stop input orders must be unique"
        }
    }
}

/**
 * Durable Distribution draft/obligation boundary.
 *
 * The draft preserves what the human owes and the order in which those obligations were entered.
 * Route optimization must derive a separate Plan from one exact [VexMapsDistributionDraft.revision]
 * rather than rewrite this source truth into optimized order.
 */
interface VexMapsDistributionRepository {
    suspend fun loadDraft():
        VexMapsDistributionDraft

    suspend fun addStop(
        placeRef: String?,
        name: String,
        subtitle: String?,
        latitude: Double,
        longitude: Double,
    ): VexMapsDistributionDraft

    suspend fun removeStop(
        stopRef: String,
    ): VexMapsDistributionDraft

    suspend fun setReturnToStart(
        enabled: Boolean,
    ): VexMapsDistributionDraft

    suspend fun clearDraft():
        VexMapsDistributionDraft
}

// [VXG RealForever]
