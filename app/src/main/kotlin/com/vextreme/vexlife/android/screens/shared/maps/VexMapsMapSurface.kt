package com.vextreme.vexlife.android.screens.shared.maps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vextreme.vexlife.android.R
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.CameraPosition
import org.maplibre.android.maps.MapView

/**
 * Android-native realization of the VexMaps basemap.
 *
 * MapLibre owns rendering only. OpenFreeMap is the current zero-key online OSM tile/style source.
 * Neither becomes the owner of VexMaps route, navigation, location-permission, or guidance meaning.
 *
 * A later local/self-hosted tile source can replace [onlineStyleUri] without changing VM-02 route
 * semantics or the registered VexMaps destination.
 */
@Composable
internal fun VexMapsMapSurface(
    darkTheme: Boolean,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onlineStyleUri =
        if (darkTheme) {
            OPEN_FREE_MAP_DARK_STYLE
        } else {
            OPEN_FREE_MAP_LIBERTY_STYLE
        }

    val mapView =
        remember(context, onlineStyleUri) {
            MapLibre.getInstance(
                context.applicationContext,
            )

            MapView(context).apply {
                onCreate(null)
                getMapAsync { map ->
                    map.setStyle(onlineStyleUri)
                    map.cameraPosition =
                        CameraPosition.Builder()
                            .target(
                                LatLng(
                                    0.0,
                                    0.0,
                                ),
                            )
                            .zoom(1.25)
                            .build()
                }
            }
        }

    DisposableEffect(
        lifecycle,
        mapView,
    ) {
        val lifecycleBridge =
            VexMapsMapViewLifecycle(
                mapView = mapView,
            )

        lifecycleBridge.synchronize(
            lifecycle.currentState,
        )

        val observer =
            LifecycleEventObserver { _, event ->
                lifecycleBridge.onEvent(event)
            }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            lifecycleBridge.dispose()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
        )

        Text(
            text = stringResource(
                R.string.vexmaps_attribution,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme
                            .surface
                            .copy(alpha = 0.82f),
                    )
                    .padding(
                        horizontal = 6.dp,
                        vertical = 3.dp,
                    ),
        )
    }
}

/**
 * Keeps MapView's imperative Android lifecycle behind the screen-local realization boundary.
 *
 * The VexLife semantic session remains owned by VexLifeSession/VexRuntime. Destroying this MapView
 * when VexMaps leaves composition must not imply the VexLife app session restarted.
 */
private class VexMapsMapViewLifecycle(
    private val mapView: MapView,
) {
    private var started = false
    private var resumed = false
    private var disposed = false

    fun synchronize(
        state: Lifecycle.State,
    ) {
        if (
            state.isAtLeast(
                Lifecycle.State.STARTED,
            )
        ) {
            start()
        }

        if (
            state.isAtLeast(
                Lifecycle.State.RESUMED,
            )
        ) {
            resume()
        }
    }

    fun onEvent(
        event: Lifecycle.Event,
    ) {
        when (event) {
            Lifecycle.Event.ON_START ->
                start()

            Lifecycle.Event.ON_RESUME ->
                resume()

            Lifecycle.Event.ON_PAUSE ->
                pause()

            Lifecycle.Event.ON_STOP ->
                stop()

            Lifecycle.Event.ON_DESTROY -> {
                pause()
                stop()
            }

            Lifecycle.Event.ON_CREATE,
            Lifecycle.Event.ON_ANY ->
                Unit
        }
    }

    fun dispose() {
        if (disposed) {
            return
        }

        pause()
        stop()
        mapView.onDestroy()
        disposed = true
    }

    private fun start() {
        if (
            disposed ||
            started
        ) {
            return
        }

        mapView.onStart()
        started = true
    }

    private fun resume() {
        if (
            disposed ||
            resumed
        ) {
            return
        }

        if (!started) {
            start()
        }

        mapView.onResume()
        resumed = true
    }

    private fun pause() {
        if (
            disposed ||
            !resumed
        ) {
            return
        }

        mapView.onPause()
        resumed = false
    }

    private fun stop() {
        if (
            disposed ||
            !started
        ) {
            return
        }

        pause()
        mapView.onStop()
        started = false
    }
}

private const val OPEN_FREE_MAP_LIBERTY_STYLE =
    "https://tiles.openfreemap.org/styles/liberty"

private const val OPEN_FREE_MAP_DARK_STYLE =
    "https://tiles.openfreemap.org/styles/dark"

// [VXG RealForever]
