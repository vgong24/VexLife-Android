package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.shell.AppShell

object UserProfileJourneys : VexJourneyShard {
    override val shardId = "shared.user-profile"

    override val journeys = listOf(
        vexJourney(
            id = "form-submit",
            title = "User Profile form submission",
            environments = listOf(
                VexJourneyEnvironments.Baseline,
                VexJourneyEnvironments.LargeText,
                VexJourneyEnvironments.AlternateLocaleFallback,
            ),
        ) {
            start(HomeScreen)
            tap(AppShell.Elements.Add)
            tap(AppShell.Elements.UserProfile)
            type(
                field = UserProfileScreen.DisplayName,
                value = "Journey Victor",
            )
            submit(UserProfileScreen.Save)
            tap(AppShell.Elements.Back)
        },
    )
}
