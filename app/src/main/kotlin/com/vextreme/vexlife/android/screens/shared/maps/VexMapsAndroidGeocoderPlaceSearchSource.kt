package com.vextreme.vexlife.android.screens.shared.maps

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Best-effort Android platform place-search provider.
 *
 * Android owns whether a Geocoder implementation exists and whether one individual request can
 * produce useful results. VexMaps owns only the request/result currentness and presentation above it.
 */
internal class VexMapsAndroidGeocoderPlaceSearchSource(
    context: Context,
) :
    VexMapsDestinationSearchSource {
    private val geocoder =
        Geocoder(
            context.applicationContext,
            Locale.getDefault(),
        )

    override fun isAvailable(): Boolean =
        Geocoder.isPresent()

    override suspend fun search(
        query: String,
        limit: Int,
    ): List<VexMapsPlaceSearchResult> {
        require(limit in 1..5)

        if (!isAvailable()) {
            throw VexMapsDestinationSearchUnavailableException()
        }

        val normalized =
            query.trim()
        require(normalized.isNotEmpty())

        val addresses =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {
                searchAsync(
                    normalized,
                    limit,
                )
            } else {
                searchBlocking(
                    normalized,
                    limit,
                )
            }

        return addresses
            .mapNotNull(
                ::projectVexMapsAndroidGeocoderAddress,
            )
            .distinctBy {
                it.placeRef
            }
            .take(limit)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun searchAsync(
        query: String,
        limit: Int,
    ): List<Address> =
        suspendCancellableCoroutine {
                continuation ->
            geocoder.getFromLocationName(
                query,
                limit,
                object :
                    Geocoder.GeocodeListener {
                    override fun onGeocode(
                        addresses:
                            MutableList<Address>,
                    ) {
                        if (
                            continuation.isActive
                        ) {
                            continuation.resume(
                                addresses,
                            )
                        }
                    }

                    override fun onError(
                        errorMessage: String?,
                    ) {
                        if (
                            continuation.isActive
                        ) {
                            continuation
                                .resumeWithException(
                                    IOException(
                                        errorMessage
                                            ?: "Android Geocoder search failed",
                                    ),
                                )
                        }
                    }
                },
            )
        }

    @Suppress("DEPRECATION")
    private suspend fun searchBlocking(
        query: String,
        limit: Int,
    ): List<Address> =
        withContext(
            Dispatchers.IO,
        ) {
            geocoder
                .getFromLocationName(
                    query,
                    limit,
                )
                .orEmpty()
        }

}

internal fun projectVexMapsAndroidGeocoderAddress(
    address: Address,
): VexMapsPlaceSearchResult? {
    if (
        !address.hasLatitude() ||
        !address.hasLongitude()
    ) {
        return null
    }

    val addressLine =
        if (
            address.maxAddressLineIndex >= 0
        ) {
            address
                .getAddressLine(0)
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }
        } else {
            null
        }

    val name =
        listOfNotNull(
            address.featureName,
            address.thoroughfare,
            address.locality,
            address.subAdminArea,
            address.adminArea,
            address.countryName,
            addressLine,
        )
            .firstOrNull {
                it.isNotBlank()
            }
            ?.trim()
            ?: return null

    val subtitle =
        addressLine
            ?.takeIf {
                !it.equals(
                    name,
                    ignoreCase = true,
                )
            }
            ?: listOfNotNull(
                address.locality,
                address.adminArea,
                address.countryName,
            )
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .joinToString(
                    separator = ", ",
                )
                .takeIf {
                    it.isNotEmpty() &&
                        !it.equals(
                            name,
                            ignoreCase = true,
                        )
                }

    val coordinate =
        VexMapCoordinate(
            latitude =
                address.latitude,
            longitude =
                address.longitude,
        )

    return VexMapsPlaceSearchResult(
        placeRef =
            stableAndroidGeocoderPlaceRef(
                coordinate = coordinate,
                name = name,
                subtitle = subtitle,
            ),
        name = name,
        subtitle = subtitle,
        coordinate = coordinate,
    )
}

private fun stableAndroidGeocoderPlaceRef(
    coordinate: VexMapCoordinate,
    name: String,
    subtitle: String?,
): String {
    val canonical =
        buildString {
            append(
                String.format(
                    Locale.ROOT,
                    "%.6f,%.6f",
                    coordinate.latitude,
                    coordinate.longitude,
                ),
            )
            append('|')
            append(
                name
                    .trim()
                    .lowercase(
                        Locale.ROOT,
                    ),
            )
            append('|')
            append(
                subtitle
                    ?.trim()
                    ?.lowercase(
                        Locale.ROOT,
                    )
                    .orEmpty(),
            )
        }

    val digest =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                canonical
                    .toByteArray(
                        Charsets.UTF_8,
                    ),
            )
            .joinToString(
                separator = "",
            ) {
                byte ->
                (
                    byte.toInt() and
                        0xff
                )
                    .toString(16)
                    .padStart(
                        2,
                        '0',
                    )
            }

    return "android-geocoder." +
        digest.take(24)
}

// [VXG RealForever]
