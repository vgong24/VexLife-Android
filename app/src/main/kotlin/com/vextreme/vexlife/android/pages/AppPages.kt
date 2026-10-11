package com.vextreme.vexlife.android.pages

import com.vextreme.vexlife.android.elements.AppBlueprint
import com.vextreme.vexlife.android.elements.PageBlueprint
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.pages.primary.events.EventsPage
import com.vextreme.vexlife.android.pages.primary.home.HomePage
import com.vextreme.vexlife.android.pages.primary.settings.SettingsPage
import com.vextreme.vexlife.android.pages.shared.debug.DebugMenuPage
import com.vextreme.vexlife.android.pages.shared.gallery.GalleryPage
import com.vextreme.vexlife.android.pages.shared.profile.UserProfilePage
import com.vextreme.vexlife.android.shell.AppShell

/**
 * Small root map of navigable pages.
 *
 * The package tree carries the same role distinction a developer sees in the app:
 *
 * pages/primary/... = peer destinations selected by bottom navigation/pager
 * pages/shared/...  = reusable destinations that may be pushed from several primary pages
 *
 * This file should stay boring. To understand or debug a page, command-click its object below and
 * work in that page's own folder rather than searching a large central screen registry.
 */
object AppPages {
    val primary: List<PageBlueprint> = listOf(
        HomePage,
        EventsPage,
        SettingsPage,
    )

    val shared: List<PageBlueprint> = listOf(
        DebugMenuPage,
        GalleryPage,
        UserProfilePage,
    )

    val all: List<PageBlueprint> = primary + shared

    val registry = UIBlueprintRegistry(
        AppBlueprint(
            shell = AppShell.blueprint,
            screens = all.map { it.asScreenBlueprint() },
        ),
    )
}
