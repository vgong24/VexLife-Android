package com.vextreme.vexlife.android

import com.vextreme.vexlife.android.blueprint.DebugElements
import com.vextreme.vexlife.android.blueprint.GalleryElements
import com.vextreme.vexlife.android.blueprint.Screens
import com.vextreme.vexlife.android.blueprint.ShellElements
import com.vextreme.vexlife.android.blueprint.VexLifeBlueprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBlueprintTest {
    private val registry = VexLifeBlueprint.registry

    @Test
    fun firstPracticumRegistersFiveScreensAndSharedShellOutsideScreenOwnership() {
        assertEquals(
            listOf(
                Screens.Home.screenId,
                Screens.Events.screenId,
                Screens.Settings.screenId,
                Screens.DebugMenu.screenId,
                Screens.Gallery.screenId,
            ),
            registry.allScreens.map { it.screenId },
        )
        assertNull(registry.ownerScreenOf(ShellElements.DebugMenu))
        assertSame(Screens.DebugMenu, registry.ownerScreenOf(DebugElements.Reset))
        assertSame(Screens.Gallery, registry.ownerScreenOf(GalleryElements.ElementsTab))
    }

    @Test
    fun sharedShellNavigationProducesTheDeclaredReachableScreens() {
        val targets = registry.navigationTargetsFrom(
            listOf(
                ShellElements.Home,
                ShellElements.Events,
                ShellElements.Settings,
                ShellElements.DebugMenu,
                ShellElements.Gallery,
            ),
        )

        assertEquals(
            setOf(
                Screens.Home.screenId,
                Screens.Events.screenId,
                Screens.Settings.screenId,
                Screens.DebugMenu.screenId,
                Screens.Gallery.screenId,
            ),
            targets.map { it.screenId }.toSet(),
        )
    }

    @Test
    fun galleryPanelsRemainChildrenOfOneSwappableContainer() {
        assertSame(GalleryElements.Content, registry.parentOf(GalleryElements.ElementsPanel))
        assertSame(GalleryElements.Content, registry.parentOf(GalleryElements.AppGraphPanel))
        assertEquals(
            setOf(
                GalleryElements.ElementsPanel.elementId,
                GalleryElements.AppGraphPanel.elementId,
            ),
            registry.childrenOf(GalleryElements.Content).map { it.elementId }.toSet(),
        )
    }

    @Test
    fun debugHistoryActionsRemainPerceivableThroughTheBlueprint() {
        assertSame(DebugElements.LogHistoryActions, registry.parentOf(DebugElements.Show))
        assertSame(DebugElements.LogHistoryActions, registry.parentOf(DebugElements.Reset))
        assertTrue(registry.elementsForScreen(Screens.DebugMenu).contains(DebugElements.LogDialog))
    }
}
