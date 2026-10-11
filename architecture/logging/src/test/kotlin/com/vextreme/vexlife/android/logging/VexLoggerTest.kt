package com.vextreme.vexlife.android.logging

import com.vextreme.vexlife.android.elements.ButtonElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.invoke
import com.vextreme.vexlife.android.elements.ActionKey
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
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

    @Test
    fun readableHistoryPreservesSemanticSequenceAndDurations() {
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

        now = now.plusSeconds(30)
        logger.recordInteraction(ResetButton, DebugMenu)

        val text = logger.renderHistory()

        assertTrue(text.contains("[6:53:00PM Oct 10, 2026 | 120 sec][DebugMenu | Clicked]"))
        assertTrue(text.contains("[HomeScreen ScreenDisappeared -> DebugMenuScreen ScreenAppeared]"))
        assertTrue(text.contains("[DebugMenu.LogHistory.Reset | Clicked][DebugMenuScreen]"))
        assertTrue(text.contains("---"))
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

    @Test
    fun sectionResetAddsReadableBoundaryWithoutErasingEvidence() {
        var now = Instant.parse("2026-10-10T18:51:00Z")
        val logger = VexLogger(clock = { now }, zoneId = { ZoneId.of("UTC") })

        logger.appOpened(Home)
        logger.startNewSection()
        now = now.plusSeconds(5)
        logger.recordInteraction(ResetButton, Home)

        assertEquals(2, logger.history.value.size)
        assertTrue(logger.renderHistory().contains("---"))
    }
}
