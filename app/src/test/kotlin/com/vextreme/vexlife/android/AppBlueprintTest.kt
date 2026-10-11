package com.vextreme.vexlife.android

import com.vextreme.vexlife.android.pages.AppPages
import com.vextreme.vexlife.android.pages.primary.events.EventsPage
import com.vextreme.vexlife.android.pages.primary.home.HomePage
import com.vextreme.vexlife.android.pages.primary.settings.SettingsPage
import com.vextreme.vexlife.android.pages.shared.debug.DebugMenuPage
import com.vextreme.vexlife.android.pages.shared.gallery.GalleryPage
import com.vextreme.vexlife.android.shell.AppShell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBlueprintTest {
    private val registry = AppPages.registry

    @Test
    fun repositoryPageGroupsMatchNavigationRole() {
        assertEquals(
            listOf(
                HomePage.screenId,
                EventsPage.screenId,
                SettingsPage.screenId,
            ),
            AppPages.primary.map { it.screenId },
        )

        assertEquals(
            listOf(
                DebugMenuPage.screenId,
                GalleryPage.screenId,
            ),
            AppPages.shared.map { it.screenId },
        )
    }

    @Test
    fun sharedShellRemainsOutsideStaticScreenOwnership() {
        assertNull(registry.ownerScreenOf(AppShell.Elements.DebugMenu))
        assertNull(registry.ownerScreenOf(AppShell.Elements.Gallery))
        assertSame(DebugMenuPage, registry.ownerScreenOf(DebugMenuPage.ResetLog))
        assertSame(GalleryPage, registry.ownerScreenOf(GalleryPage.ElementsTab))
    }

    @Test
    fun shellNavigationReferencesRegisteredPageObjects() {
        val targets = registry.navigationTargetsFrom(
            listOf(
                AppShell.Elements.Home,
                AppShell.Elements.Events,
                AppShell.Elements.Settings,
                AppShell.Elements.DebugMenu,
                AppShell.Elements.Gallery,
            ),
        )

        assertEquals(
            AppPages.all.map { it.screenId }.toSet(),
            targets.map { it.screenId }.toSet(),
        )
    }

    @Test
    fun ordinaryPageLayoutProducesNestedInspectableIdentity() {
        val root = registry.screenRoot(HomePage).element
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
    fun galleryUsesTheSameNamedPanelObjectsItNavigatesTo() {
        assertSame(GalleryPage.Content, registry.parentOf(GalleryPage.ElementsPanel))
        assertSame(GalleryPage.Content, registry.parentOf(GalleryPage.AppGraphPanel))
        assertEquals(
            setOf(
                GalleryPage.ElementsPanel.elementId,
                GalleryPage.AppGraphPanel.elementId,
            ),
            registry.childrenOf(GalleryPage.Content).map { it.elementId }.toSet(),
        )
    }

    @Test
    fun debugLogControlsRemainTraceableThroughTheirParentRow() {
        val showParent = registry.parentOf(DebugMenuPage.ShowLog)
        val resetParent = registry.parentOf(DebugMenuPage.ResetLog)

        assertEquals(showParent?.elementId, resetParent?.elementId)
        assertTrue(showParent?.elementId?.endsWith("log-history.actions") == true)
    }
}
