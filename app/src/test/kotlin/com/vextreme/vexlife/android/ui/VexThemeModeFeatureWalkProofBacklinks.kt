package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.testing.featurewalk.ThemeModeFeatureWalkRefs
import com.vextreme.vexlife.android.testing.featurewalk.VexFeatureWalkProofBacklink

/**
 * Focused proof ownership for the active ThemeMode Feature-Walk checkpoint.
 *
 * The installed Journey owns the human interaction path. These backlinks keep the two semantic
 * consequences with the focused tests that already prove them instead of teaching Journey another
 * ThemeMode-specific interpretation:
 *
 * Debug Menu action -> ThemeModeFamily
 * ThemeModeFamily.DisplayState -> VexThemePresentation
 */
internal val vexThemeModeFeatureWalkProofBacklinks =
    listOf(
        VexFeatureWalkProofBacklink(
            checkpointRef =
                ThemeModeFeatureWalkRefs
                    .INSTALLED_TOGGLE_REALIZATION,
            testClass =
                VexLifeSessionTest::class.java,
            testMethodName =
                "debugMenuThemeActionRoutesThroughTheSessionThemeModeFamily",
        ),
        VexFeatureWalkProofBacklink(
            checkpointRef =
                ThemeModeFeatureWalkRefs
                    .INSTALLED_TOGGLE_REALIZATION,
            testClass =
                ThemeModeTest::class.java,
            testMethodName =
                "displayStateOwnsTheExpectedVexThemePresentation",
        ),
    )

// [VXG RealForever]
