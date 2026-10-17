package com.vextreme.vexlife.android.screens.shared.maps

/**
 * One provider-observed place candidate that can become a VexMaps route/distribution input.
 *
 * The provider owns the observation and stable provider-scoped [placeRef]. This value does not claim
 * the place is canonical, selected, routable, or current merely because a search returned it.
 */
data class VexMapsPlaceSearchResult(
    val placeRef: String,
    val name: String,
    val subtitle: String?,
    val coordinate: VexMapCoordinate,
) {
    init {
        require(placeRef.isNotBlank())
        require(name.isNotBlank())
    }
}

// [VXG RealForever]
