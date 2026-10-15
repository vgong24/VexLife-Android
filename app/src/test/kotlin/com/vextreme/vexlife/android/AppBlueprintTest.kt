package com.vextreme.vexlife.android

import com.vextreme.vexlife.android.elements.ElementIntent
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.primary.events.EventsScreen
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.primary.settings.SettingsScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
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
                VexMapsScreen.screenId,
                VexStreamMusicScreen.screenId,
                TalkToVexScreen.screenId,
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
        assertSame(GalleryScreen, registry.ownerScreenOf(GalleryScreen.DialogModalSample))
        assertSame(GalleryScreen, registry.ownerScreenOf(GalleryScreen.BottomSheetModalSample))
        assertSame(UserProfileScreen, registry.ownerScreenOf(UserProfileScreen.DisplayName))
    }

    @Test
    fun allDeclaredNavigationTargetsAreRegisteredScreenObjects() {
        val targets = registry.navigationTargetsFrom(registry.allElements)

        assertEquals(
            AppScreens.all.map { it.screenId }.toSet(),
            targets.map { it.screenId }.toSet(),
        )
    }

    @Test
    fun ordinaryScreenLayoutProducesNestedInspectableIdentity() {
        val root = registry.screenRoot(HomeScreen).element
        val children = registry.childrenOf(root)
        val vexStream = children.single {
            it.elementId == HomeScreen.VexStreamSection.elementId
        }
        val systems = children.single {
            it.elementId == HomeScreen.SystemsSection.elementId
        }

        assertEquals("element.home.body", root.elementId)
        assertEquals(
            setOf(
                HomeScreen.ConnectVexStreamMusic.elementId,
                HomeScreen.VexStreamMusic.elementId,
            ),
            registry.childrenOf(vexStream).map { it.elementId }.toSet(),
        )
        assertEquals(
            setOf(
                HomeScreen.VexMaps.elementId,
                HomeScreen.TalkToVex.elementId,
            ),
            registry.childrenOf(systems).map { it.elementId }.toSet(),
        )
    }

    @Test
    fun everyToolbarActionIsRegisteredToItsOwningScreen() {
        AppScreens.all.forEach { screen ->
            screen.toolbarActions.forEach { action ->
                assertSame(
                    action,
                    registry.element(
                        action.elementId,
                    ),
                )
                assertSame(
                    screen,
                    registry.ownerScreenOf(
                        action,
                    ),
                )
                assertNull(
                    registry.parentOf(
                        action,
                    ),
                )
            }
        }
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
    fun gallerySwapIntentNamesItsLocalContainerAndChildExplicitly() {
        val intent = GalleryScreen.AppGraphTab.intent as ElementIntent.SwapTo

        assertSame(GalleryScreen.Content, intent.container)
        assertSame(GalleryScreen.AppGraphPanel, intent.child)
        assertSame(
            GalleryScreen,
            registry.ownerScreenOf(intent.container),
        )
        assertSame(
            GalleryScreen,
            registry.ownerScreenOf(intent.child),
        )
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

    @Test
    fun galleryModalBlueprintMatchesInheritedVexComposePaths() {
        assertSame(
            GalleryScreen.DialogModalTitle,
            registry.element(
                GalleryScreen.DialogModalTitle.elementId,
            ),
        )
        assertSame(
            GalleryScreen.DialogModalBody,
            registry.element(
                GalleryScreen.DialogModalBody.elementId,
            ),
        )
        assertSame(
            GalleryScreen.BottomSheetContent,
            registry.element(
                GalleryScreen.BottomSheetContent.elementId,
            ),
        )
        assertSame(
            GalleryScreen.BottomSheetTitle,
            registry.element(
                GalleryScreen.BottomSheetTitle.elementId,
            ),
        )
        assertSame(
            GalleryScreen.BottomSheetBody,
            registry.element(
                GalleryScreen.BottomSheetBody.elementId,
            ),
        )

        assertEquals(
            GalleryScreen.DialogModalSample.elementId +
                ".title",
            GalleryScreen.DialogModalTitle.elementId,
        )
        assertEquals(
            GalleryScreen.DialogModalSample.elementId +
                ".body",
            GalleryScreen.DialogModalBody.elementId,
        )
        assertEquals(
            GalleryScreen.BottomSheetModalSample.elementId +
                ".content",
            GalleryScreen.BottomSheetContent.elementId,
        )
        assertEquals(
            GalleryScreen.BottomSheetContent.elementId +
                ".title",
            GalleryScreen.BottomSheetTitle.elementId,
        )
        assertEquals(
            GalleryScreen.BottomSheetContent.elementId +
                ".body",
            GalleryScreen.BottomSheetBody.elementId,
        )

        assertSame(
            GalleryScreen.DialogModalSample,
            registry.parentOf(
                GalleryScreen.DialogModalTitle,
            ),
        )
        assertSame(
            GalleryScreen.DialogModalSample,
            registry.parentOf(
                GalleryScreen.DialogModalBody,
            ),
        )
        assertSame(
            GalleryScreen.BottomSheetModalSample,
            registry.parentOf(
                GalleryScreen.BottomSheetContent,
            ),
        )
        assertSame(
            GalleryScreen.BottomSheetContent,
            registry.parentOf(
                GalleryScreen.BottomSheetTitle,
            ),
        )
        assertSame(
            GalleryScreen.BottomSheetContent,
            registry.parentOf(
                GalleryScreen.BottomSheetBody,
            ),
        )
    }

    @Test
    fun vexStreamSearchAddsNewParentContinuityWithoutRenamingLegacyIds() {
        assertSame(
            VexStreamMusicScreen.SearchSheet,
            registry.parentOf(
                VexStreamMusicScreen.SearchContent,
            ),
        )

        assertSame(
            VexStreamMusicScreen.SearchContent,
            registry.parentOf(
                VexStreamMusicScreen.SearchPrompt,
            ),
        )
        assertSame(
            VexStreamMusicScreen.SearchContent,
            registry.parentOf(
                VexStreamMusicScreen.SearchNoResults,
            ),
        )

        assertSame(
            VexStreamMusicScreen.SearchContent,
            registry.parentOf(
                VexStreamMusicScreen.SearchQuery,
            ),
        )
        assertSame(
            VexStreamMusicScreen.SearchContent,
            registry.parentOf(
                VexStreamMusicScreen.CloseSearch,
            ),
        )

        assertTrue(
            VexStreamMusicScreen.SearchContent.elementId
                .startsWith(
                    VexStreamMusicScreen.SearchSheet.elementId + ".",
                ),
        )

        assertEquals(
            "element.vexstream-music.body.library.search",
            VexStreamMusicScreen.SearchQuery.elementId,
        )
        assertEquals(
            "element.vexstream-music.body.library.search.close",
            VexStreamMusicScreen.CloseSearch.elementId,
        )
    }

}
