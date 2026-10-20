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
                            ),
                    ),
                ),
        )
}

// [VXG RealForever]
