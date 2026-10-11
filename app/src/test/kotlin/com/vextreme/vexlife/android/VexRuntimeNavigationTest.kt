package com.vextreme.vexlife.android

import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.elements.FormSubmission
import com.vextreme.vexlife.android.elements.navigation.NavigationTrail
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.primary.events.EventsScreen
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.primary.settings.SettingsScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.appAction
import com.vextreme.vexlife.android.runtime.AppActions
import com.vextreme.vexlife.android.runtime.AppNavigationBack
import com.vextreme.vexlife.android.runtime.BackResolution
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.shell.AppShell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VexRuntimeNavigationTest {
    private fun runtime(
        logger: VexLogger = VexLogger(),
        appActions: AppActionRegistrations = AppActionRegistrations.Empty,
    ): VexRuntime =
        VexRuntime(
            registry = AppScreens.registry,
            logger = logger,
            initialScreen = HomeScreen,
            primaryScreens = AppScreens.primary,
            currentAppActions = { appActions },
        )

    @Test
    fun primaryScreensArePeersNotAStack() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.Events)

        assertEquals(EventsScreen.screenId, runtime.currentScreen.screenId)
        assertTrue(runtime.navigation.screenStack.isEmpty())

        val back = runtime.nextBack as BackResolution.AppNavigation
        val root = back.step as AppNavigationBack.ReturnToPrimaryRoot
        assertEquals(HomeScreen.screenId, root.target.screenId)
    }

    @Test
    fun backFromPrimaryPeerReturnsHomeBeforeRootBackFallsThrough() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.Settings)

        assertEquals(SettingsScreen.screenId, runtime.currentScreen.screenId)
        assertTrue(runtime.goBack())
        assertEquals(HomeScreen.screenId, runtime.currentScreen.screenId)

        assertEquals(null, runtime.nextBack)
        assertFalse(runtime.goBack())
    }

    @Test
    fun sharedScreenPushesAndBackReturnsToTheSelectedPrimaryScreen() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.Events)
        runtime.activate(AppShell.Elements.DebugMenu)

        assertEquals(DebugMenuScreen.screenId, runtime.currentScreen.screenId)

        val back = runtime.nextBack as BackResolution.AppNavigation
        val pop = back.step as AppNavigationBack.PopScreen
        assertEquals(EventsScreen.screenId, pop.target.screenId)

        runtime.goBack(back)

        assertEquals(EventsScreen.screenId, runtime.currentScreen.screenId)
        assertTrue(runtime.navigation.screenStack.isEmpty())
    }

    @Test
    fun screenLocalBackLayerPopsBeforeLeavingThePushedScreen() {
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

        assertEquals(HomeScreen.screenId, runtime.currentScreen.screenId)
    }

    @Test
    fun swapChangesLocalSelectionWithoutAddingBackDepth() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.Gallery)
        runtime.activate(GalleryScreen.AppGraphTab)

        assertSame(
            GalleryScreen.AppGraphPanel,
            runtime.presentation.selected(GalleryScreen.Content),
        )

        runtime.goBack()

        assertEquals(HomeScreen.screenId, runtime.currentScreen.screenId)
    }

    @Test
    fun offScreenOwnedElementCannotBeActivatedDirectly() {
        val runtime = runtime()

        try {
            runtime.activate(UserProfileScreen.Save)
            fail("off-screen feature element activation should fail")
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty().contains(UserProfileScreen.screenId))
            assertTrue(error.message.orEmpty().contains(HomeScreen.screenId))
        }
    }

    @Test
    fun formSubmissionLogsOneSubmittedSnapshotAndForwardsIt() {
        val logger = VexLogger()
        var request: com.vextreme.vexlife.android.runtime.AppActionRequest? = null
        val actions = AppActionRegistrations.of(
            appAction(
                action = AppActions.SaveUserProfile,
                ownerRef = UserProfileScreen.screenId,
            ) {
                request = it
            },
        )
        val runtime = runtime(
            logger = logger,
            appActions = actions,
        )

        runtime.activate(AppShell.Elements.UserProfile)

        val typedName = "Victor Private Draft"
        runtime.submit(
            element = UserProfileScreen.Save,
            submission = FormSubmission.of(
                UserProfileScreen.DisplayName to typedName,
            ),
        )

        assertEquals(typedName, request?.submission?.value(UserProfileScreen.DisplayName))
        assertEquals(AppActions.SaveUserProfile, request?.action)
        assertEquals(UserProfileScreen.Save.elementId, request?.source?.elementId)

        val text = logger.renderHistory()
        assertTrue(text.contains("Save Submitted"))
        assertTrue(text.contains(typedName))
        assertTrue(
            text.contains(
                UserProfileScreen.DisplayName.elementId,
            ),
        )
    }

    @Test
    fun platformDismissalLogsThePresentationWithoutInventingACloseClick() {
        val logger = VexLogger()
        val runtime = runtime(logger = logger)

        runtime.activate(AppShell.Elements.DebugMenu)
        runtime.activate(DebugMenuScreen.ShowLog)
        runtime.dismissPresentation(DebugMenuScreen.LogSheet)

        assertFalse(runtime.presentation.isOpen(DebugMenuScreen.LogSheet))
        assertTrue(logger.renderHistory().contains("Sheet Dismissed"))
        assertFalse(logger.renderHistory().contains("Close Clicked"))
    }

    @Test
    fun backDismissesPresentationBeforePoppingItsScreen() {
        val runtime = runtime()

        runtime.activate(AppShell.Elements.DebugMenu)
        runtime.activate(DebugMenuScreen.ShowLog)

        assertTrue(runtime.presentation.isOpen(DebugMenuScreen.LogSheet))
        assertEquals(DebugMenuScreen.screenId, runtime.currentScreen.screenId)
        assertEquals(BackResolution.DismissPresentation, runtime.nextBack)

        runtime.goBack(requireNotNull(runtime.nextBack))

        assertFalse(runtime.presentation.isOpen(DebugMenuScreen.LogSheet))
        assertEquals(DebugMenuScreen.screenId, runtime.currentScreen.screenId)

        runtime.goBack()

        assertEquals(HomeScreen.screenId, runtime.currentScreen.screenId)
    }
}
