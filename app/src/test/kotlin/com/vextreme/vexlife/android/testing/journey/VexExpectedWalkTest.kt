package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.logging.VexLogKind
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.shell.AppShell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexExpectedWalkTest {
    @Test
    fun navigationWalkBuildsCanonicalExpectedRecords() {
        val walk = VexExpectedWalk()

        walk.start(HomeScreen)
        walk.tap(AppShell.Elements.Add)
        walk.tap(AppShell.Elements.DebugMenu)

        assertEquals(DebugMenuScreen.screenId, walk.currentScreen.screenId)
        assertEquals(
            listOf(
                VexExpectedRecord(
                    kind = VexLogKind.AppOpened,
                    subjectRef = "app",
                    action = "App Opened",
                    screenBeforeId = null,
                    screenAfterId = HomeScreen.screenId,
                ),
                VexExpectedRecord(
                    kind = VexLogKind.Interaction,
                    subjectRef = AppShell.Elements.Add.elementId,
                    action = "Clicked",
                    screenBeforeId = HomeScreen.screenId,
                    screenAfterId = null,
                ),
                VexExpectedRecord(
                    kind = VexLogKind.Interaction,
                    subjectRef = AppShell.Elements.DebugMenu.elementId,
                    action = "Clicked",
                    screenBeforeId = HomeScreen.screenId,
                    screenAfterId = DebugMenuScreen.screenId,
                ),
            ),
            walk.records,
        )
    }

    @Test
    fun resetClearsTheExpectedHistoryOracle() {
        val walk = VexExpectedWalk()

        walk.start(HomeScreen)
        walk.tap(AppShell.Elements.Add)
        walk.tap(AppShell.Elements.DebugMenu)
        walk.tap(DebugMenuScreen.ResetLog)

        assertTrue(walk.records.isEmpty())

        walk.tap(DebugMenuScreen.ShowLog)

        assertEquals(1, walk.records.size)
        assertEquals(DebugMenuScreen.ShowLog.elementId, walk.records.single().subjectRef)
    }

    @Test
    fun submitUsesTypedStableFieldRefsWithoutLoggingTyping() {
        val walk = VexExpectedWalk()

        walk.start(HomeScreen)
        walk.tap(AppShell.Elements.Add)
        walk.tap(AppShell.Elements.UserProfile)
        walk.type(
            field = UserProfileScreen.DisplayName,
            value = "Journey Victor",
        )
        walk.submit(UserProfileScreen.Save)

        val submitted = walk.records.last()
        assertEquals("Submitted", submitted.action)
        assertEquals(UserProfileScreen.Save.elementId, submitted.subjectRef)
        assertEquals(
            """
            {
              "${UserProfileScreen.DisplayName.elementId}": "Journey Victor"
            }
            """.trimIndent(),
            submitted.detailsJson,
        )
    }
    @Test
    fun externalSignalIsNotAUserInteractionButDynamicTapIs() {
        val walk = VexExpectedWalk()
        val signal =
            journeySignal(
                signalRef = "signal.test.external",
            ) {
                error("expected-walk model must not execute external effects")
            }
        val dynamic =
            dynamicElement(
                "element.events.dynamic.event.test.attention.acknowledge",
            )

        walk.start(HomeScreen)
        walk.signal(signal)
        walk.tapDynamic(dynamic)

        assertEquals(2, walk.records.size)
        assertEquals(VexLogKind.AppOpened, walk.records.first().kind)
        assertEquals(
            VexExpectedRecord(
                kind = VexLogKind.Interaction,
                subjectRef = dynamic.elementId,
                action = "Clicked",
                screenBeforeId = HomeScreen.screenId,
                screenAfterId = null,
            ),
            walk.records.last(),
        )
    }

}
