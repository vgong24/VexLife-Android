package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.shell.AppShell

object DebugJourneys : VexJourneyShard {
    override val shardId = "shared.debug"

    override val journeys = listOf(
        vexJourney(
            title = "Debug Log History gesture dismissal",
            environments = listOf(
                VexJourneyEnvironments.Baseline,
                VexJourneyEnvironments.LargeText,
            ),
        ) {
            start(HomeScreen)
            tap(AppShell.Elements.Add)
            tap(AppShell.Elements.DebugMenu)
            tap(DebugMenuScreen.DarkMode)
            tap(DebugMenuScreen.ShowLog)
            dismiss(DebugMenuScreen.LogSheet)
            tap(AppShell.Elements.Back)
        },

        vexJourney("Debug Reset starts a fresh history") {
            start(HomeScreen)
            tap(AppShell.Elements.Add)
            tap(AppShell.Elements.DebugMenu)

            // Reset deliberately clears the same semantic history used as the journey oracle.
            // The expected-walk model mirrors that feature effect, then proves post-reset facts.
            tap(DebugMenuScreen.ResetLog)
            tap(DebugMenuScreen.ShowLog)
            dismiss(DebugMenuScreen.LogSheet)
            tap(AppShell.Elements.Back)
        },
    )
}
