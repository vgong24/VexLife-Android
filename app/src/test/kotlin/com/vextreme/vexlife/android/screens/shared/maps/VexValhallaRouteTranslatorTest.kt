package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexValhallaRouteTranslatorTest {
    @Test
    fun maneuverUsesPolylineShapeIndexInsteadOfProviderRoadLength() {
        val geometry =
            listOf(
                VexRouteGeometryPoint(
                    34.0,
                    -118.0,
                ),
                VexRouteGeometryPoint(
                    34.0,
                    -117.999,
                ),
                VexRouteGeometryPoint(
                    34.001,
                    -117.999,
                ),
                VexRouteGeometryPoint(
                    34.001,
                    -117.998,
                ),
            )

        val providerRoute =
            VexValhallaRouteProjection(
                legs =
                    listOf(
                        VexValhallaLegProjection(
                            geometry = geometry,
                            maneuvers =
                                listOf(
                                    VexValhallaManeuverProjection(
                                        instruction =
                                            "Start east",
                                        beginShapeIndex = 0,
                                    ),
                                    VexValhallaManeuverProjection(
                                        instruction =
                                            "Turn left",
                                        beginShapeIndex = 1,
                                    ),
                                    VexValhallaManeuverProjection(
                                        instruction =
                                            "Turn right",
                                        beginShapeIndex = 2,
                                    ),
                                ),
                        ),
                    ),
            )

        val result =
            with(VexValhallaRouteTranslator) {
                providerRoute
                    .toOfflineRouteResult(
                        routeRef = "route.test",
                        routeRevision = 4L,
                        destinationLabel = "Test",
                    )
            }

        val geometryAxis =
            cumulativeGeometryMeters(
                geometry,
            )

        assertEquals(
            geometryAxis.last(),
            result.route.totalDistanceMeters,
        )
        assertEquals(
            geometryAxis[1],
            result.route
                .maneuvers[0]
                .distanceFromRouteStartMeters,
        )
        assertEquals(
            geometryAxis[2],
            result.route
                .maneuvers[1]
                .distanceFromRouteStartMeters,
        )
        assertEquals(
            geometry,
            result.geometry,
        )
    }

    @Test
    fun duplicateTransitionDistanceDoesNotBreakStrictRouteOrdering() {
        val pointA =
            VexRouteGeometryPoint(
                34.0,
                -118.0,
            )
        val pointB =
            VexRouteGeometryPoint(
                34.0,
                -117.999,
            )
        val pointC =
            VexRouteGeometryPoint(
                34.001,
                -117.999,
            )

        val providerRoute =
            VexValhallaRouteProjection(
                legs =
                    listOf(
                        VexValhallaLegProjection(
                            geometry =
                                listOf(
                                    pointA,
                                    pointB,
                                    pointC,
                                ),
                            maneuvers =
                                listOf(
                                    VexValhallaManeuverProjection(
                                        "Start",
                                        0,
                                    ),
                                    VexValhallaManeuverProjection(
                                        "Turn first",
                                        1,
                                    ),
                                    VexValhallaManeuverProjection(
                                        "Duplicate transition",
                                        1,
                                    ),
                                ),
                        ),
                    ),
            )

        val route =
            with(VexValhallaRouteTranslator) {
                providerRoute
                    .toOfflineRouteResult(
                        routeRef = "route.test",
                        routeRevision = 1L,
                        destinationLabel = "Test",
                    )
                    .route
            }

        assertEquals(
            1,
            route.maneuvers.size,
        )
    }

    @Test
    fun polyline6DecoderPreservesSmallCoordinateDeltas() {
        val expected =
            listOf(
                VexRouteGeometryPoint(
                    34.000001,
                    -118.000001,
                ),
                VexRouteGeometryPoint(
                    34.000101,
                    -117.999901,
                ),
            )

        val encoded =
            encodePolyline6ForTest(
                expected,
            )

        assertEquals(
            expected,
            decodePolyline6(
                encoded,
            ),
        )
    }

    @Test
    fun geometryDistanceIsMonotonic() {
        val distances =
            cumulativeGeometryMeters(
                listOf(
                    VexRouteGeometryPoint(
                        34.0,
                        -118.0,
                    ),
                    VexRouteGeometryPoint(
                        34.001,
                        -118.0,
                    ),
                    VexRouteGeometryPoint(
                        34.001,
                        -117.999,
                    ),
                ),
            )

        assertEquals(0, distances.first())
        assertTrue(
            distances.zipWithNext().all {
                (a, b) -> b > a
            },
        )
    }
}

private fun encodePolyline6ForTest(
    points: List<VexRouteGeometryPoint>,
): String {
    var previousLatitude = 0
    var previousLongitude = 0

    return buildString {
        points.forEach { point ->
            val latitude =
                (point.latitude *
                    1_000_000.0)
                    .toInt()
            val longitude =
                (point.longitude *
                    1_000_000.0)
                    .toInt()

            appendEncodedValue(
                latitude -
                    previousLatitude,
            )
            appendEncodedValue(
                longitude -
                    previousLongitude,
            )

            previousLatitude =
                latitude
            previousLongitude =
                longitude
        }
    }
}

private fun StringBuilder.appendEncodedValue(
    value: Int,
) {
    var encoded =
        if (value < 0) {
            (value shl 1).inv()
        } else {
            value shl 1
        }

    while (encoded >= 0x20) {
        append(
            ((0x20 or
                (encoded and 0x1f)) +
                63).toChar(),
        )
        encoded =
            encoded shr 5
    }

    append(
        (encoded + 63)
            .toChar(),
    )
}

// [VXG RealForever]
