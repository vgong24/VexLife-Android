package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.ui.VexLifeTheme
import org.junit.Rule
import org.junit.Test

class VexStreamLibraryRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun songsPresentationRendersFeatureOwnedFixtureWithoutPlaybackProvider() {
        val state =
            vexStreamPreviewState()
        val runtime =
            VexRuntime(
                registry = AppScreens.registry,
                logger = VexLogger(),
                initialScreen = AppScreens.primary.first(),
                primaryScreens = AppScreens.primary,
                currentAppActions = {
                    AppActionRegistrations.Empty
                },
            ).apply {
                navigation.push(
                    VexStreamMusicScreen,
                )
            }

        compose.setContent {
            VexLifeTheme(
                darkTheme = true,
            ) {
                Surface {
                    VexStreamSongsPane(
                        runtime = runtime,
                        tracks = state.playableTracks(),
                        offlineCopyConnection = null,
                        controller = null,
                        resolver = null,
                        onVisitArtist = {},
                    )
                }
            }
        }

        compose
            .onNodeWithText(
                "VexStream Mobile",
            )
            .assertIsDisplayed()
        compose
            .onNodeWithText(
                "Take It With You",
            )
            .assertIsDisplayed()
        compose
            .onNodeWithText(
                "Songs",
            )
            .assertIsDisplayed()
        compose
            .onNodeWithText(
                "Albums",
            )
            .assertIsDisplayed()
    }
}

// [VXG RealForever]
