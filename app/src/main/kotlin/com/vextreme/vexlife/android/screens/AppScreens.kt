package com.vextreme.vexlife.android.screens

import com.vextreme.vexlife.android.elements.AppBlueprint
import com.vextreme.vexlife.android.elements.ScreenBlueprint
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.screens.primary.events.EventsScreen
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.primary.settings.SettingsScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.shell.AppShell

/**
 * Small root map of navigable screens.
 *
 * The package tree carries the same role distinction a developer sees in the app:
 *
 * screens/primary/... = peer destinations selected by bottom navigation/pager
 * screens/shared/...  = reusable destinations that may be pushed from several primary screens
 *
 * This file should stay boring. To understand or debug a screen, command-click its object below and
 * work in that screen's own folder rather than searching a large central screen registry.
 */
object AppScreens {
    val primary: List<ScreenBlueprint> = listOf(
        HomeScreen,
        EventsScreen,
        SettingsScreen,
    )

    val shared: List<ScreenBlueprint> = listOf(
        DebugMenuScreen,
        GalleryScreen,
        UserProfileScreen,
        VexStreamMusicScreen,
        TalkToVexScreen,
    )

    val all: List<ScreenBlueprint> = primary + shared

    val registry = UIBlueprintRegistry(
        AppBlueprint(
            shell = AppShell.blueprint,
            screens = all.map { it.asRegistration() },
        ),
    )
}
