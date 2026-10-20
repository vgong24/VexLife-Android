package com.vextreme.vexlife.android.testing.featurewalk

import com.vextreme.vexlife.android.screens.shared.maps.VexMapsDocs

object VexMapsFeatureWalkRefs {
    const val FEATURE: String =
        VexMapsDocs.FEATURE_REF
    const val DESTINATION_NAVIGATION: String =
        "$FEATURE/W01"
    const val DESTINATION_SHELL: String =
        "$DESTINATION_NAVIGATION/C01"
    const val DESTINATION_SHELL_REACHABLE: String =
        "$DESTINATION_SHELL/P01"
    const val DESTINATION_SHELL_BACK: String =
        "$DESTINATION_SHELL/P02"
    const val FIND_ROUTE_NAVIGATE: String =
        "$DESTINATION_NAVIGATION/C02"
    const val SEARCH_CURRENTNESS: String =
        "$FIND_ROUTE_NAVIGATE/P01"
    const val INSTALLED_FIND_ROUTE_NAVIGATE: String =
        "$FIND_ROUTE_NAVIGATE/P02"
}

internal object VexMapsFeatureWalks {
    val destinationShell =
        VexFeatureWalkCase(
            ref =
                VexMapsFeatureWalkRefs.DESTINATION_SHELL,
            key =
                "destination-shell",
            title =
                "VexMaps destination shell",
            checkpoints =
                listOf(
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexMapsFeatureWalkRefs
                                .DESTINATION_SHELL_REACHABLE,
                        key =
                            "screen-reachable",
                        title =
                            "The installed app can navigate from Home to the VexMaps Screen",
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexMapsFeatureWalkRefs
                                .DESTINATION_SHELL_BACK,
                        key =
                            "back-returns",
                        title =
                            "Android Back returns from VexMaps to the app shell",
                    ),
                ),
        )

    val findRouteNavigate =
        VexFeatureWalkCase(
            ref =
                VexMapsFeatureWalkRefs.FIND_ROUTE_NAVIGATE,
            key =
                "find-route-navigate",
            title =
                "Find a destination, form a route, and navigate",
            checkpoints =
                listOf(
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexMapsFeatureWalkRefs
                                .SEARCH_CURRENTNESS,
                        key =
                            "search-currentness",
                        title =
                            "A newer destination search owns results when an older provider returns late",
                    ),
                    VexFeatureWalkCheckpoint(
                        ref =
                            VexMapsFeatureWalkRefs
                                .INSTALLED_FIND_ROUTE_NAVIGATE,
                        key =
                            "installed-find-route-navigate",
                        title =
                            "The installed app completes the ordinary find, route, and navigate experience",
                        hold =
                            VexFeatureWalkHold(
                                reason =
                                    "Current VexMaps source preserves the ordinary find-to-route-to-navigate experience as incomplete.",
                                releasePredicate =
                                    "A canonical Journey executes the full path with the provider/native proof required by each boundary.",
                            ),
                    ),
                ),
        )

    val feature =
        VexFeatureWalkFeature(
            featureRef =
                VexMapsDocs.FEATURE_REF,
            currentName =
                VexMapsDocs.CURRENT_FEATURE_NAME,
            walks =
                listOf(
                    VexFeatureWalk(
                        ref =
                            VexMapsFeatureWalkRefs
                                .DESTINATION_NAVIGATION,
                        key =
                            "destination-navigation",
                        title =
                            "Destination navigation",
                        cases =
                            listOf(
                                destinationShell,
                                findRouteNavigate,
                            ),
                    ),
                ),
        )
}

// [VXG RealForever]
