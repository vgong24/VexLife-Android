package com.vextreme.vexlife.android.screens.shared.maps

import androidx.lifecycle.Lifecycle
import org.maplibre.android.maps.MapView

/**
 * Android lifecycle bridge for the native MapLibre MapView.
 *
 * VexMaps composition owns when this bridge exists; this adapter only mirrors Android lifecycle
 * events into the MapView and guarantees one terminal onDestroy.
 */
internal class VexMapLibreLifecycle(
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

// [VXG RealForever]
