package com.vextreme.vexlife.android.screens.shared.maps

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionStatus

internal sealed interface VexMapsOfflineMapState {
    data class Loading(
        val retainedReady:
            Ready? = null,
    ) : VexMapsOfflineMapState

    data object None :
        VexMapsOfflineMapState

    data class Ready(
        val preferred:
            VexMapsOfflineMapRegion,
        val completeRegionCount: Int,
    ) : VexMapsOfflineMapState

    data class Error(
        val retainedReady:
            Ready? = null,
    ) : VexMapsOfflineMapState
}

internal fun VexMapsOfflineMapState.effectiveReady():
    VexMapsOfflineMapState.Ready? =
    when (this) {
        is VexMapsOfflineMapState.Ready ->
            this

        is VexMapsOfflineMapState.Loading ->
            retainedReady

        is VexMapsOfflineMapState.Error ->
            retainedReady

        VexMapsOfflineMapState.None ->
            null
    }

internal data class VexMapsOfflineMapRegion(
    val regionId: Long,
    val styleUri: String,
    val completedResourceCount: Long,
    val requiredResourceCount: Long,
)

internal data class VexMapsOfflineMapCandidate(
    val regionId: Long,
    val styleUri: String?,
    val isComplete: Boolean,
    val completedResourceCount: Long,
    val requiredResourceCount: Long,
)

internal fun selectPreferredOfflineMapRegion(
    preferredRegionId: Long?,
    candidates:
        List<VexMapsOfflineMapCandidate>,
): VexMapsOfflineMapRegion? {
    val complete =
        candidates
            .filter { candidate ->
                candidate.isComplete &&
                    !candidate.styleUri.isNullOrBlank()
            }

    val selected =
        complete.firstOrNull { candidate ->
            candidate.regionId ==
                preferredRegionId
        } ?: complete.maxByOrNull { candidate ->
            candidate.regionId
        } ?: return null

    return VexMapsOfflineMapRegion(
        regionId = selected.regionId,
        styleUri =
            requireNotNull(
                selected.styleUri,
            ),
        completedResourceCount =
            selected.completedResourceCount,
        requiredResourceCount =
            selected.requiredResourceCount,
    )
}

/**
 * Imports prepared MapLibre offline-region databases into MapLibre's app-private database.
 *
 * VexMaps intentionally does not bulk-download OpenFreeMap's public service. A map-region database
 * can instead be produced from an authorized/self-hosted tile source (for example VexHome) and then
 * selected through Android's document picker. The imported resources remain addressable by the
 * region's own style URL, while MapLibre serves the saved resources from its offline database.
 */
internal class VexMapsOfflineMapStore(
    context: Context,
) {
    private val appContext =
        context.applicationContext
    private val manager =
        OfflineManager.getInstance(
            appContext,
        )
    private val preferences =
        appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    suspend fun load():
        VexMapsOfflineMapState {
        val regions =
            listRegionsWithStatus()

        val preferred =
            selectPreferredOfflineMapRegion(
                preferredRegionId =
                    preferences
                        .getLong(
                            KEY_PREFERRED_REGION_ID,
                            NO_REGION_ID,
                        )
                        .takeIf {
                            it != NO_REGION_ID
                        },
                candidates =
                    regions.map { candidate ->
                        candidate.snapshot
                    },
            )

        if (preferred == null) {
            return VexMapsOfflineMapState.None
        }

        preferences
            .edit()
            .putLong(
                KEY_PREFERRED_REGION_ID,
                preferred.regionId,
            )
            .apply()

        return VexMapsOfflineMapState.Ready(
            preferred = preferred,
            completeRegionCount =
                regions.count { candidate ->
                    candidate.snapshot.isComplete &&
                        !candidate
                            .snapshot
                            .styleUri
                            .isNullOrBlank()
                },
        )
    }

    suspend fun import(
        source: Uri,
    ): VexMapsOfflineMapState {
        val staging =
            copyToPrivateStaging(source)

        try {
            val merged =
                merge(staging)

            val completeMerged =
                merged
                    .mapNotNull { region ->
                        val status =
                            status(region)

                        if (
                            status?.isComplete == true &&
                            !region
                                .definition
                                .styleURL
                                .isNullOrBlank()
                        ) {
                            region.id
                        } else {
                            null
                        }
                    }

            require(
                completeMerged.isNotEmpty(),
            ) {
                "imported map database contains no complete offline region"
            }

            preferences
                .edit()
                .putLong(
                    KEY_PREFERRED_REGION_ID,
                    completeMerged.max(),
                )
                .apply()

            return load()
        } finally {
            withContext(Dispatchers.IO) {
                staging.delete()
            }
        }
    }

    private suspend fun copyToPrivateStaging(
        source: Uri,
    ): File =
        withContext(Dispatchers.IO) {
            val directory =
                File(
                    appContext.cacheDir,
                    "vexmaps/offline-map-import",
                )
            check(
                directory.isDirectory ||
                    directory.mkdirs(),
            )

            val staging =
                File(
                    directory,
                    "maplibre-offline.db",
                )
            staging.delete()

            val copied =
                appContext
                    .contentResolver
                    .openInputStream(source)
                    ?.use { input ->
                        FileOutputStream(
                            staging,
                        ).use { output ->
                            input.copyTo(output)
                            output.fd.sync()
                        }
                        staging.length()
                    } ?: error(
                    "selected offline map database could not be opened",
                )

            require(copied > 0L) {
                "selected offline map database is empty"
            }

            staging
        }

    private suspend fun merge(
        database: File,
    ): List<OfflineRegion> =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                manager.mergeOfflineRegions(
                    database.absolutePath,
                    object :
                        OfflineManager.MergeOfflineRegionsCallback {
                        override fun onMerge(
                            offlineRegions:
                                Array<OfflineRegion>?,
                        ) {
                            if (
                                continuation.isActive
                            ) {
                                continuation.resume(
                                    offlineRegions
                                        ?.toList()
                                        .orEmpty(),
                                )
                            }
                        }

                        override fun onError(
                            error: String,
                        ) {
                            if (
                                continuation.isActive
                            ) {
                                continuation
                                    .resumeWithException(
                                        IllegalStateException(
                                            error,
                                        ),
                                    )
                            }
                        }
                    },
                )
            }
        }

    private suspend fun listRegions():
        List<OfflineRegion> =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                manager.listOfflineRegions(
                    object :
                        OfflineManager.ListOfflineRegionsCallback {
                        override fun onList(
                            offlineRegions:
                                Array<OfflineRegion>?,
                        ) {
                            if (
                                continuation.isActive
                            ) {
                                continuation.resume(
                                    offlineRegions
                                        ?.toList()
                                        .orEmpty(),
                                )
                            }
                        }

                        override fun onError(
                            error: String,
                        ) {
                            if (
                                continuation.isActive
                            ) {
                                continuation
                                    .resumeWithException(
                                        IllegalStateException(
                                            error,
                                        ),
                                    )
                            }
                        }
                    },
                )
            }
        }

    private suspend fun status(
        region: OfflineRegion,
    ): OfflineRegionStatus? =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                region.getStatus(
                    object :
                        OfflineRegion
                            .OfflineRegionStatusCallback {
                        override fun onStatus(
                            status:
                                OfflineRegionStatus?,
                        ) {
                            if (
                                continuation.isActive
                            ) {
                                continuation.resume(
                                    status,
                                )
                            }
                        }

                        override fun onError(
                            error: String?,
                        ) {
                            if (
                                continuation.isActive
                            ) {
                                continuation
                                    .resumeWithException(
                                        IllegalStateException(
                                            error
                                                ?: "offline map region status failed",
                                        ),
                                    )
                            }
                        }
                    },
                )
            }
        }

    private suspend fun listRegionsWithStatus():
        List<RegionWithStatus> =
        listRegions()
            .map { region ->
                val status =
                    status(region)

                RegionWithStatus(
                    region = region,
                    snapshot =
                        VexMapsOfflineMapCandidate(
                            regionId =
                                region.id,
                            styleUri =
                                region
                                    .definition
                                    .styleURL,
                            isComplete =
                                status?.isComplete ==
                                    true,
                            completedResourceCount =
                                status
                                    ?.completedResourceCount
                                    ?: 0L,
                            requiredResourceCount =
                                status
                                    ?.requiredResourceCount
                                    ?: 0L,
                        ),
                )
            }

    private data class RegionWithStatus(
        val region: OfflineRegion,
        val snapshot:
            VexMapsOfflineMapCandidate,
    )

    private companion object {
        const val PREFS_NAME =
            "vexmaps.offline-map"
        const val KEY_PREFERRED_REGION_ID =
            "preferred-region-id"
        const val NO_REGION_ID =
            Long.MIN_VALUE
    }
}

// [VXG RealForever]
