package com.vextreme.vexlife.android.data.loghistory

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.shell.AppShell
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class LogHistoryFixtureTest {
    @Test
    fun fixtureCanBeExtendedWithoutMutatingItsBaseScenario() {
        val base = LogHistoryFixture.build(
            startAt = Instant.parse("2026-10-11T01:51:00Z"),
        ) {
            appOpened(HomeScreen)
            afterSeconds(120)
            click(
                element = AppShell.Elements.DebugMenu,
                from = HomeScreen,
                to = DebugMenuScreen,
            )
        }

        val extended = base.append {
            afterSeconds(30)
            click(
                element = DebugMenuScreen.ResetLog,
                from = DebugMenuScreen,
            )
        }

        assertEquals(2, base.records.size)
        assertEquals(3, extended.records.size)
        assertEquals(120L, base.records[1].durationSincePreviousSeconds)
        assertEquals(30L, extended.records[2].durationSincePreviousSeconds)
        assertEquals(3L, extended.records[2].sequence)
    }
}
