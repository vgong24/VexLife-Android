package com.vextreme.vexlife.android

import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.elements.FormSubmission
import com.vextreme.vexlife.android.elements.navigation.NavigationTrail
import com.vextreme.vexlife.android.pages.AppPages
import com.vextreme.vexlife.android.pages.primary.events.EventsPage
import com.vextreme.vexlife.android.pages.primary.home.HomePage
import com.vextreme.vexlife.android.pages.primary.settings.SettingsPage
import com.vextreme.vexlife.android.pages.shared.debug.DebugMenuPage
import com.vextreme.vexlife.android.pages.shared.profile.UserProfilePage
import com.vextreme.vexlife.android.runtime.AppActionRequest
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.AppNavigationBack
import com.vextreme.vexlife.android.runtime.BackResolution
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.shell.AppShell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexRuntimeNavigationTest {
    private fun runtime(
        logger: VexLogger = VexLogger(),
        onAppAction: (AppActionRequest) -> Unit = {},
    ): VexRuntime =
        VexRuntime(
            registry = AppPages.registry,
            logger = logger,
            initialScreen = HomePage,
            primaryScreens = AppPages.primary,
            onAppAction = onAppAction,
        )

    @Test
    fun primaryPagesArePeersNotAStack() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.Events)

        assertEquals(EventsPage.screenId, runtime.currentScreen.screenId)
        assertTrue(runtime.navigation.pageStack.isEmpty())

        val back = runtime.nextBack as BackResolution.AppNavigation
        val root = back.step as AppNavigationBack.ReturnToPrimaryRoot
        assertEquals(HomePage.screenId, root.target.screenId)
    }

    @Test
    fun backFromPrimaryPeerReturnsHomeBeforeRootBackFallsThrough() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.Settings)

        assertEquals(SettingsPage.screenId, runtime.currentScreen.screenId)
        assertTrue(runtime.goBack())
        assertEquals(HomePage.screenId, runtime.currentScreen.screenId)

        assertEquals(null, runtime.nextBack)
        assertFalse(runtime.goBack())
    }

    @Test
    fun sharedPagePushesAndBackReturnsToTheSelectedPrimaryPage() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.Events)
        runtime.activate(AppShell.Elements.DebugMenu)

        assertEquals(DebugMenuPage.screenId, runtime.currentScreen.screenId)

        val back = runtime.nextBack as BackResolution.AppNavigation
        val pop = back.step as AppNavigationBack.PopPage
        assertEquals(EventsPage.screenId, pop.target.screenId)

        runtime.goBack(back)

        assertEquals(EventsPage.screenId, runtime.currentScreen.screenId)
        assertTrue(runtime.navigation.pageStack.isEmpty())
    }

    @Test
    fun pageLocalBackLayerPopsBeforeLeavingThePushedPage() {
        val runtime = runtime()
        val trail = NavigationTrail("root")

        runtime.activate(AppShell.Elements.Gallery)
        trail.push("child")
        runtime.attachBackLayer(trail)

        val localBack = runtime.nextBack as BackResolution.PopLocalLayer
        runtime.goBack(localBack)

        assertEquals("root", trail.current)
        assertEquals("screen.gallery", runtime.currentScreen.screenId)
        assertFalse(trail.canGoBack)

        runtime.goBack()

        assertEquals(HomePage.screenId, runtime.currentScreen.screenId)
    }

    @Test
    fun formSubmissionLogsOnlyTheSaveBoundaryAndForwardsTheSnapshot() {
        val logger = VexLogger()
        var request: AppActionRequest? = null
        val runtime = runtime(
            logger = logger,
            onAppAction = { request = it },
        )

        runtime.activate(AppShell.Elements.UserProfile)

        val typedName = "Victor Private Draft"
        runtime.submit(
            element = UserProfilePage.Save,
            submission = FormSubmission.of(
                UserProfilePage.DisplayName to typedName,
            ),
        )

        assertEquals(typedName, request?.submission?.value(UserProfilePage.DisplayName))
        assertEquals(AppActions.SaveUserProfile, request?.action)
        assertEquals(UserProfilePage.Save.elementId, request?.source?.elementId)

        val text = logger.renderHistory()
        assertTrue(text.contains("UserProfile.Body.Profile.Actions.Save"))
        assertFalse(text.contains(typedName))
    }

    @Test
    fun backDismissesPresentationBeforePoppingItsPage() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.DebugMenu)
        runtime.activate(DebugMenuPage.ShowLog)

        assertTrue(runtime.presentation.isOpen(DebugMenuPage.LogDialog))
        assertEquals(DebugMenuPage.screenId, runtime.currentScreen.screenId)
        assertEquals(BackResolution.DismissPresentation, runtime.nextBack)

        runtime.goBack(requireNotNull(runtime.nextBack))

        assertFalse(runtime.presentation.isOpen(DebugMenuPage.LogDialog))
        assertEquals(DebugMenuPage.screenId, runtime.currentScreen.screenId)

        runtime.goBack()

        assertEquals(HomePage.screenId, runtime.currentScreen.screenId)
    }
}
