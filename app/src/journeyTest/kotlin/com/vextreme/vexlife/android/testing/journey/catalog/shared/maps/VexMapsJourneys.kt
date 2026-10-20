package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.shell.AppShell
import com.vextreme.vexlife.android.testing.featurewalk.FeatureWalks
import com.vextreme.vexlife.android.testing.featurewalk.vexFeatureWalkJourney

object VexMapsJourneys : VexJourneyShard {
    override val shardId = "shared.vex-maps"

    private val destinationShellFeatureWalk =
        vexFeatureWalkJourney(
            featureWalkCase =
                FeatureWalks
                    .vexMaps
                    .destinationNavigation
                    .destinationShell,
        ) {
            start(HomeScreen)
            tap(HomeScreen.VexMaps)
            tap(AppShell.Elements.Back)
        }

    val destinationShell =
        destinationShellFeatureWalk.journey

    override val journeys =
        listOf(
            destinationShell,
        )

    override val featureWalkJourneys =
        listOf(
            destinationShellFeatureWalk,
        )
}

// [VXG RealForever]
