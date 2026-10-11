package com.vextreme.vexlife.android

import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.primary.events.EventsScreen
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.primary.settings.SettingsScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.shell.AppShell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBlueprintTest {
    private val registry = AppScreens.registry

    @Test
    fun repositoryScreenGroupsMatchNavigationRole() {
        assertEquals(
            listOf(
                HomeScreen.screenId,
                EventsScreen.screenId,
                SettingsScreen.screenId,
            ),
            AppScreens.primary.map { it.screenId },
        )

        assertEquals(
            listOf(
                DebugMenuScreen.screenId,
                GalleryScreen.screenId,
                UserProfileScreen.screenId,
            ),
            AppScreens.shared.map { it.screenId },
        )
    }

    @Test
    fun sharedShellRemainsOutsideStaticScreenOwnership() {
        assertNull(registry.ownerScreenOf(AppShell.Elements.DebugMenu))
        assertNull(registry.ownerScreenOf(AppShell.Elements.Gallery))
        assertNull(registry.ownerScreenOf(AppShell.Elements.UserProfile))
        assertSame(DebugMenuScreen, registry.ownerScreenOf(DebugMenuScreen.ResetLog))
        assertSame(GalleryScreen, registry.ownerScreenOf(GalleryScreen.ElementsTab))
        assertSame(UserProfileScreen, registry.ownerScreenOf(UserProfileScreen.DisplayName))
    }

    @Test
    fun shellNavigationReferencesRegisteredScreenObjects() {
        val targets = registry.navigationTargetsFrom(
            listOf(
                AppShell.Elements.Home,
                AppShell.Elements.Events,
                AppShell.Elements.Settings,
                AppShell.Elements.DebugMenu,
                AppShell.Elements.Gallery,
                AppShell.Elements.UserProfile,
            ),
        )

        assertEquals(
            AppScreens.all.map { it.screenId }.toSet(),
            targets.map { it.screenId }.toSet(),
        )
    }

    @Test
    fun ordinaryScreenLayoutProducesNestedInspectableIdentity() {
        val root = registry.screenRoot(HomeScreen).element
        val preview = registry.childrenOf(root).single()
        val previewChildren = registry.childrenOf(preview)

        assertEquals("element.home.body", root.elementId)
        assertEquals("element.home.body.preview", preview.elementId)
        assertEquals(
            setOf(
                "element.home.body.preview.title",
                "element.home.body.preview.message",
            ),
            previewChildren.map { it.elementId }.toSet(),
        )
    }

    @Test
    fun userProfileFieldsRemainNestedUnderTheProfileCard() {
        val persistedParent = registry.parentOf(UserProfileScreen.PersistedName)
        val editParent = registry.parentOf(UserProfileScreen.DisplayName)
        val saveParent = registry.parentOf(UserProfileScreen.Save)

        assertSame(persistedParent, editParent)
        assertSame(editParent, saveParent)
        assertTrue(editParent?.elementId?.endsWith("body.profile") == true)
    }

    @Test
    fun galleryUsesTheSameNamedPanelObjectsItNavigatesTo() {
        assertSame(GalleryScreen.Content, registry.parentOf(GalleryScreen.ElementsPanel))
        assertSame(GalleryScreen.Content, registry.parentOf(GalleryScreen.AppGraphPanel))
        assertEquals(
            setOf(
                GalleryScreen.ElementsPanel.elementId,
                GalleryScreen.AppGraphPanel.elementId,
            ),
            registry.childrenOf(GalleryScreen.Content).map { it.elementId }.toSet(),
        )
    }

    @Test
    fun debugLogControlsRemainTraceableThroughTheirParentRow() {
        val showParent = registry.parentOf(DebugMenuScreen.ShowLog)
        val resetParent = registry.parentOf(DebugMenuScreen.ResetLog)

        assertEquals(showParent?.elementId, resetParent?.elementId)
        assertTrue(showParent?.elementId?.endsWith("log-history.actions") == true)
    }
}
