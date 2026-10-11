package com.vextreme.vexlife.android.screens.shared.maps

import kotlin.math.roundToInt
import org.json.JSONObject

/**
 * Narrow provider projection used only while translating Valhalla output.
 *
 * Provider JSON stops here. VexMaps route/navigation consumers receive [VexNavigationRoute].
 */
internal data class VexValhallaRouteProjection(
    val totalDistanceMeters: Int,
    val segments: List<VexValhallaManeuverSegment>,
)

internal data class VexValhallaManeuverSegment(
    val instruction: String,
    val lengthMeters: Int,
)

/**
 * Converts one Valhalla response into the stable VM-02 route contract.
 *
 * Valhalla maneuvers describe the segment that starts at a maneuver. The instruction for segment N
 * therefore becomes eligible at the distance reached after segments 0..N-1. The first segment is
 * the route start itself, so only later segments become Vex navigation maneuvers.
 */
internal object VexValhallaRouteTranslator {
    fun fromJson(
        rawJson: String,
    ): VexValhallaRouteProjection {
        val root =
            JSONObject(rawJson)
        val trip =
            root.getJSONObject("trip")
        val legs =
            trip.getJSONArray("legs")

        val segments =
            buildList {
                for (
                    legIndex in
                    0 until legs.length()
                ) {
                    val maneuvers =
                        legs
                            .getJSONObject(legIndex)
                            .getJSONArray("maneuvers")

                    for (
                        maneuverIndex in
                        0 until maneuvers.length()
                    ) {
                        val maneuver =
                            maneuvers.getJSONObject(
                                maneuverIndex,
                            )
                        val instruction =
                            maneuver
                                .optString(
                                    "instruction",
                                )
                                .trim()
                        val lengthMeters =
                            maneuver
                                .optDouble(
                                    "length",
                                    0.0,
                                )
                                .kilometersToMeters()

                        if (
                            instruction.isNotEmpty()
                        ) {
                            add(
                                VexValhallaManeuverSegment(
                                    instruction =
                                        instruction,
                                    lengthMeters =
                                        lengthMeters,
                                ),
                            )
                        }
                    }
                }
            }

        val summaryKilometers =
            trip
                .optJSONObject("summary")
                ?.optDouble(
                    "length",
                    Double.NaN,
                )
                ?: Double.NaN

        val derivedDistanceMeters =
            segments.sumOf { segment ->
                segment.lengthMeters
            }

        val totalDistanceMeters =
            if (
                summaryKilometers.isFinite() &&
                summaryKilometers > 0.0
            ) {
                summaryKilometers
                    .kilometersToMeters()
            } else {
                derivedDistanceMeters
            }

        require(totalDistanceMeters > 0) {
            "Valhalla response has no positive route distance"
        }

        return VexValhallaRouteProjection(
            totalDistanceMeters =
                totalDistanceMeters,
            segments = segments,
        )
    }

    fun VexValhallaRouteProjection
        .toNavigationRoute(
            routeRef: String,
            routeRevision: Long,
            destinationLabel: String,
        ): VexNavigationRoute {
        var distanceBeforeSegment = 0

        val maneuvers =
            segments
                .mapIndexedNotNull {
                    index,
                    segment,
                    ->
                    val transitionDistance =
                        distanceBeforeSegment
                            .coerceAtMost(
                                totalDistanceMeters,
                            )

                    distanceBeforeSegment =
                        (
                            distanceBeforeSegment +
                                segment.lengthMeters
                        ).coerceAtMost(
                            totalDistanceMeters,
                        )

                    if (
                        index == 0 ||
                        transitionDistance <= 0
                    ) {
                        null
                    } else {
                        VexNavigationManeuver(
                            maneuverRef =
                                "valhalla.maneuver.$index",
                            instructionProjection =
                                segment.instruction,
                            distanceFromRouteStartMeters =
                                transitionDistance,
                        )
                    }
                }
                .distinctBy {
                    it.distanceFromRouteStartMeters
                }

        return VexNavigationRoute(
            routeRef = routeRef,
            routeRevision = routeRevision,
            destinationLabel =
                destinationLabel,
            totalDistanceMeters =
                totalDistanceMeters,
            maneuvers = maneuvers,
        )
    }
}

private fun Double.kilometersToMeters(): Int =
    (this * 1_000.0)
        .roundToInt()
        .coerceAtLeast(0)

// [VXG RealForever]
