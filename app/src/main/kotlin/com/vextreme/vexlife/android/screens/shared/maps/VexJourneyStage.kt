package com.vextreme.vexlife.android.screens.shared.maps

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vextreme.vexlife.android.ui.VexThemePresentation
import com.vextreme.vexlife.android.ui.currentVexThemePresentation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** A route-matched place on the route and the direction of travel there (degrees clockwise from north). */
internal data class VexRoutePosition(
    val latitude: Double,
    val longitude: Double,
    val bearingDegrees: Double,
)

/**
 * Pure: where [distanceAlongMeters] falls on [geometry], interpolated within its segment, facing
 * along that segment. Clamped to the route's ends. Zero-length segments borrow the next real one's
 * heading so the camera never spins on a duplicated vertex.
 */
internal fun vexRoutePosition(
    geometry: List<VexRouteGeometryPoint>,
    distanceAlongMeters: Int,
): VexRoutePosition {
    require(geometry.size >= 2) { "route geometry must contain at least two points" }
    val cumulative = cumulativeGeometryMeters(geometry)
    val along = distanceAlongMeters.coerceIn(0, cumulative.last())
    val segment =
        (0 until geometry.lastIndex)
            .firstOrNull { index -> along <= cumulative[index + 1] }
            ?: (geometry.lastIndex - 1)
    val from = geometry[segment]
    val to = geometry[segment + 1]
    val length = cumulative[segment + 1] - cumulative[segment]
    val fraction =
        if (length <= 0) 0.0 else (along - cumulative[segment]).toDouble() / length
    val headingSegment =
        (segment until geometry.lastIndex)
            .firstOrNull { index -> cumulative[index + 1] > cumulative[index] }
            ?: segment

    return VexRoutePosition(
        latitude = from.latitude + (to.latitude - from.latitude) * fraction,
        longitude = from.longitude + (to.longitude - from.longitude) * fraction,
        bearingDegrees =
            vexInitialBearingDegrees(
                geometry[headingSegment],
                geometry[headingSegment + 1],
            ),
    )
}

internal fun vexInitialBearingDegrees(
    from: VexRouteGeometryPoint,
    to: VexRouteGeometryPoint,
): Double {
    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
    val y = sin(longitudeDelta) * cos(toLatitude)
    val x =
        cos(fromLatitude) * sin(toLatitude) -
            sin(fromLatitude) * cos(toLatitude) * cos(longitudeDelta)
    return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
}

/**
 * What the journey stage draws: the one current route and how far along it the traveller is.
 *
 * A read-only projection of [VexNavigationSession]; it never starts, stops or reroutes.
 */
internal data class VexJourneyStage(
    val routeKey: String,
    val geometry: List<VexRouteGeometryPoint>,
    val distanceAlongMeters: Int,
) {
    val position: VexRoutePosition = vexRoutePosition(geometry, distanceAlongMeters)
}

internal fun VexNavigationSessionState.toVexJourneyStage(): VexJourneyStage? {
    if (phase == VexNavigationSessionPhase.IDLE) {
        return null
    }
    val route = routeResult ?: return null
    return VexJourneyStage(
        routeKey = route.vexMapsRouteKey(),
        geometry = route.geometry,
        distanceAlongMeters = convergence?.progress?.distanceAlongRouteMeters ?: 0,
    )
}

@Composable
internal fun rememberVexJourneyStage(): VexJourneyStage? {
    val state by VexMapsNavigationProcess.session.states.collectAsState()
    return remember(state) { state.toVexJourneyStage() }
}

/**
 * The Map stage's base layer: the road ahead, heading-up, following the route-matched position.
 *
 * Device: a live MapLibre map (OpenFreeMap style matching the app's light/dark) with the route,
 * the travelled part dimmed, and the traveller's puck. Gestures are off; the stage is a glance and
 * the full map tool is VexMaps. Preview / Layoutlib, or before the style loads: a heading-up
 * schematic of the same route, so the stage is never blank, including offline.
 */
@Composable
internal fun VexJourneyStageMap(
    stage: VexJourneyStage,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        VexJourneyStageSchematic(
            stage = stage,
            modifier = modifier,
        )
        return
    }

    var styleLoaded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        if (!styleLoaded) {
            VexJourneyStageSchematic(
                stage = stage,
                modifier = Modifier.fillMaxSize(),
            )
        }
        VexJourneyStageLiveMap(
            stage = stage,
            onStyleLoaded = { styleLoaded = true },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun VexJourneyStageLiveMap(
    stage: VexJourneyStage,
    onStyleLoaded: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val presentation = currentVexThemePresentation()
    val styleUri =
        when (presentation) {
            VexThemePresentation.Light -> VexMapsOnlineStyle.LIBERTY.styleUri
            VexThemePresentation.Dark,
            VexThemePresentation.Unresolved,
            -> VexMapsOnlineStyle.DARK.styleUri
        }
    val routeColor = MaterialTheme.colorScheme.primary.toArgb()
    val puckColor = MaterialTheme.colorScheme.tertiary.toArgb()
    val renderer = remember { VexJourneyStageRenderer() }
    val mapView =
        remember(context) {
            MapLibre.getInstance(context.applicationContext)
            MapView(context).apply {
                onCreate(null)
                getMapAsync { map ->
                    map.uiSettings.setAllGesturesEnabled(false)
                    map.uiSettings.isCompassEnabled = false
                }
            }
        }

    DisposableEffect(lifecycle, mapView) {
        val bridge = VexMapLibreLifecycle(mapView = mapView)
        bridge.synchronize(lifecycle.currentState)
        val observer = LifecycleEventObserver { _, event -> bridge.onEvent(event) }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            renderer.detach()
            bridge.dispose()
        }
    }

    LaunchedEffect(mapView, styleUri) {
        mapView.getMapAsync { map ->
            map.setStyle(styleUri) { style ->
                renderer.attach(map, style, routeColor, puckColor)
                renderer.show(stage, animate = false)
                onStyleLoaded()
            }
        }
    }

    LaunchedEffect(stage) {
        renderer.show(stage, animate = true)
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

/** MapLibre mechanics for the journey stage only. Owns no route meaning. */
private class VexJourneyStageRenderer {
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var shownRouteKey: String? = null

    fun attach(
        map: MapLibreMap,
        style: Style,
        routeColor: Int,
        puckColor: Int,
    ) {
        this.map = map
        this.style = style
        shownRouteKey = null
        style.addSource(GeoJsonSource(ROUTE_SOURCE))
        style.addSource(GeoJsonSource(TRAVELLED_SOURCE))
        style.addSource(GeoJsonSource(PUCK_SOURCE))
        style.addLayer(
            LineLayer(ROUTE_CASING_LAYER, ROUTE_SOURCE).withProperties(
                lineColor(AndroidColor.argb(110, 0, 0, 0)),
                lineWidth(12f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        style.addLayer(
            LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                lineColor(routeColor),
                lineWidth(7.5f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        style.addLayer(
            LineLayer(TRAVELLED_LAYER, TRAVELLED_SOURCE).withProperties(
                lineColor(AndroidColor.argb(170, 128, 128, 128)),
                lineWidth(7.5f),
                lineOpacity(0.85f),
                lineCap(Property.LINE_CAP_ROUND),
                lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        style.addLayer(
            CircleLayer(PUCK_LAYER, PUCK_SOURCE).withProperties(
                circleRadius(9f),
                circleColor(puckColor),
                circleStrokeWidth(3.5f),
                circleStrokeColor(AndroidColor.WHITE),
            ),
        )
    }

    fun detach() {
        map = null
        style = null
        shownRouteKey = null
    }

    fun show(
        stage: VexJourneyStage,
        animate: Boolean,
    ) {
        val map = map ?: return
        val style = style ?: return
        val position = stage.position

        if (shownRouteKey != stage.routeKey) {
            (style.getSource(ROUTE_SOURCE) as? GeoJsonSource)
                ?.setGeoJson(LineString.fromLngLats(stage.geometry.map(::point)))
            shownRouteKey = stage.routeKey
        }
        (style.getSource(TRAVELLED_SOURCE) as? GeoJsonSource)
            ?.setGeoJson(LineString.fromLngLats(travelled(stage).map(::point)))
        (style.getSource(PUCK_SOURCE) as? GeoJsonSource)
            ?.setGeoJson(Point.fromLngLat(position.longitude, position.latitude))

        // Look a little ahead so the traveller sits in the lower part of the view, road in front.
        val ahead = vexRoutePosition(stage.geometry, stage.distanceAlongMeters + LOOK_AHEAD_METERS)
        val camera =
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(ahead.latitude, ahead.longitude))
                    .bearing(position.bearingDegrees)
                    .tilt(48.0)
                    .zoom(16.0)
                    .build(),
            )
        if (animate) map.animateCamera(camera, 900) else map.moveCamera(camera)
    }

    private fun travelled(
        stage: VexJourneyStage,
    ): List<VexRouteGeometryPoint> {
        val cumulative = cumulativeGeometryMeters(stage.geometry)
        val passed = stage.geometry.filterIndexed { index, _ -> cumulative[index] < stage.distanceAlongMeters }
        val here = VexRouteGeometryPoint(stage.position.latitude, stage.position.longitude)
        return (passed + here).let { points -> if (points.size < 2) listOf(here, here) else points }
    }

    private fun point(
        coordinate: VexRouteGeometryPoint,
    ): Point = Point.fromLngLat(coordinate.longitude, coordinate.latitude)

    companion object {
        const val ROUTE_SOURCE = "vex-stage-route"
        const val TRAVELLED_SOURCE = "vex-stage-travelled"
        const val PUCK_SOURCE = "vex-stage-puck"
        const val ROUTE_CASING_LAYER = "vex-stage-route-casing"
        const val ROUTE_LAYER = "vex-stage-route-line"
        const val TRAVELLED_LAYER = "vex-stage-travelled-line"
        const val PUCK_LAYER = "vex-stage-puck-circle"
        const val LOOK_AHEAD_METERS = 140
    }
}

/**
 * Heading-up schematic of the same route: no tiles, so it renders in previews and offline.
 *
 * The traveller sits low and centred facing up; the route ahead is bright, the travelled part
 * faint; a soft street lattice gives the eye a sense of motion and scale.
 */
@Composable
internal fun VexJourneyStageSchematic(
    stage: VexJourneyStage,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val base = colors.surfaceContainerLowest
    val lattice = colors.outlineVariant.copy(alpha = 0.22f)
    val majorLattice = colors.outlineVariant.copy(alpha = 0.36f)
    val routeColor = colors.primary
    val casing = colors.scrim.copy(alpha = 0.45f)
    val travelledColor = colors.onSurfaceVariant.copy(alpha = 0.35f)
    val puckColor = colors.tertiary
    val destinationColor = colors.secondary

    Canvas(
        modifier =
            modifier.background(
                Brush.verticalGradient(
                    listOf(base, colors.surfaceContainerLow, base),
                ),
            ),
    ) {
        val anchor = Offset(size.width / 2f, size.height * 0.66f)
        val pxPerMeter = size.height * 0.62f / SCHEMATIC_METERS_AHEAD
        val origin = stage.position
        val cosLatitude = cos(Math.toRadians(origin.latitude))

        fun project(point: VexRouteGeometryPoint): Offset {
            val east = (point.longitude - origin.longitude) * METERS_PER_DEGREE * cosLatitude
            val north = (point.latitude - origin.latitude) * METERS_PER_DEGREE
            return Offset((east * pxPerMeter).toFloat(), (-north * pxPerMeter).toFloat())
        }

        val cumulative = cumulativeGeometryMeters(stage.geometry)
        val here = VexRouteGeometryPoint(origin.latitude, origin.longitude)
        val passed =
            stage.geometry.filterIndexed { index, _ -> cumulative[index] < stage.distanceAlongMeters } + here
        val ahead =
            listOf(here) +
                stage.geometry.filterIndexed { index, _ -> cumulative[index] > stage.distanceAlongMeters }

        translate(anchor.x, anchor.y) {
            rotate(degrees = -origin.bearingDegrees.toFloat(), pivot = Offset.Zero) {
                drawLattice(lattice, majorLattice, pxPerMeter)
                drawRoute(passed.map(::project), travelledColor, null, 7.dpPx(this))
                drawRoute(ahead.map(::project), routeColor, casing, 8.dpPx(this))
                val destination = project(stage.geometry.last())
                drawCircle(casing, radius = 12.dpPx(this), center = destination)
                drawCircle(destinationColor, radius = 9.dpPx(this), center = destination)
                drawCircle(base, radius = 3.5f.dpPx(this), center = destination)
            }
            drawPuck(puckColor, base)
        }
    }
}

private fun Int.dpPx(scope: DrawScope): Float = with(scope) { this@dpPx.toFloat() * density }

private fun Float.dpPx(scope: DrawScope): Float = with(scope) { this@dpPx * density }

private fun DrawScope.drawRoute(
    points: List<Offset>,
    color: Color,
    casing: Color?,
    width: Float,
) {
    if (points.size < 2) return
    val path =
        Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
    casing?.let {
        drawPath(path, it, style = Stroke(width = width * 1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawLattice(
    minor: Color,
    major: Color,
    pxPerMeter: Float,
) {
    val step = 120f * pxPerMeter
    val reach = size.maxDimension * 1.6f
    var offset = -reach
    var index = 0
    while (offset <= reach) {
        val color = if (index % 4 == 0) major else minor
        val width = if (index % 4 == 0) 2.2f.dpPx(this) else 1.1f.dpPx(this)
        drawLine(color, Offset(offset, -reach), Offset(offset, reach), strokeWidth = width)
        drawLine(color, Offset(-reach, offset), Offset(reach, offset), strokeWidth = width)
        offset += step
        index++
    }
}

private fun DrawScope.drawPuck(
    color: Color,
    base: Color,
) {
    val r = 15.dpPx(this)
    drawCircle(color.copy(alpha = 0.22f), radius = r * 2.1f, center = Offset.Zero)
    val chevron =
        Path().apply {
            moveTo(0f, -r)
            lineTo(r * 0.82f, r * 0.85f)
            lineTo(0f, r * 0.42f)
            lineTo(-r * 0.82f, r * 0.85f)
            close()
        }
    drawPath(chevron, base, style = Stroke(width = 5.dpPx(this), join = StrokeJoin.Round))
    drawPath(chevron, color)
}

private const val SCHEMATIC_METERS_AHEAD = 1_400f
private const val METERS_PER_DEGREE = 111_320.0

// [VXG RealForever][VEXMAPS][JOURNEY-STAGE]
