package com.vextreme.vexlife.android.testing.featurewalk

import com.vextreme.vexlife.android.vexstream.VexStreamDocs

object VexStreamFeatureWalkRefs {
    const val FEATURE: String =
        VexStreamDocs.FEATURE_REF
    const val OFFLINE_DOWNLOAD: String =
        "$FEATURE/W01"
    const val DOWNLOAD_AND_REMOVE: String =
        "$OFFLINE_DOWNLOAD/C01"
    const val DIRECT_DOWNLOAD: String =
        "$DOWNLOAD_AND_REMOVE/P01"
    const val DOWNLOAD_VISIBLE: String =
        "$DOWNLOAD_AND_REMOVE/P02"
    const val REMOVE_DOWNLOAD: String =
        "$DOWNLOAD_AND_REMOVE/P03"
    const val CATALOG_IDENTITY_PRESERVED: String =
        "$DOWNLOAD_AND_REMOVE/P04"

    const val DOWNLOAD_COLLECTION_MISSING: String =
        "$OFFLINE_DOWNLOAD/C02"
    const val PARTIAL_COLLECTION_VISIBLE: String =
        "$DOWNLOAD_COLLECTION_MISSING/P01"
    const val DOWNLOAD_MISSING_ACTION: String =
        "$DOWNLOAD_COLLECTION_MISSING/P02"
    const val COLLECTION_AVAILABLE_OFFLINE: String =
        "$DOWNLOAD_COLLECTION_MISSING/P03"

    const val LIBRARY_ORGANIZATION: String =
        "$FEATURE/W02"
    const val SORT_FILTER_PLAYBACK: String =
        "$LIBRARY_ORGANIZATION/C01"
    const val ORGANIZER_REACHABLE: String =
        "$SORT_FILTER_PLAYBACK/P01"
    const val FILTERED_MEMBERSHIP: String =
        "$SORT_FILTER_PLAYBACK/P02"
    const val FILTERED_PLAYBACK_CONTEXT: String =
        "$SORT_FILTER_PLAYBACK/P03"
    const val CLEAR_RESTORES_LIBRARY: String =
        "$SORT_FILTER_PLAYBACK/P04"
}

/**
 * Android-local completion map for the VexStream Download/Remove path.
 *
 * The deeper Desktop V4 -> Android stream/offline/restart/reconnect proof is cross-repository and
 * remains owned by Vextreme-SDK's institutional EndToEnd relationship record. This map does not
 * duplicate that coordinator or claim its external evidence.
 */
internal object VexStreamFeatureWalks {
    val downloadAndRemove =
        VexFeatureWalkCase(
            ref =
                VexStreamFeatureWalkRefs.DOWNLOAD_AND_REMOVE,
            key =
                "download-and-remove",
            title =
                "Download and remove a VexStream song",
            checkpoints =
                listOf(
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexStreamFeatureWalkRefs.DIRECT_DOWNLOAD,
                        key =
                            "direct-download",
                        title =
                            "The exact VexStream track exposes the direct Download action",
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexStreamFeatureWalkRefs.DOWNLOAD_VISIBLE,
                        key =
                            "download-visible",
                        title =
                            "The exact downloaded track appears in Downloads",
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexStreamFeatureWalkRefs.REMOVE_DOWNLOAD,
                        key =
                            "remove-download",
                        title =
                            "Remove download targets that exact downloaded track",
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexStreamFeatureWalkRefs.CATALOG_IDENTITY_PRESERVED,
                        key =
                            "catalog-identity-preserved",
                        title =
                            "Removing the local copy preserves the VexStream catalog track identity",
                    ),
                ),
        )

    val downloadCollectionMissing =
        VexFeatureWalkCase(
            ref =
                VexStreamFeatureWalkRefs.DOWNLOAD_COLLECTION_MISSING,
            key =
                "download-collection-missing",
            title =
                "Download only the missing songs from a VexStream collection",
            checkpoints =
                listOf(
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexStreamFeatureWalkRefs.PARTIAL_COLLECTION_VISIBLE,
                        key =
                            "partial-collection-visible",
                        title =
                            "The collection remains browseable while only part of it is on this phone",
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexStreamFeatureWalkRefs.DOWNLOAD_MISSING_ACTION,
                        key =
                            "download-missing-action",
                        title =
                            "The collection exposes one Download missing action for current missing media",
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexStreamFeatureWalkRefs.COLLECTION_AVAILABLE_OFFLINE,
                        key =
                            "collection-available-offline",
                        title =
                            "After Download missing completes, every current collection track appears in Downloads",
                    ),
                ),
        )

    val sortFilterPlayback =
        VexFeatureWalkCase(
            ref =
                VexStreamFeatureWalkRefs.SORT_FILTER_PLAYBACK,
            key =
                "sort-filter-playback",
            title =
                "Organize the VexStream library before playback",
            checkpoints =
                listOf(
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexStreamFeatureWalkRefs
                                .ORGANIZER_REACHABLE,
                        key =
                            "organizer-reachable",
                        title =
                            "The library exposes one Sort and Filter organization surface",
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexStreamFeatureWalkRefs
                                .FILTERED_MEMBERSHIP,
                        key =
                            "filtered-membership",
                        title =
                            "Duration and categorical filters narrow the current playable library without admitting unknown duration",
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexStreamFeatureWalkRefs
                                .FILTERED_PLAYBACK_CONTEXT,
                        key =
                            "filtered-playback-context",
                        title =
                            "Visible rows and library Play or Shuffle use the same filtered listening membership",
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexStreamFeatureWalkRefs
                                .CLEAR_RESTORES_LIBRARY,
                        key =
                            "clear-restores-library",
                        title =
                            "Clearing filters restores ordinary playable library membership",
                    ),
                ),
        )

    val feature =
        VexFeatureWalkFeature(
            featureRef =
                VexStreamDocs.FEATURE_REF,
            currentName =
                VexStreamDocs.CURRENT_FEATURE_NAME,
            walks =
                listOf(
                    VexFeatureWalk(
                        ref =
                            VexStreamFeatureWalkRefs.OFFLINE_DOWNLOAD,
                        key =
                            "offline-download",
                        title =
                            "Offline download",
                        cases =
                            listOf(
                                downloadAndRemove,
                                downloadCollectionMissing,
                            ),
                    ),
                    VexFeatureWalk(
                        ref =
                            VexStreamFeatureWalkRefs.LIBRARY_ORGANIZATION,
                        key =
                            "library-organization",
                        title =
                            "Library organization",
                        cases =
                            listOf(
                                sortFilterPlayback,
                            ),
                    ),
                ),
        )
}

// [VXG RealForever]
