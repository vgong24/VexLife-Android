package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexPreviewRepositories
import org.junit.Rule
import org.junit.Test

class VexStreamLibraryRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun pairedLibraryRendersFeatureOwnedFixture() {
        compose.setContent {
            CompositionLocalProvider(
                LocalInspectionMode provides true,
            ) {
                VexPreviewHost(
                    id = "vexstream-library-render-test",
                    startScreen = VexStreamMusicScreen,
                    repositoriesFactory = {
                        VexPreviewRepositories
                            .empty()
                            .copy(
                                vexStream =
                                    InMemoryVexStreamMusicRepository(
                                        initialState =
                                            vexStreamPreviewState(),
                                    ),
                            )
                    },
                ) { context ->
                    VexStreamMusicScreenContent(
                        runtime = context.runtime,
                        repository =
                            context.repositories.vexStream,
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
