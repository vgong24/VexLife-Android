package com.vextreme.vexlife.android.preview

import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.screens.primary.home.HomeScreen
import com.vextreme.vexlife.android.screens.shared.gallery.GalleryScreen
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VexPreviewSetupTest {
    private fun runtime(): VexRuntime =
        VexRuntime(
            registry = AppScreens.registry,
            logger = VexLogger(),
            initialScreen = HomeScreen,
            primaryScreens = AppScreens.primary,
            currentAppActions = { AppActionRegistrations.Empty },
        )

    @Test
    fun selectChildSeedsTheCurrentPreviewScreen() {
        val runtime = runtime()
        runtime.navigation.push(GalleryScreen)

        VexPreviewSetup(runtime).selectChild(
            container = GalleryScreen.Content,
            child = GalleryScreen.AppGraphPanel,
        )

        assertSame(
            GalleryScreen.AppGraphPanel,
            runtime.presentation.selected(GalleryScreen.Content),
        )
    }

    @Test
    fun selectChildCannotSeedAnotherScreensLocalState() {
        val runtime = runtime()

        try {
            VexPreviewSetup(runtime).selectChild(
                container = GalleryScreen.Content,
                child = GalleryScreen.AppGraphPanel,
            )
            fail("preview setup must not seed another Screen")
        } catch (error: IllegalStateException) {
            assertTrue(error.message.orEmpty().contains(GalleryScreen.screenId))
            assertTrue(error.message.orEmpty().contains(HomeScreen.screenId))
        }
    }
}
