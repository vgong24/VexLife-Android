package com.vextreme.vexlife.android.screens.shared.maps

import android.content.Context
import com.valhalla.valhalla.Valhalla
import com.valhalla.valhalla.config.ValhallaConfigFactory
import java.io.Closeable
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * One offline routing request expressed in VexMaps terms.
 *
 * Coordinates are inputs to the routing adapter. The returned route is immediately translated into
 * the provider-independent [VexNavigationRoute] owned by VM-02.
 */
data class VexOfflineRouteRequest(
    val routeRef: String,
    val routeRevision: Long,
    val origin: VexMapCoordinate,
    val destination: VexMapCoordinate,
    val destinationLabel: String,
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
    }
}

data class VexMapCoordinate(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "latitude must be finite and in [-90, 90]"
        }
        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "longitude must be finite and in [-180, 180]"
        }
    }
}

/**
 * Offline Valhalla adapter backed by a tile archive already present on the device.
 *
 * The archive is an input/custody concern, not a cloud API. This class never downloads routing data,
 * uses an API key, or sends origin/destination coordinates to a routing service.
 *
 * One Valhalla native actor mmaps the tile archive and is intentionally reused for this provider's
 * lifetime. Call [close] when the owning Maps session/region pack is released.
 */
class VexValhallaOfflineRouteProvider(
    context: Context,
    private val tileExtract: File,
) : Closeable {
    private val appContext =
        context.applicationContext

    private val engine: Valhalla

    init {
        require(
            tileExtract.isFile &&
                tileExtract.canRead(),
        ) {
            "Valhalla region tile archive is not readable: ${tileExtract.absolutePath}"
        }

        val config =
            ValhallaConfigFactory.usingTileExtract(
                tileExtract.absolutePath,
            )

        engine =
            Valhalla(
                appContext,
                config,
            )
    }

    suspend fun route(
        request: VexOfflineRouteRequest,
    ): VexOfflineRouteResult =
        withContext(Dispatchers.IO) {
            val rawResponse =
                engine.routeRaw(
                    request.toValhallaJson(),
                )

            VexValhallaRouteTranslator
                .fromJson(rawResponse)
                .toOfflineRouteResult(
                    routeRef = request.routeRef,
                    routeRevision =
                        request.routeRevision,
                    destinationLabel =
                        request.destinationLabel,
                )
        }

    override fun close() {
        engine.close()
    }
}

private fun VexOfflineRouteRequest.toValhallaJson():
    String {
    val locations =
        JSONArray()
            .put(origin.toValhallaWaypoint())
            .put(destination.toValhallaWaypoint())

    return JSONObject()
        .put("locations", locations)
        .put("costing", "auto")
        .put("units", "kilometers")
        .put("directions_type", "instructions")
        .put("format", "json")
        .put("shape_format", "polyline6")
        .toString()
}

private fun VexMapCoordinate.toValhallaWaypoint():
    JSONObject =
    JSONObject()
        .put("lat", latitude)
        .put("lon", longitude)

// [VXG RealForever]
