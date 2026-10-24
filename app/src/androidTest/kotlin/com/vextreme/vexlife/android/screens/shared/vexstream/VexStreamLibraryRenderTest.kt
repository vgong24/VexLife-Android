package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.vextreme.vexlife.android.data.vexstream.VexStreamLibraryLoadState
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
            vexStreamRuntime()

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
            .onNodeWithContentDescription(
                "Download",
            )
            .assertIsDisplayed()
        compose
            .onNodeWithContentDescription(
                "Remove download",
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

    @Test
    fun restoringDownloadsShowsProgressInsteadOfTerminalEmptyState() {
        val runtime =
            vexStreamRuntime()

        compose.setContent {
            VexLifeTheme(
                darkTheme = true,
            ) {
                Surface {
                    Column {
                        VexStreamLibraryLoadingStatus(
                            loadState =
                                VexStreamLibraryLoadState.RESTORING_LOCAL,
                        )
                        VexStreamDownloadsPane(
                            runtime = runtime,
                            tracks = emptyList(),
                            canShowTerminalEmptyState = false,
                            offlineCopyConnection = null,
                            controller = null,
                            resolver = null,
                            onVisitArtist = {},
                        )
                    }
                }
            }
        }

        compose
            .onNodeWithText(
                "Restoring downloaded music…",
            )
            .assertIsDisplayed()
        compose
            .onNodeWithText(
                "No downloaded songs yet.",
            )
            .assertDoesNotExist()
    }

    @Test
    fun albumDetailKeepsOfflineCopyActionVisible() {
        val state =
            vexStreamPreviewState()
        val runtime =
            vexStreamRuntime()
        val resolver =
            VexStreamPlaybackSourceResolver(
                InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext,
            )

        compose.setContent {
            VexLifeTheme(
                darkTheme = true,
            ) {
                Surface {
                    VexStreamAlbumsPane(
                        runtime = runtime,
                        tracks = state.playableTracks(),
                        offlineCopyConnection = null,
                        controller = null,
                        resolver = resolver,
                        onVisitArtist = {},
                    )
                }
            }
        }

        compose
            .onNodeWithText(
                "Desktop Hub",
            )
            .performClick()
        compose
            .onNodeWithContentDescription(
                "Download",
            )
            .assertIsDisplayed()
    }

    private fun vexStreamRuntime(): VexRuntime =
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
}

// [VXG RealForever]
