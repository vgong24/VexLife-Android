package com.vextreme.vexlife.android.screens.shared.maps

/**
 * # VexMaps
 *
 * Feature home:
 *
 * app/src/main/kotlin/com/vextreme/vexlife/android/screens/shared/maps/README.md
 *
 * Current intended experience:
 *
 * app/src/main/kotlin/com/vextreme/vexlife/android/screens/shared/maps/design/README.md
 *
 * Repository ownership topology:
 *
 * docs/foundation/VEXMAPS-FEATURE-TOPOLOGY.md
 *
 * Read VexMaps by human question:
 *
 * - [VexMapsScreen] owns stable Screen/action identity.
 * - [VexMapsMapSurface] is the native/custom composition root.
 * - [VexMapsPlaceSearchSession] owns local place-search pack/attempt/result currentness.
 * - [VexMapsDestinationSearchSession] owns ordinary visible "Where to?" request/result currentness.
 * - [VexMapsAndroidGeocoderPlaceSearchSource] is the first best-effort system destination-search
 *   provider; it does not own selected-destination or route truth.
 * - [VexMapsStyleConvergenceRelay] owns online style selection/application relationship.
 * - [VexNavigationSession] owns one prepared/active navigation journey.
 * - [VexMapLibreAdapter] owns translation into MapLibre rendering mechanics.
 *
 * Distribution is a child feature with its own feature home and stable identity under
 * feature.vexmaps.distribution.
 *
 * The active [VexMaps Complete] effort is an experience-completion trajectory, not a feature rename:
 * architecture can be accepted/current while the ordinary human "find -> route -> navigate" journey
 * remains incomplete.
 */
@Suppress("unused")
internal object VexMapsDocs {
    const val FEATURE_REF: String =
        "feature.vexmaps"

    const val CURRENT_FEATURE_NAME: String =
        "VexMaps"

    val historicalSearchNames: List<String> =
        emptyList()
}

// [VXG RealForever]
