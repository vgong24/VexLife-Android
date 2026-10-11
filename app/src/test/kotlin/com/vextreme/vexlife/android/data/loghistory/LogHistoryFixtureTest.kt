package com.vextreme.vexlife.android.data.loghistory

import com.vextreme.vexlife.android.pages.primary.home.HomePage
import com.vextreme.vexlife.android.pages.shared.debug.DebugMenuPage
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
            appOpened(HomePage)
            afterSeconds(120)
            click(
                element = AppShell.Elements.DebugMenu,
                from = HomePage,
                to = DebugMenuPage,
            )
        }

        val extended = base.append {
            afterSeconds(30)
            click(
                element = DebugMenuPage.ResetLog,
                from = DebugMenuPage,
            )
        }

        assertEquals(2, base.records.size)
        assertEquals(3, extended.records.size)
        assertEquals(120L, base.records[1].durationSincePreviousSeconds)
        assertEquals(30L, extended.records[2].durationSincePreviousSeconds)
        assertEquals(3L, extended.records[2].sequence)
    }
}
