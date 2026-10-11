package com.vextreme.vexlife.android.screens.shared.maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android-only, session-scoped device location seam for VexMaps.
 *
 * This does not persist coordinates, request permissions, route, or own navigation meaning. The
 * system Location Button grants access first; this source then asks Android for one current fix.
 */
internal class VexDeviceLocationSource(
    context: Context,
) {
    private val appContext =
        context.applicationContext

    private val locationManager =
        requireNotNull(
            appContext.getSystemService(
                LocationManager::class.java,
            ),
        ) {
            "Android LocationManager is unavailable"
        }

    suspend fun currentPreciseLocation():
        VexDeviceLocationResult {
        if (
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return VexDeviceLocationResult.PermissionMissing
        }

        if (
            !LocationManagerCompat.isLocationEnabled(
                locationManager,
            )
        ) {
            return VexDeviceLocationResult.LocationDisabled
        }

        val provider =
            preferredProvider()
                ?: return VexDeviceLocationResult.Unavailable

        val cancellationSignal =
            CancellationSignal()

        return try {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    cancellationSignal.cancel()
                }

                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    cancellationSignal,
                    ContextCompat.getMainExecutor(
                        appContext,
                    ),
                ) { location ->
                    if (!continuation.isActive) {
                        return@getCurrentLocation
                    }

                    continuation.resume(
                        location?.let { fix ->
                            VexDeviceLocationResult.Located(
                                latitude = fix.latitude,
                                longitude = fix.longitude,
                                accuracyMeters =
                                    fix.accuracy
                                        .takeIf { accuracy ->
                                            accuracy.isFinite() &&
                                                accuracy >= 0f
                                        },
                            )
                        } ?: VexDeviceLocationResult.Unavailable,
                    )
                }
            }
        } catch (_: SecurityException) {
            VexDeviceLocationResult.PermissionMissing
        } catch (_: IllegalArgumentException) {
            VexDeviceLocationResult.Unavailable
        }
    }

    private fun preferredProvider(): String? {
        val candidates =
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
            )

        return candidates.firstOrNull { provider ->
            LocationManagerCompat.hasProvider(
                locationManager,
                provider,
            ) &&
                runCatching {
                    locationManager.isProviderEnabled(
                        provider,
                    )
                }.getOrDefault(false)
        }
    }
}

internal sealed interface VexDeviceLocationResult {
    data class Located(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float?,
    ) : VexDeviceLocationResult

    data object PermissionMissing :
        VexDeviceLocationResult

    data object LocationDisabled :
        VexDeviceLocationResult

    data object Unavailable :
        VexDeviceLocationResult
}

// [VXG RealForever]
