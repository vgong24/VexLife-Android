package com.vextreme.vexlife.android.data.maps

/**
 * Human-facing VexMaps appearance preference.
 *
 * This is intentionally separate from any provider style identifier. Automatic follows the current
 * VexLife appearance; Light/Dark are explicit Maps overrides.
 */
enum class VexMapsAppearancePreference {
    AUTOMATIC,
    LIGHT,
    DARK,
}

internal fun VexMapsAppearancePreference.toStoredAppearanceValue():
    String? =
    when (this) {
        VexMapsAppearancePreference.AUTOMATIC ->
            null

        VexMapsAppearancePreference.LIGHT ->
            "light"

        VexMapsAppearancePreference.DARK ->
            "dark"
    }

/**
 * Reads the existing online_style_ref storage column as an appearance preference.
 *
 * "liberty" is the accepted legacy OpenFreeMap light-style value. It remains readable so this
 * semantic correction does not require a Room schema migration or strand existing preference data.
 */
internal fun vexMapsAppearancePreferenceFromStoredValue(
    storedValue: String?,
): VexMapsAppearancePreference =
    when (
        storedValue
            ?.trim()
            ?.lowercase()
    ) {
        null,
        "",
        "automatic",
        ->
            VexMapsAppearancePreference.AUTOMATIC

        "light",
        "liberty",
        ->
            VexMapsAppearancePreference.LIGHT

        "dark" ->
            VexMapsAppearancePreference.DARK

        else ->
            VexMapsAppearancePreference.AUTOMATIC
    }

// [VXG RealForever]
