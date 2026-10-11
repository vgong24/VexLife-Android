package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import com.vextreme.vexlife.android.shell.AppShell

object GalleryJourneys : VexJourneyShard {
    override val shardId = "shared.gallery"

    override val journeys = listOf(
        vexJourney(
            title = "Gallery elements, modals and local panel swap",
            environments = listOf(
                VexJourneyEnvironments.Baseline,
                VexJourneyEnvironments.LargeText,
            ),
        ) {
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
    )
}
