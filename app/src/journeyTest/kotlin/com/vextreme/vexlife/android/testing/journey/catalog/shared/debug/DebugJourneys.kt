package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.shell.AppShell

object DebugJourneys : VexJourneyShard {
    override val shardId = "shared.debug"

    val logHistoryDismiss = vexJourney(
        id = "log-history-dismiss",
        title = "Debug Log History gesture dismissal",
        environments = listOf(
            VexJourneyEnvironments.Baseline,
            VexJourneyEnvironments.LargeText,
        ),
    ) {
        start(HomeScreen)
        tap(AppShell.Elements.Add)
        tap(AppShell.Elements.DebugMenu)
        tap(DebugMenuScreen.StartSyntheticEvents)
        tap(DebugMenuScreen.StopSyntheticEvents)
        tap(DebugMenuScreen.EmitSyntheticEvent)
        tap(DebugMenuScreen.EmitSyntheticEventBurst)
        tap(DebugMenuScreen.InsertHistoricalSyntheticEvent)
        tap(DebugMenuScreen.RemoveOldestSyntheticEvent)
        tap(DebugMenuScreen.DarkMode)
        tap(DebugMenuScreen.ShowLog)
        dismiss(DebugMenuScreen.LogSheet)
        tap(AppShell.Elements.Back)
    }

    val resetFreshHistory = vexJourney(
        id = "reset-fresh-history",
        title = "Debug Reset starts a fresh history",
    ) {
        start(HomeScreen)
        tap(AppShell.Elements.Add)
        tap(AppShell.Elements.DebugMenu)

        // Reset deliberately clears the same semantic history used as the journey oracle.
        // The expected-walk model mirrors that feature effect, then proves post-reset facts.
        tap(DebugMenuScreen.ResetLog)
        tap(DebugMenuScreen.ShowLog)
        dismiss(DebugMenuScreen.LogSheet)
        tap(AppShell.Elements.Back)
    }

    override val journeys = listOf(
        logHistoryDismiss,
        resetFreshHistory,
    )
}
