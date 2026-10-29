package com.vextreme.vexlife.android.testing.featurewalk

/**
 * Stable Android-local completion identity for the Theme Mode patient-zero feature.
 *
 * This is test/proof metadata only. ThemeMode semantic State remains owned by the product source,
 * Debug Menu remains only the developer-facing doorway, and Feature Walk does not become either
 * runtime owner.
 */
object ThemeModeFeatureWalkRefs {
    const val FEATURE: String =
        "feature.vexlife.theme-mode"
    const val DEBUG_MENU_TOGGLE: String =
        "$FEATURE/W01"
    const val TOGGLE_THEME_MODE: String =
        "$DEBUG_MENU_TOGGLE/C01"
    const val INSTALLED_TOGGLE_REALIZATION: String =
        "$TOGGLE_THEME_MODE/P01"
}

internal object ThemeModeFeatureWalks {
    val toggleThemeMode =
        VexFeatureWalkCase(
            ref =
                ThemeModeFeatureWalkRefs.TOGGLE_THEME_MODE,
            key =
                "toggle-theme-mode",
            title =
                "Toggle Theme Mode from Debug Menu",
            checkpoints =
                listOf(
                    VexFeatureWalkCheckpoint(
                        ref =
                            ThemeModeFeatureWalkRefs
                                .INSTALLED_TOGGLE_REALIZATION,
                        key =
                            "installed-toggle-realization",
                        title =
                            "The installed Debug Menu toggle enters ThemeModeFamily and the app renders the resulting Theme Mode",
                    ),
                ),
        )

    val feature =
        VexFeatureWalkFeature(
            featureRef =
                ThemeModeFeatureWalkRefs.FEATURE,
            currentName =
                "Theme Mode",
            walks =
                listOf(
                    VexFeatureWalk(
                        ref =
                            ThemeModeFeatureWalkRefs.DEBUG_MENU_TOGGLE,
                        key =
                            "debug-menu-toggle",
                        title =
                            "Debug Menu theme toggle",
                        cases =
                            listOf(
                                toggleThemeMode,
                            ),
                    ),
                ),
        )
}

// [VXG RealForever]
