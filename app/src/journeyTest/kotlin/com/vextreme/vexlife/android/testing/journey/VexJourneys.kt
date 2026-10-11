package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.debug.DebugMenuScreen
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import com.vextreme.vexlife.android.screens.shared.profile.UserProfileScreen
import com.vextreme.vexlife.android.screens.shared.vex.TalkToVexScreen
import com.vextreme.vexlife.android.screens.shared.vexstream.VexStreamMusicScreen
import com.vextreme.vexlife.android.shell.AppShell

/**
 * Current executable semantic map.
 *
 * These declarations are intentionally ordinary walks, not a second expectation tree. The same list
 * feeds the JVM coverage gate and Android instrumentation engine.
 */
object VexJourneyCatalog {
    val all: List<VexJourney> = listOf(
        vexJourney("Primary peers and native root Back") {
            start(HomeScreen)
            tap(AppShell.Elements.Events)
            tap(AppShell.Elements.Home)
            tap(AppShell.Elements.Settings)
            back()
        },

        vexJourney("Debug Log History gesture dismissal") {
            start(HomeScreen)
            tap(AppShell.Elements.Add)
            tap(AppShell.Elements.DebugMenu)
            tap(DebugMenuScreen.DarkMode)
            tap(DebugMenuScreen.ShowLog)
            dismiss(DebugMenuScreen.LogSheet)
            tap(AppShell.Elements.Back)
        },

        vexJourney("Debug Reset starts a fresh history") {
            start(HomeScreen)
            tap(AppShell.Elements.Add)
            tap(AppShell.Elements.DebugMenu)

            // Reset deliberately clears the same semantic history used as the journey oracle.
            // The expected-walk model mirrors that feature effect, then proves post-reset facts.
            tap(DebugMenuScreen.ResetLog)
            tap(DebugMenuScreen.ShowLog)
            dismiss(DebugMenuScreen.LogSheet)
            tap(AppShell.Elements.Back)
        },

        vexJourney("Gallery elements, modals and local panel swap") {
            start(HomeScreen)
            tap(AppShell.Elements.Add)
            tap(AppShell.Elements.Gallery)

            tap(GalleryScreen.SampleButton)
            tap(GalleryScreen.SampleToggle)
            tap(GalleryScreen.SampleFab)

            tap(GalleryScreen.ShowDialogModalSample)
            tap(GalleryScreen.CloseDialogModalSample)

            tap(GalleryScreen.ShowBottomSheetModalSample)
            tap(GalleryScreen.CloseBottomSheetModalSample)

            tap(GalleryScreen.AppGraphTab)
            tap(GalleryScreen.ElementsTab)
            tap(AppShell.Elements.Back)
        },

        vexJourney("User Profile form submission") {
            start(HomeScreen)
            tap(AppShell.Elements.Add)
            tap(AppShell.Elements.UserProfile)
            type(
                field = UserProfileScreen.DisplayName,
                value = "Journey Victor",
            )
            submit(UserProfileScreen.Save)
            tap(AppShell.Elements.Back)
        },

        vexJourney("VexStream Music placeholder") {
            start(HomeScreen)
            tap(HomeScreen.VexStreamMusic)
            tap(VexStreamMusicScreen.PlaceholderAction)
            tap(AppShell.Elements.Back)
        },

        vexJourney("Talk to Vex placeholder") {
            start(HomeScreen)
            tap(HomeScreen.TalkToVex)
            tap(TalkToVexScreen.PlaceholderAction)
            tap(AppShell.Elements.Back)
        },
    )
}
