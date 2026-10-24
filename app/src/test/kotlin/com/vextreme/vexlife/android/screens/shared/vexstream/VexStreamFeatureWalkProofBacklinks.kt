package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.testing.featurewalk.VexFeatureWalkProofBacklink
import com.vextreme.vexlife.android.testing.featurewalk.VexStreamFeatureWalkRefs
import com.vextreme.vexlife.android.vexstream.offline.VexStreamOfflineCopyTest

internal val vexStreamFeatureWalkProofBacklinks =
    listOf(
        VexFeatureWalkProofBacklink(
            checkpointRef =
                VexStreamFeatureWalkRefs
                    .LIBRARY_MISSING_DOWNLOADED,
            testClass =
                VexStreamOfflineCopyTest::class.java,
            testMethodName =
                "libraryBulkDownloadAndRemoveUsesOnlyThreeSongFixtureAndPreservesCatalog",
        ),
        VexFeatureWalkProofBacklink(
            checkpointRef =
                VexStreamFeatureWalkRefs
                    .LIBRARY_DOWNLOADS_REMOVED,
            testClass =
                VexStreamOfflineCopyTest::class.java,
            testMethodName =
                "libraryBulkDownloadAndRemoveUsesOnlyThreeSongFixtureAndPreservesCatalog",
        ),
        VexFeatureWalkProofBacklink(
            checkpointRef =
                VexStreamFeatureWalkRefs
                    .LIBRARY_CATALOG_PRESERVED_AFTER_REMOVE_ALL,
            testClass =
                VexStreamOfflineCopyTest::class.java,
            testMethodName =
                "libraryBulkDownloadAndRemoveUsesOnlyThreeSongFixtureAndPreservesCatalog",
        ),
        VexFeatureWalkProofBacklink(
            checkpointRef =
                VexStreamFeatureWalkRefs
                    .FILTERED_MEMBERSHIP,
            testClass =
                VexStreamLibraryProjectionTest::class.java,
            testMethodName =
                "durationFilterExcludesUnknownAndComposesWithSearchAndCategories",
        ),
        VexFeatureWalkProofBacklink(
            checkpointRef =
                VexStreamFeatureWalkRefs
                    .FILTERED_PLAYBACK_CONTEXT,
            testClass =
                VexStreamLibraryProjectionTest::class.java,
            testMethodName =
                "filteredProjectionIsExactPlaybackMembershipForRowsPlayAllAndShuffleAll",
        ),
        VexFeatureWalkProofBacklink(
            checkpointRef =
                VexStreamFeatureWalkRefs
                    .CLEAR_RESTORES_LIBRARY,
            testClass =
                VexStreamLibraryProjectionTest::class.java,
            testMethodName =
                "clearFiltersPreservesSelectedSortAndThreshold",
        ),
    )

// [VXG RealForever]
