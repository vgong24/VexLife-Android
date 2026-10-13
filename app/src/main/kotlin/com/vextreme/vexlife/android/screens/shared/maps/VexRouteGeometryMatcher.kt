package com.vextreme.vexlife.android.screens.shared.maps

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt

data class VexNavigationLocationSample(
    val sampleRef: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val elapsedRealtimeMillis: Long,
) {
    init {
        require(sampleRef.isNotBlank())
        require(
            latitude.isFinite() &&
                latitude in -90.0..90.0,
        )
        require(
            longitude.isFinite() &&
                longitude in -180.0..180.0,
        )
        require(elapsedRealtimeMillis >= 0L)
        require(
            accuracyMeters == null ||
                (
                    accuracyMeters.isFinite() &&
                        accuracyMeters >= 0f
                ),
        )
    }
}

data class VexRouteMatchingPolicy(
    val maximumFixAgeMillis: Long =
        10_000L,
    val maximumAccuracyMeters: Float =
        80f,
    val minimumOffRouteDistanceMeters:
        Double = 35.0,
    val accuracyOffRouteMultiplier:
        Double = 2.0,
) {
    init {
        require(maximumFixAgeMillis > 0L)
        require(maximumAccuracyMeters > 0f)
        require(
            minimumOffRouteDistanceMeters >
                0.0,
        )
        require(
            accuracyOffRouteMultiplier >=
                1.0,
        )
    }

    fun acceptsAccuracy(
        accuracyMeters: Float?,
    ): Boolean =
        accuracyMeters == null ||
            accuracyMeters <=
            maximumAccuracyMeters
}

sealed interface VexRouteMatchResult {
    data class Matched(
        val navigationFix: VexNavigationFix,
        val distanceFromRouteMeters: Double,
    ) : VexRouteMatchResult

    data class OffRoute(
        val distanceFromRouteMeters: Double,
        val thresholdMeters: Double,
    ) : VexRouteMatchResult

    data class Inaccurate(
        val accuracyMeters: Float,
    ) : VexRouteMatchResult

    data object Stale :
        VexRouteMatchResult

    data object OutOfOrder :
        VexRouteMatchResult
}

/**
 * Stateful admission boundary between high-frequency device coordinates and semantic route progress.
 *
 * The matcher retains only the last accepted/observed monotonic sample time. It does not own Android
 * permissions or callbacks, routing, rerouting, or presentation.
 */
class VexRouteGeometryMatcher(
    private val route:
        VexNavigationRoute,
    geometry:
        List<VexRouteGeometryPoint>,
    private val policy:
        VexRouteMatchingPolicy =
        VexRouteMatchingPolicy(),
) {
    private val geometry =
        geometry.toList()
    private val cumulativeMeters =
        cumulativeGeometryMeters(
            this.geometry,
        )

    private var latestSampleElapsedRealtimeMillis:
        Long? = null

    init {
        require(this.geometry.size >= 2)
        require(
            cumulativeMeters.last() ==
                route.totalDistanceMeters,
        ) {
            "route and geometry must share one distance axis"
        }
    }

    fun match(
        sample: VexNavigationLocationSample,
        nowElapsedRealtimeMillis: Long,
    ): VexRouteMatchResult {
        require(
            nowElapsedRealtimeMillis >= 0L,
        )

        val lastObserved =
            latestSampleElapsedRealtimeMillis

        if (
            lastObserved != null &&
            sample.elapsedRealtimeMillis <=
            lastObserved
        ) {
            return VexRouteMatchResult.OutOfOrder
        }

        latestSampleElapsedRealtimeMillis =
            sample.elapsedRealtimeMillis

        val age =
            nowElapsedRealtimeMillis -
                sample.elapsedRealtimeMillis

        if (
            age < 0L ||
            age > policy.maximumFixAgeMillis
        ) {
            return VexRouteMatchResult.Stale
        }

        val accuracy =
            sample.accuracyMeters

        if (
            accuracy != null &&
            !policy.acceptsAccuracy(
                accuracy,
            )
        ) {
            return VexRouteMatchResult
                .Inaccurate(
                    accuracyMeters = accuracy,
                )
        }

        val projection =
            nearestRouteProjection(
                sample = sample,
                geometry = geometry,
                cumulativeMeters =
                    cumulativeMeters,
            )

        val offRouteThreshold =
            max(
                policy
                    .minimumOffRouteDistanceMeters,
                (
                    accuracy?.toDouble()
                        ?: 0.0
                ) *
                    policy
                        .accuracyOffRouteMultiplier,
            )

        if (
            projection.distanceFromRouteMeters >
            offRouteThreshold
        ) {
            return VexRouteMatchResult.OffRoute(
                distanceFromRouteMeters =
                    projection
                        .distanceFromRouteMeters,
                thresholdMeters =
                    offRouteThreshold,
            )
        }

        return VexRouteMatchResult.Matched(
            navigationFix =
                VexNavigationFix(
                    fixRef =
                        sample.sampleRef,
                    routeRef =
                        route.routeRef,
                    routeRevision =
                        route.routeRevision,
                    distanceAlongRouteMeters =
                        projection
                            .distanceAlongRouteMeters
                            .roundToInt()
                            .coerceIn(
                                0,
                                route
                                    .totalDistanceMeters,
                            ),
                ),
            distanceFromRouteMeters =
                projection
                    .distanceFromRouteMeters,
        )
    }
}

private data class RouteProjection(
    val distanceAlongRouteMeters: Double,
    val distanceFromRouteMeters: Double,
)

private fun nearestRouteProjection(
    sample: VexNavigationLocationSample,
    geometry: List<VexRouteGeometryPoint>,
    cumulativeMeters: List<Int>,
): RouteProjection {
    var best:
        RouteProjection? = null

    geometry
        .zipWithNext()
        .forEachIndexed {
            index,
            pair,
            ->
            val segment =
                projectToSegment(
                    sample = sample,
                    from = pair.first,
                    to = pair.second,
                )

            val segmentLengthMeters =
                (
                    cumulativeMeters[index + 1] -
                        cumulativeMeters[index]
                ).toDouble()

            val candidate =
                RouteProjection(
                    distanceAlongRouteMeters =
                        cumulativeMeters[index] +
                            segmentLengthMeters *
                            segment.fraction,
                    distanceFromRouteMeters =
                        segment.distanceMeters,
                )

            val currentBest =
                best

            if (
                currentBest == null ||
                candidate
                    .distanceFromRouteMeters <
                currentBest
                    .distanceFromRouteMeters
            ) {
                best = candidate
            }
        }

    return requireNotNull(best)
}

private data class SegmentProjection(
    val fraction: Double,
    val distanceMeters: Double,
)

private fun projectToSegment(
    sample: VexNavigationLocationSample,
    from: VexRouteGeometryPoint,
    to: VexRouteGeometryPoint,
): SegmentProjection {
    val latitudeScale =
        EARTH_RADIUS_METERS *
            Math.PI /
            180.0
    val longitudeScale =
        latitudeScale *
            cos(
                Math.toRadians(
                    sample.latitude,
                ),
            )

    fun local(
        point: VexRouteGeometryPoint,
    ): Pair<Double, Double> =
        (
            (
                point.longitude -
                    sample.longitude
            ) * longitudeScale
        ) to
            (
                (
                    point.latitude -
                        sample.latitude
                ) * latitudeScale
            )

    val (
        fromX,
        fromY,
    ) = local(from)
    val (
        toX,
        toY,
    ) = local(to)

    val segmentX =
        toX - fromX
    val segmentY =
        toY - fromY
    val segmentLengthSquared =
        segmentX * segmentX +
            segmentY * segmentY

    val fraction =
        if (segmentLengthSquared == 0.0) {
            0.0
        } else {
            (
                -(
                    fromX * segmentX +
                        fromY * segmentY
                ) /
                    segmentLengthSquared
            ).coerceIn(
                0.0,
                1.0,
            )
        }

    val nearestX =
        fromX +
            segmentX * fraction
    val nearestY =
        fromY +
            segmentY * fraction

    return SegmentProjection(
        fraction = fraction,
        distanceMeters =
            kotlin.math.hypot(
                nearestX,
                nearestY,
            ),
    )
}

private const val EARTH_RADIUS_METERS =
    6_371_008.8

// [VXG RealForever]
