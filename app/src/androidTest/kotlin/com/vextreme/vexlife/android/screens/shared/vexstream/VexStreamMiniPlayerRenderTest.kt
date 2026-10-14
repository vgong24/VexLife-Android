package com.vextreme.vexlife.android.screens.shared.vexstream

import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.ui.VexLifeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Render-contract proof for an already-projected VexStream playing state.
 *
 * Playback/session semantics are intentionally absent: this test starts from known presentation
 * truth and proves that Compose exposes the expected semantic controls. Pixel appearance belongs to
 * the paired deterministic golden.
 */
class VexStreamMiniPlayerRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun playingProjectionRendersExpectedSemanticSurface() {
        val activated =
            mutableListOf<ActionElement>()

        compose.setContent {
            VexLifeTheme(
                darkTheme = true,
            ) {
                Surface {
                    VexStreamMiniPlayerContent(
                        playback =
                            VexStreamPlaybackUiState(
                                currentMediaId =
                                    "preview.streamed",
                                currentTitle =
                                    "VexStream Mobile",
                                currentArtist = "Vex",
                                isPlaying = true,
                                queueMediaIds =
                                    listOf(
                                        "preview.streamed",
                                        "preview.downloaded",
                                    ),
                                currentIndex = 0,
                                currentPositionMs = 42_000L,
                                durationMs = 205_000L,
                                bufferedPositionMs = 91_000L,
                            ),
                        controls =
                            VexStreamPlaybackControlAvailability(
                                canSeek = true,
                                canPrevious = false,
                                canTogglePlayback = true,
                                canNext = true,
                                canShuffle = true,
                            ),
                        handler =
                            ElementActionHandler { element ->
                                activated += element
                            },
                        shuffleSelected = false,
                        onSeek = {},
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
            .onNodeWithText("Vex")
            .assertIsDisplayed()
        compose
            .onNodeWithText("1/2")
            .assertIsDisplayed()
        compose
            .onNodeWithText("0:42")
            .assertIsDisplayed()
        compose
            .onNodeWithText("3:25")
            .assertIsDisplayed()

        compose
            .onNodeWithTag(
                VexStreamMusicScreen
                    .SeekPlayback
                    .elementId,
            )
            .assertIsDisplayed()
            .assertIsEnabled()

        compose
            .onNodeWithTag(
                VexStreamMusicScreen
                    .PreviousTrack
                    .elementId,
            )
            .assertIsDisplayed()
            .assertIsNotEnabled()

        compose
            .onNodeWithTag(
                VexStreamMusicScreen
                    .TogglePlayback
                    .elementId,
            )
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .performClick()

        compose
            .onNodeWithTag(
                VexStreamMusicScreen
                    .NextTrack
                    .elementId,
            )
            .assertIsDisplayed()
            .assertIsEnabled()

        compose
            .onNodeWithTag(
                VexStreamMusicScreen
                    .ShuffleQueue
                    .elementId,
            )
            .assertIsDisplayed()
            .assertIsEnabled()

        compose
            .onNodeWithTag(
                VexStreamMusicScreen
                    .ShowQueue
                    .elementId,
            )
            .assertIsDisplayed()
            .assertIsEnabled()

        assertEquals(
            listOf(
                VexStreamMusicScreen
                    .TogglePlayback,
            ),
            activated,
        )
    }
}

// [VXG RealForever]
