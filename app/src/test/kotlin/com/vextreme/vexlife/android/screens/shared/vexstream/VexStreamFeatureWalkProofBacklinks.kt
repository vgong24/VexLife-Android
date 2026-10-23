package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.testing.featurewalk.VexFeatureWalkProofBacklink
import com.vextreme.vexlife.android.testing.featurewalk.VexStreamFeatureWalkRefs

internal val vexStreamFeatureWalkProofBacklinks =
    listOf(
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
                    .CLEAR_RESTORES_LIBRARY,
            testClass =
                VexStreamLibraryProjectionTest::class.java,
            testMethodName =
                "clearFiltersPreservesSelectedSortAndThreshold",
        ),
    )

// [VXG RealForever]
