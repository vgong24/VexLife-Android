package com.vextreme.vexlife.android.screens.shared.maps

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.sin
import kotlin.math.sqrt
import org.json.JSONObject

data class VexRouteGeometryPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(
            latitude.isFinite() &&
                latitude in -90.0..90.0,
        )
        require(
            longitude.isFinite() &&
                longitude in -180.0..180.0,
        )
    }
}

data class VexOfflineRouteResult(
    val route: VexNavigationRoute,
    val geometry: List<VexRouteGeometryPoint>,
    val travelMode: VexMapsTravelMode =
        VexMapsTravelMode.DRIVE,
    val estimatedDurationSeconds: Int? = null,
) {
    init {
        require(
            estimatedDurationSeconds == null ||
                estimatedDurationSeconds > 0,
        ) {
            "estimatedDurationSeconds must be positive when present"
        }
        require(geometry.size >= 2) {
            "route geometry must contain at least two points"
        }
    }
}

internal data class VexValhallaRouteProjection(
    val legs: List<VexValhallaLegProjection>,
    val estimatedDurationSeconds: Int? = null,
)

internal data class VexValhallaLegProjection(
    val geometry: List<VexRouteGeometryPoint>,
    val maneuvers: List<VexValhallaManeuverProjection>,
)

internal data class VexValhallaManeuverProjection(
    val instruction: String,
    val beginShapeIndex: Int,
)

/**
 * Converts Valhalla provider output into one route geometry axis used by both maneuver timing and
 * live location matching.
 *
 * Valhalla maneuver road length and decoded-polyline distance are different rulers on curves.
 * begin_shape_index places each maneuver directly on the same polyline axis as live map matching.
 */
internal object VexValhallaRouteTranslator {
    fun fromJson(
        rawJson: String,
    ): VexValhallaRouteProjection {
        val trip =
            JSONObject(rawJson)
                .getJSONObject("trip")
        val legsJson =
            trip.getJSONArray("legs")
        val estimatedDurationSeconds =
            projectValhallaDurationSeconds(
                trip
                    .optJSONObject("summary")
                    ?.optDouble(
                        "time",
                        Double.NaN,
                    ),
            )

        val legs =
            buildList {
                for (
                    legIndex in
                    0 until legsJson.length()
                ) {
                    val leg =
                        legsJson.getJSONObject(
                            legIndex,
                        )
                    val geometry =
                        decodePolyline6(
                            leg.getString("shape"),
                        )
                    val maneuversJson =
                        leg.getJSONArray(
                            "maneuvers",
                        )

                    val maneuvers =
                        buildList {
                            for (
                                maneuverIndex in
                                0 until maneuversJson.length()
                            ) {
                                val maneuver =
                                    maneuversJson
                                        .getJSONObject(
                                            maneuverIndex,
                                        )
                                val instruction =
                                    maneuver
                                        .optString(
                                            "instruction",
                                        )
                                        .trim()
                                val beginShapeIndex =
                                    maneuver.optInt(
                                        "begin_shape_index",
                                        -1,
                                    )

                                if (
                                    instruction.isNotEmpty() &&
                                    beginShapeIndex >= 0
                                ) {
                                    add(
                                        VexValhallaManeuverProjection(
                                            instruction =
                                                instruction,
                                            beginShapeIndex =
                                                beginShapeIndex,
                                        ),
                                    )
                                }
                            }
                        }

                    add(
                        VexValhallaLegProjection(
                            geometry = geometry,
                            maneuvers = maneuvers,
                        ),
                    )
                }
            }

        require(legs.isNotEmpty()) {
            "Valhalla response contains no route legs"
        }

        return VexValhallaRouteProjection(
            legs = legs,
            estimatedDurationSeconds =
                estimatedDurationSeconds,
        )
    }

    fun VexValhallaRouteProjection
        .toOfflineRouteResult(
            routeRef: String,
            routeRevision: Long,
            destinationLabel: String,
            travelMode: VexMapsTravelMode =
                VexMapsTravelMode.DRIVE,
        ): VexOfflineRouteResult {
        val combinedGeometry =
            mutableListOf<VexRouteGeometryPoint>()
        val maneuverCandidates =
            mutableListOf<Pair<Int, String>>()

        var distanceBeforeLegMeters = 0

        legs.forEachIndexed {
            legIndex,
            leg,
            ->
            require(leg.geometry.size >= 2) {
                "Valhalla leg $legIndex has insufficient geometry"
            }

            val legCumulativeMeters =
                cumulativeGeometryMeters(
                    leg.geometry,
                )

            leg.maneuvers.forEachIndexed {
                maneuverIndex,
                maneuver,
                ->
                require(
                    maneuver.beginShapeIndex in
                        leg.geometry.indices,
                ) {
                    "maneuver begin_shape_index is outside leg geometry"
                }

                val distance =
                    distanceBeforeLegMeters +
                        legCumulativeMeters[
                            maneuver.beginShapeIndex
                        ]

                val isRouteStart =
                    legIndex == 0 &&
                        maneuverIndex == 0 &&
                        distance == 0

                if (!isRouteStart) {
                    maneuverCandidates +=
                        distance to
                            maneuver.instruction
                }
            }

            if (combinedGeometry.isEmpty()) {
                combinedGeometry +=
                    leg.geometry
            } else {
                val startsAtPreviousEnd =
                    combinedGeometry.last() ==
                        leg.geometry.first()

                combinedGeometry +=
                    if (startsAtPreviousEnd) {
                        leg.geometry.drop(1)
                    } else {
                        leg.geometry
                    }
            }

            distanceBeforeLegMeters +=
                legCumulativeMeters.last()
        }

        val totalDistanceMeters =
            cumulativeGeometryMeters(
                combinedGeometry,
            ).last()

        require(totalDistanceMeters > 0) {
            "Valhalla route geometry has no positive distance"
        }

        val maneuvers =
            maneuverCandidates
                .asSequence()
                .filter { (distance, _) ->
                    distance > 0 &&
                        distance <=
                        totalDistanceMeters
                }
                .distinctBy { (distance, _) ->
                    distance
                }
                .mapIndexed {
                    index,
                    candidate,
                    ->
                    val (
                        distance,
                        instruction,
                    ) = candidate

                    VexNavigationManeuver(
                        maneuverRef =
                            "valhalla.maneuver.${index + 1}",
                        instructionProjection =
                            instruction,
                        distanceFromRouteStartMeters =
                            distance,
                    )
                }
                .toList()

        return VexOfflineRouteResult(
            route =
                VexNavigationRoute(
                    routeRef = routeRef,
                    routeRevision = routeRevision,
                    destinationLabel =
                        destinationLabel,
                    totalDistanceMeters =
                        totalDistanceMeters,
                    maneuvers = maneuvers,
                ),
            geometry =
                combinedGeometry.toList(),
            travelMode =
                travelMode,
            estimatedDurationSeconds =
                estimatedDurationSeconds,
        )
    }
}

internal fun decodePolyline6(
    encoded: String,
): List<VexRouteGeometryPoint> {
    require(encoded.isNotEmpty()) {
        "encoded polyline must not be empty"
    }

    val points =
        mutableListOf<VexRouteGeometryPoint>()

    var index = 0
    var latitude = 0
    var longitude = 0

    while (index < encoded.length) {
        val latitudeDelta =
            decodePolylineValue(
                encoded = encoded,
                startIndex = index,
            )
        index =
            latitudeDelta.nextIndex
        latitude +=
            latitudeDelta.value

        require(index < encoded.length) {
            "encoded polyline ended before longitude"
        }

        val longitudeDelta =
            decodePolylineValue(
                encoded = encoded,
                startIndex = index,
            )
        index =
            longitudeDelta.nextIndex
        longitude +=
            longitudeDelta.value

        points +=
            VexRouteGeometryPoint(
                latitude =
                    latitude / 1_000_000.0,
                longitude =
                    longitude / 1_000_000.0,
            )
    }

    require(points.size >= 2) {
        "decoded route polyline needs at least two points"
    }

    return points
}

private data class DecodedPolylineValue(
    val value: Int,
    val nextIndex: Int,
)

private fun decodePolylineValue(
    encoded: String,
    startIndex: Int,
): DecodedPolylineValue {
    var result = 0
    var shift = 0
    var index = startIndex

    while (true) {
        require(index < encoded.length) {
            "truncated encoded polyline"
        }

        val value =
            encoded[index++].code - 63

        require(value >= 0) {
            "invalid encoded polyline character"
        }

        result =
            result or
                ((value and 0x1f) shl shift)
        shift += 5

        require(shift <= 30) {
            "encoded polyline value is too large"
        }

        if (value < 0x20) {
            break
        }
    }

    val signed =
        if ((result and 1) != 0) {
            (result shr 1).inv()
        } else {
            result shr 1
        }

    return DecodedPolylineValue(
        value = signed,
        nextIndex = index,
    )
}

internal fun projectValhallaDurationSeconds(
    rawSeconds: Double?,
): Int? =
    rawSeconds
        ?.takeIf { seconds ->
            seconds.isFinite() &&
                seconds > 0.0
        }
        ?.let { seconds ->
            ceil(seconds)
                .toInt()
        }

internal fun cumulativeGeometryMeters(
    geometry: List<VexRouteGeometryPoint>,
): List<Int> {
    require(geometry.isNotEmpty())

    val cumulative =
        ArrayList<Int>(
            geometry.size,
        )
    var totalMeters = 0.0
    cumulative += 0

    geometry
        .zipWithNext()
        .forEach { (from, to) ->
            totalMeters +=
                haversineMeters(
                    from,
                    to,
                )
            cumulative +=
                totalMeters
                    .toInt()
                    .coerceAtLeast(
                        cumulative.last(),
                    )
        }

    return cumulative
}

private fun haversineMeters(
    from: VexRouteGeometryPoint,
    to: VexRouteGeometryPoint,
): Double {
    val fromLatitude =
        Math.toRadians(
            from.latitude,
        )
    val toLatitude =
        Math.toRadians(
            to.latitude,
        )
    val latitudeDelta =
        toLatitude - fromLatitude
    val longitudeDelta =
        Math.toRadians(
            to.longitude -
                from.longitude,
        )

    val a =
        sin(latitudeDelta / 2.0) *
            sin(latitudeDelta / 2.0) +
            cos(fromLatitude) *
            cos(toLatitude) *
            sin(longitudeDelta / 2.0) *
            sin(longitudeDelta / 2.0)

    return EARTH_RADIUS_METERS *
        2.0 *
        atan2(
            sqrt(a),
            sqrt(1.0 - a),
        )
}

private const val EARTH_RADIUS_METERS =
    6_371_008.8

// [VXG RealForever]
