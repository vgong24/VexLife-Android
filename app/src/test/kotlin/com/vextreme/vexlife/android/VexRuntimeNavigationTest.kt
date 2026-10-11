package com.vextreme.vexlife.android

import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.elements.navigation.NavigationTrail
import com.vextreme.vexlife.android.pages.AppPages
import com.vextreme.vexlife.android.pages.primary.events.EventsPage
import com.vextreme.vexlife.android.pages.primary.home.HomePage
import com.vextreme.vexlife.android.pages.primary.settings.SettingsPage
import com.vextreme.vexlife.android.pages.shared.debug.DebugMenuPage
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.shell.AppShell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexRuntimeNavigationTest {
    private fun runtime(): VexRuntime =
        VexRuntime(
            registry = AppPages.registry,
            logger = VexLogger(),
            initialScreen = HomePage,
            primaryScreens = AppPages.primary,
            onAppAction = { _, _ -> },
        )

    @Test
    fun primaryPagesArePeersNotAStack() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.Events)

        assertEquals(EventsPage.screenId, runtime.currentScreen.screenId)
        assertTrue(runtime.canGoBack)
        assertTrue(runtime.navigation.pageStack.isEmpty())
    }

    @Test
    fun backFromPrimaryPeerReturnsHomeBeforeRootBackFallsThrough() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.Settings)

        assertEquals(SettingsPage.screenId, runtime.currentScreen.screenId)
        assertTrue(runtime.goBack())
        assertEquals(HomePage.screenId, runtime.currentScreen.screenId)

        assertFalse(runtime.canGoBack)
        assertFalse(runtime.goBack())
    }

    @Test
    fun sharedPagePushesAndBackReturnsToTheSelectedPrimaryPage() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.Events)
        runtime.activate(AppShell.Elements.DebugMenu)

        assertEquals(DebugMenuPage.screenId, runtime.currentScreen.screenId)
        assertTrue(runtime.navigation.canPopPage)

        runtime.goBack()

        assertEquals(EventsPage.screenId, runtime.currentScreen.screenId)
        assertFalse(runtime.navigation.canPopPage)
    }

    @Test
    fun pageLocalBackLayerPopsBeforeLeavingThePushedPage() {
        val runtime = runtime()
        val trail = NavigationTrail("root")

        runtime.activate(AppShell.Elements.Gallery)
        trail.push("child")
        runtime.attachBackLayer(trail)

        runtime.goBack()

        assertEquals("root", trail.current)
        assertEquals("screen.gallery", runtime.currentScreen.screenId)
        assertFalse(trail.canGoBack)

        runtime.goBack()

        assertEquals(HomePage.screenId, runtime.currentScreen.screenId)
    }

    @Test
    fun backDismissesPresentationBeforePoppingItsPage() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.DebugMenu)
        runtime.activate(DebugMenuPage.ShowLog)

        assertTrue(runtime.presentation.isOpen(DebugMenuPage.LogDialog))
        assertEquals(DebugMenuPage.screenId, runtime.currentScreen.screenId)

        runtime.goBack()

        assertFalse(runtime.presentation.isOpen(DebugMenuPage.LogDialog))
        assertEquals(DebugMenuPage.screenId, runtime.currentScreen.screenId)

        runtime.goBack()

        assertEquals(HomePage.screenId, runtime.currentScreen.screenId)
    }
}
