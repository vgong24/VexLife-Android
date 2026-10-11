package com.vextreme.vexlife.android.screens

import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.AppBlueprint
import com.vextreme.vexlife.android.elements.UIBlueprintRegistry
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
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
    val primary: List<AppScreenBlueprint> = listOf(
        HomeScreen,
        EventsScreen,
        SettingsScreen,
    )

    val shared: List<AppScreenBlueprint> = listOf(
        DebugMenuScreen,
        GalleryScreen,
        UserProfileScreen,
        VexStreamMusicScreen,
        TalkToVexScreen,
    )

    val all: List<AppScreenBlueprint> = primary + shared

    private val byScreenId: Map<String, AppScreenBlueprint> =
        all.associateBy { it.screenId }

    val registry = UIBlueprintRegistry(
        AppBlueprint(
            shell = AppShell.blueprint,
            screens = all.map { it.asRegistration() },
        ),
    )

    /**
     * Forms one immutable action-wiring snapshot from the same registered Screen list.
     *
     * Adding a Screen to AppScreens is enough to include its local action owner. If the Screen
     * declares an Invoke action but forgets its registration override, coverage fails here rather
     * than waiting for the user to click an unwired control.
     */
    internal fun appActionRegistrations(
        context: ScreenActionContext,
    ): AppActionRegistrations {
        val combined = AppActionRegistrations.combine(
            *all
                .map { screen -> screen.appActionRegistrations(context) }
                .toTypedArray(),
        )

        val screenActionElements = all
            .flatMap { screen -> registry.elementsForScreen(screen) }
            .filterIsInstance<ActionElement>()

        return combined.requireCoverage(screenActionElements)
    }

    /**
     * Resolves the app-level Screen object that owns rendering/actions for one registered screenId.
     *
     * This map is derived from [all]; it is not another hand-maintained Screen registry.
     */
    internal fun require(screenId: String): AppScreenBlueprint =
        requireNotNull(byScreenId[screenId]) {
            "screen is registered in runtime blueprint but missing from AppScreens: $screenId"
        }
}
