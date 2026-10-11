package com.vextreme.vexlife.android.pages

import com.vextreme.vexlife.android.elements.AppBlueprint
import com.vextreme.vexlife.android.elements.PageBlueprint
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.pages.primary.events.EventsPage
import com.vextreme.vexlife.android.pages.primary.home.HomePage
import com.vextreme.vexlife.android.pages.primary.settings.SettingsPage
import com.vextreme.vexlife.android.pages.shared.debug.DebugMenuPage
import com.vextreme.vexlife.android.pages.shared.gallery.GalleryPage
import com.vextreme.vexlife.android.shell.AppShell

object AppPages {
    val primary: List<PageBlueprint> = listOf(
        HomePage,
        EventsPage,
        SettingsPage,
    )

    val shared: List<PageBlueprint> = listOf(
        DebugMenuPage,
        GalleryPage,
    )

    val all: List<PageBlueprint> = primary + shared

    val registry = UIBlueprintRegistry(
        AppBlueprint(
            shell = AppShell.blueprint,
            screens = all.map { it.asScreenBlueprint() },
        ),
    )
}
