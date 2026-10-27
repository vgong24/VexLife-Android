package com.vextreme.vexlife.android.ui

/**
 * Android realization of the portable ThemeMode semantic domain.
 *
 * The semantic values stay explicit even while the current Android implementation stores theme
 * state as a Boolean. Initialization/default-selection policy is intentionally not owned here:
 * this type defines WHAT values exist, not WHICH value a product should choose when no current
 * selection exists.
 */
enum class ThemeMode {
    Light,
    Dark,
}

/**
 * Bounded compatibility adapter for the current Android darkTheme Boolean.
 *
 * Boolean storage remains an implementation detail. Explicit ThemeMode values stay authoritative
 * at the semantic boundary so selecting the already-current value is idempotent rather than an
 * inversion. The exhaustive mappings also force this adapter to evolve if ThemeMode later admits
 * another value instead of silently collapsing that value into true/false.
 */
internal object ThemeModeBooleanAdapter {
    fun fromDarkTheme(darkTheme: Boolean): ThemeMode =
        if (darkTheme) ThemeMode.Dark else ThemeMode.Light

    fun toDarkTheme(themeMode: ThemeMode): Boolean =
        when (themeMode) {
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
}
