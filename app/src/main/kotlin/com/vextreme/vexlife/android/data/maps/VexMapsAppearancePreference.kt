package com.vextreme.vexlife.android.data.maps

import java.util.Locale

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
            "appearance.light"

        VexMapsAppearancePreference.DARK ->
            "appearance.dark"
    }

/**
 * Reads the existing online_style_ref storage column as an appearance preference.
 *
 * "dark" / "liberty" are legacy provider-style values. The old implementation persisted the
 * resolved default style after a successful load, so those bytes cannot prove an explicit human
 * Light/Dark choice. They therefore migrate conservatively to Automatic.
 *
 * New explicit preferences use appearance.light / appearance.dark in the existing nullable column,
 * avoiding a Room schema migration while keeping human intent separate from provider identity.
 */
internal fun vexMapsAppearancePreferenceFromStoredValue(
    storedValue: String?,
): VexMapsAppearancePreference =
    when (
        storedValue
            ?.trim()
            ?.lowercase(
                Locale.ROOT,
            )
    ) {
        null,
        "",
        "automatic",
        ->
            VexMapsAppearancePreference.AUTOMATIC

        "appearance.light" ->
            VexMapsAppearancePreference.LIGHT

        "appearance.dark" ->
            VexMapsAppearancePreference.DARK

        "light",
        "liberty",
        "dark",
        ->
            VexMapsAppearancePreference.AUTOMATIC

        else ->
            VexMapsAppearancePreference.AUTOMATIC
    }

// [VXG RealForever]
