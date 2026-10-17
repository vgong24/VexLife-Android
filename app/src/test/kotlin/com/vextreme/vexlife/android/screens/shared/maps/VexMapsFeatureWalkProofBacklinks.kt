package com.vextreme.vexlife.android.screens.shared.maps

import com.vextreme.vexlife.android.testing.featurewalk.VexFeatureWalkProofBacklink
import com.vextreme.vexlife.android.testing.featurewalk.VexMapsFeatureWalkRefs

internal val vexMapsFeatureWalkProofBacklinks =
    listOf(
        VexFeatureWalkProofBacklink(
            checkpointRef =
                VexMapsFeatureWalkRefs.SEARCH_CURRENTNESS,
            testClass =
                VexMapsDestinationSearchSessionTest::class,
            testMethodName =
                "newerSearchOwnsResultsEvenIfOlderProviderReturnsLate",
        ),
    )

// [VXG RealForever]
