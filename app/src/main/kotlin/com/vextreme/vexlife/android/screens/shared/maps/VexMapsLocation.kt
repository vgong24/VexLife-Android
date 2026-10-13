package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.R

/**
 * VexMaps location projection/readiness meaning.
 *
 * Android LocationManager remains in VexDeviceLocationSource. This file answers how observed location
 * results project into Maps and navigation readiness/presentation state.
 */
internal fun projectVexMapsDeviceLocation(
    result: VexDeviceLocationResult,
): VexDeviceLocationResult.Located? =
    result as? VexDeviceLocationResult.Located

internal fun projectVexMapsMapDeviceLocation(
    plannerLocation:
        VexDeviceLocationResult.Located?,
    navigationVisualLocation:
        VexMapsNavigationVisualLocation?,
): VexDeviceLocationResult.Located? =
    navigationVisualLocation
        ?.let { visual ->
            VexDeviceLocationResult.Located(
                latitude =
                    visual.coordinate.latitude,
                longitude =
                    visual.coordinate.longitude,
                accuracyMeters =
                    visual.accuracyMeters,
            )
        }
        ?: plannerLocation

internal fun isVexMapsNavigationStartPreflightSatisfied(
    result: VexDeviceLocationResult,
    matchingPolicy:
        VexRouteMatchingPolicy =
        VexRouteMatchingPolicy(),
): Boolean =
    result is VexDeviceLocationResult.Located &&
        matchingPolicy.acceptsAccuracy(
            result.accuracyMeters,
        )

internal fun canStartVexMapsNavigation(
    precisePermissionGranted: Boolean,
    hasCurrentLocation: Boolean,
    locationFixReady: Boolean,
): Boolean =
    precisePermissionGranted &&
        hasCurrentLocation &&
        locationFixReady

internal fun canContinueVexMapsNavigationStart(
    expectedRouteKey: String,
    currentPhase: VexNavigationSessionPhase,
    currentRouteKey: String?,
): Boolean =
    currentPhase ==
        VexNavigationSessionPhase.READY &&
        currentRouteKey ==
        expectedRouteKey

internal sealed interface VexMapsLocationState {
    val stringResOrNull: Int?

    data object Idle :
        VexMapsLocationState {
        override val stringResOrNull:
            Int? = null
    }

    data object Finding :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_finding
    }

    data object Ready :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_ready
    }

    data object Denied :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_denied
    }

    data object Approximate :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_approximate
    }

    data object Disabled :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_disabled
    }

    data object Unavailable :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_unavailable
    }

    data object Inaccurate :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_inaccurate
    }

    data object Error :
        VexMapsLocationState {
        override val stringResOrNull =
            R.string.vexmaps_location_error
    }
}

// [VXG RealForever]
