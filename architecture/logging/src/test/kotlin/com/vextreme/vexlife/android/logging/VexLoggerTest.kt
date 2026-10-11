package com.vextreme.vexlife.android.logging

import com.vextreme.vexlife.android.elements.ActionKey
import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.invoke
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexLoggerTest {
    private object Home : ScreenElement("screen.home", 1)
    private object DebugMenu : ScreenElement("screen.debug-menu", 2)
    private object DebugButton : ButtonElement(
        elementId = "element.debug-menu",
        labelRes = 3,
        intent = invoke(ActionKey("action.debug")),
    )
    private object ResetButton : ButtonElement(
        elementId = "element.debug-menu.log-history.reset",
        labelRes = 4,
        intent = invoke(ActionKey("action.log.reset")),
    )
    private object SaveButton : ButtonElement(
        elementId = "element.user-profile.body.profile.actions.save",
        labelRes = 5,
        intent = invoke(ActionKey("action.user-profile.save")),
    )

    @Test
    fun readableHistoryGroupsEventsByScreenAndReportsVisitDurationOnce() {
        var now = Instant.parse("2026-10-10T18:51:00Z")
        val logger = VexLogger(
            clock = { now },
            zoneId = { ZoneId.of("UTC") },
        )

        logger.appOpened(Home)

        now = now.plusSeconds(120)
        logger.recordInteraction(DebugButton, Home, DebugMenu)

        now = now.plusSeconds(60)
        logger.recordInteraction(ResetButton, DebugMenu)

        val text = logger.renderHistory()

        assertTrue(text.contains("---HomeScreen---"))
        assertTrue(text.contains("[6:51:00PM Oct 10, 2026]\nApp Opened"))
        assertTrue(text.contains("[6:53:00PM Oct 10, 2026]\nDebugMenu Clicked"))
        assertTrue(text.contains("[Duration: 120 seconds]"))

        assertTrue(text.contains("---DebugMenuScreen---"))
        assertTrue(text.contains("[6:54:00PM Oct 10, 2026]\nReset Clicked"))

        assertFalse(text.contains("ScreenAppeared"))
        assertFalse(text.contains("ScreenDisappeared"))
        assertFalse(text.contains("| 120 sec"))
    }

    @Test
    fun submittedSnapshotRendersAsJsonOnlyAtTheSubmitBoundary() {
        val logger = VexLogger(
            clock = { Instant.parse("2026-10-10T18:51:00Z") },
            zoneId = { ZoneId.of("UTC") },
        )
        val json = """
            {
              "element.user-profile.body.profile.display-name": "Victor"
            }
        """.trimIndent()

        logger.appOpened(Home)
        logger.recordInteraction(
            element = SaveButton,
            screenBefore = Home,
            action = "Submitted",
            detailsJson = json,
        )

        val text = logger.renderHistory()

        assertTrue(text.contains("Save Submitted"))
        assertTrue(text.contains(json))
    }

    @Test
    fun screenScopesStartDestinationAtTheTransitionTimestamp() {
        var now = Instant.parse("2026-10-10T18:51:00Z")
        val logger = VexLogger(clock = { now })

        logger.appOpened(Home)
        now = now.plusSeconds(12)
        logger.recordInteraction(DebugButton, Home, DebugMenu)
        now = now.plusSeconds(7)
        logger.recordInteraction(ResetButton, DebugMenu)

        val scopes = VexLogger.screenScopes(logger.history.value)

        assertEquals(2, scopes.size)
        assertEquals(Home.screenId, scopes[0].screenId)
        assertEquals(12L, scopes[0].durationSeconds)
        assertEquals(DebugMenu.screenId, scopes[1].screenId)
        assertEquals(Instant.parse("2026-10-10T18:51:12Z"), scopes[1].enteredAt)
        assertEquals(null, scopes[1].durationSeconds)
    }

    @Test
    fun newAppOpenDoesNotInventDurationAcrossOfflineTime() {
        val first = VexLogRecord(
            sequence = 1,
            timestamp = Instant.parse("2026-10-10T18:51:00Z"),
            durationSincePreviousSeconds = null,
            kind = VexLogKind.AppOpened,
            subjectRef = "app",
            action = "App Opened",
            screenBeforeId = null,
            screenAfterId = Home.screenId,
            section = 0,
        )
        val second = first.copy(
            sequence = 1,
            timestamp = Instant.parse("2026-10-11T18:51:00Z"),
        )

        val scopes = VexLogger.screenScopes(listOf(first, second))

        assertEquals(2, scopes.size)
        assertEquals(null, scopes[0].durationSeconds)
        assertEquals(null, scopes[1].durationSeconds)
    }

    @Test
    fun observerFailureCannotUndoAcceptedLocalDiagnosticRecord() {
        val logger = VexLogger(
            clock = { Instant.parse("2026-10-10T18:51:00Z") },
            onRecord = { error("persistence sink failed") },
        )

        logger.appOpened(Home)

        assertEquals(1, logger.history.value.size)
        assertEquals(VexLogKind.AppOpened, logger.history.value.single().kind)
    }

    @Test
    fun observerRunsAfterRecordIsVisibleLocally() {
        lateinit var logger: VexLogger
        var locallyVisibleWhenObserved = false

        logger = VexLogger(
            clock = { Instant.parse("2026-10-10T18:51:00Z") },
            onRecord = { record ->
                locallyVisibleWhenObserved = logger.history.value.lastOrNull() == record
            },
        )

        logger.appOpened(Home)

        assertTrue(locallyVisibleWhenObserved)
    }
}
