package com.vextreme.vexlife.android.screens.shared.maps

/**
 * Feature-local planning/search state and small admission helpers.
 *
 * This file owns no Compose rendering, provider SDK, repository or active-navigation lifetime. It
 * keeps the state transitions used by VexMaps composition and planner presentation independently
 * testable/addressable.
 */
/**
 * Admits only the newest result from a family of asynchronous composition-owned requests.
 *
 * The gate does not decide whether a route/search/location result is semantically valid; the owning
 * domain/provider code still does that. It answers the narrower race question: "is this coroutine
 * result still the response to the latest request the current composition cares about?"
 */
internal class VexMapsLatestRequestGate {
    private var generation = 0L

    fun begin(): Long {
        generation += 1L
        return generation
    }

    fun invalidate() {
        generation += 1L
    }

    fun accepts(
        requestGeneration: Long,
    ): Boolean =
        requestGeneration ==
            generation
}

internal sealed interface VexMapsPlaceSearchState {
    data object Idle :
        VexMapsPlaceSearchState

    data object Importing :
        VexMapsPlaceSearchState

    data object Searching :
        VexMapsPlaceSearchState

    data class Results(
        val places: List<VexMapsPlaceSearchResult>,
    ) : VexMapsPlaceSearchState

    data class Error(
        val error: VexMapsPlaceSearchError,
    ) : VexMapsPlaceSearchState
}

internal enum class VexMapsPlaceSearchError {
    IMPORT,
    SEARCH,
}

internal fun shouldVexMapsCurrentLocationAffectRoute(
    selectedOriginLatitude: Double?,
    selectedOriginLongitude: Double?,
): Boolean =
    restoreVexMapCoordinate(
        latitude =
            selectedOriginLatitude,
        longitude =
            selectedOriginLongitude,
    ) == null

internal fun restoreVexMapCoordinate(
    latitude: Double?,
    longitude: Double?,
): VexMapCoordinate? {
    if (
        latitude == null ||
        longitude == null
    ) {
        return null
    }

    return runCatching {
        VexMapCoordinate(
            latitude = latitude,
            longitude = longitude,
        )
    }.getOrNull()
}

internal fun restoreVexMapsMapSelectionTarget(
    savedName: String,
): VexMapsMapSelectionTarget =
    VexMapsMapSelectionTarget
        .entries
        .firstOrNull { target ->
            target.name == savedName
        }
        ?: VexMapsMapSelectionTarget.DESTINATION

internal fun restoreVexMapsTravelMode(
    savedName: String,
): VexMapsTravelMode =
    VexMapsTravelMode
        .entries
        .firstOrNull { mode ->
            mode.name == savedName
        }
        ?: VexMapsTravelMode.DRIVE

internal fun resolveVexMapsRouteOrigin(
    selectedOrigin: VexMapCoordinate?,
    deviceOrigin: VexMapCoordinate?,
): VexMapCoordinate? =
    selectedOrigin ?: deviceOrigin

internal enum class VexMapsMapSelectionTarget {
    ORIGIN,
    DESTINATION,
}

internal fun nextVexMapsSelectionTarget(
    selectedRole: VexMapsMapSelectionTarget,
): VexMapsMapSelectionTarget =
    when (selectedRole) {
        VexMapsMapSelectionTarget.ORIGIN ->
            VexMapsMapSelectionTarget.DESTINATION

        VexMapsMapSelectionTarget.DESTINATION ->
            VexMapsMapSelectionTarget.DESTINATION
    }

internal sealed interface VexMapsPlanningState {
    data object Idle :
        VexMapsPlanningState

    data object Importing :
        VexMapsPlanningState

    data object Routing :
        VexMapsPlanningState

    data class Error(
        val error: VexMapsPlanningError,
    ) : VexMapsPlanningState
}

internal enum class VexMapsPlanningError {
    REGION_IMPORT,
    ROUTE,
}

internal fun VexMapsPlanningState
    .afterVexMapsRouteInputChanged():
    VexMapsPlanningState =
    when (this) {
        VexMapsPlanningState.Routing ->
            VexMapsPlanningState.Idle

        is VexMapsPlanningState.Error ->
            if (
                error ==
                VexMapsPlanningError.ROUTE
            ) {
                VexMapsPlanningState.Idle
            } else {
                this
            }

        VexMapsPlanningState.Idle,
        VexMapsPlanningState.Importing ->
            this
    }

// [VXG RealForever]
