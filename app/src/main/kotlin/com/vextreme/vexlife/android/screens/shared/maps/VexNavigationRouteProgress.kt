package com.vextreme.vexlife.android.screens.shared.maps

/**
 * Provider-independent route meaning consumed by the VexMaps navigation core.
 *
 * A routing adapter may obtain this from Google, another provider, VexHome, or a deterministic
 * fixture. Provider SDK objects do not cross this boundary.
 */
data class VexNavigationRoute(
    val routeRef: String,
    val routeRevision: Long,
    val destinationLabel: String,
    val totalDistanceMeters: Int,
    val maneuvers: List<VexNavigationManeuver>,
) {
    init {
        require(routeRef.isNotBlank()) {
            "routeRef must not be blank"
        }
        require(routeRevision >= 0L) {
            "routeRevision must be non-negative"
        }
        require(destinationLabel.isNotBlank()) {
            "destinationLabel must not be blank"
        }
        require(totalDistanceMeters > 0) {
            "totalDistanceMeters must be positive"
        }
        require(
            maneuvers
                .map { maneuver -> maneuver.maneuverRef }
                .distinct()
                .size == maneuvers.size,
        ) {
            "maneuver refs must be unique inside one route revision"
        }
        require(
            maneuvers.zipWithNext().all { (left, right) ->
                left.distanceFromRouteStartMeters <
                    right.distanceFromRouteStartMeters
            },
        ) {
            "maneuvers must be strictly ordered along the route"
        }
        require(
            maneuvers.all { maneuver ->
                maneuver.distanceFromRouteStartMeters <=
                    totalDistanceMeters
            },
        ) {
            "maneuvers must stay inside the route distance"
        }
    }

    fun deepCopy(): VexNavigationRoute =
        copy(
            maneuvers =
                maneuvers.map { maneuver ->
                    maneuver.copy()
                },
        )
}

data class VexNavigationManeuver(
    val maneuverRef: String,
    val instructionProjection: String,
    val distanceFromRouteStartMeters: Int,
) {
    init {
        require(maneuverRef.isNotBlank()) {
            "maneuverRef must not be blank"
        }
        require(instructionProjection.isNotBlank()) {
            "instructionProjection must not be blank"
        }
        require(distanceFromRouteStartMeters >= 0) {
            "distanceFromRouteStartMeters must be non-negative"
        }
    }
}

/**
 * Route-matched navigation fix.
 *
 * Raw GPS coordinates intentionally do not belong to this contract. An Android/provider adapter may
 * use GPS, road snapping, dead reckoning, or a simulator to produce this route-relative projection.
 * That keeps location permission and provider-specific map matching outside deterministic route
 * progress semantics.
 */
data class VexNavigationFix(
    val fixRef: String,
    val routeRef: String,
    val routeRevision: Long,
    val distanceAlongRouteMeters: Int,
) {
    init {
        require(fixRef.isNotBlank()) {
            "fixRef must not be blank"
        }
        require(routeRef.isNotBlank()) {
            "routeRef must not be blank"
        }
        require(routeRevision >= 0L) {
            "routeRevision must be non-negative"
        }
        require(distanceAlongRouteMeters >= 0) {
            "distanceAlongRouteMeters must be non-negative"
        }
    }

    fun deepCopy(): VexNavigationFix = copy()
}

data class VexNavigationManeuverProgress(
    val maneuver: VexNavigationManeuver,
    val distanceToManeuverMeters: Int,
) {
    init {
        require(distanceToManeuverMeters >= 0) {
            "distanceToManeuverMeters must be non-negative"
        }
    }

    fun deepCopy(): VexNavigationManeuverProgress =
        copy(
            maneuver = maneuver.copy(),
        )
}

data class VexNavigationRouteProgress(
    val routeRef: String,
    val routeRevision: Long,
    val destinationLabel: String,
    val distanceAlongRouteMeters: Int,
    val distanceRemainingMeters: Int,
    val nextManeuver: VexNavigationManeuverProgress?,
    val arrived: Boolean,
) {
    fun deepCopy(): VexNavigationRouteProgress =
        copy(
            nextManeuver = nextManeuver?.deepCopy(),
        )
}

/**
 * Pure owner of route-relative progress math.
 *
 * This object has no Android, renderer, network, permission, audio, or provider dependency. It can
 * therefore be proven from deterministic fixtures before a real location/routing adapter exists.
 */
object VexNavigationRouteProgressOwner {
    fun project(
        route: VexNavigationRoute,
        fix: VexNavigationFix,
    ): VexNavigationRouteProgress {
        require(fix.routeRef == route.routeRef) {
            "fix routeRef does not match route"
        }
        require(fix.routeRevision == route.routeRevision) {
            "fix routeRevision does not match route"
        }

        val distanceAlongRouteMeters =
            fix.distanceAlongRouteMeters
                .coerceAtMost(route.totalDistanceMeters)

        val distanceRemainingMeters =
            (
                route.totalDistanceMeters -
                    distanceAlongRouteMeters
            ).coerceAtLeast(0)

        val nextManeuver =
            route.maneuvers
                .firstOrNull { maneuver ->
                    maneuver.distanceFromRouteStartMeters >
                        distanceAlongRouteMeters
                }
                ?.let { maneuver ->
                    VexNavigationManeuverProgress(
                        maneuver = maneuver.copy(),
                        distanceToManeuverMeters =
                            maneuver.distanceFromRouteStartMeters -
                                distanceAlongRouteMeters,
                    )
                }

        return VexNavigationRouteProgress(
            routeRef = route.routeRef,
            routeRevision = route.routeRevision,
            destinationLabel = route.destinationLabel,
            distanceAlongRouteMeters = distanceAlongRouteMeters,
            distanceRemainingMeters = distanceRemainingMeters,
            nextManeuver = nextManeuver,
            arrived = distanceRemainingMeters == 0,
        )
    }
}

// [VXG RealForever]
