package com.vextreme.vexlife.android.screens.shared.vexstream

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.ui.VexLifeTheme

/**
 * Semantic visual case: vexstream.player.playing
 *
 * The player domain/session is not re-driven here. The golden starts from an already-projected
 * playing state and proves only the presentation layer: title/artist/progress/queue position,
 * transport availability and the playing -> Pause visual affordance.
 */
@PreviewTest
@Preview(
    name = "VexStream · player playing · baseline",
    widthDp = 412,
    heightDp = 220,
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = false,
)
@Composable
fun vexStreamPlayingMiniPlayerGolden() {
    VexLifeTheme(
        darkTheme = true,
    ) {
        Surface {
            VexStreamMiniPlayerContent(
                playback =
                    VexStreamPlaybackUiState(
                        currentMediaId = "preview.streamed",
                        currentTitle = "VexStream Mobile",
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
                    ElementActionHandler {
                        // Golden capture has no side effects. Semantic action behavior is proved
                        // independently by Journey/runtime and Media3 ownership.
                    },
                shuffleSelected = false,
                onSeek = {},
            )
        }
    }
}


/**
 * Semantic visual case: vexstream.player.paused
 *
 * Same presentation owner as the Playing golden, but with pause truth projected to the Play
 * affordance. Playback/session behavior remains outside this pixel contract.
 */
@PreviewTest
@Preview(
    name = "VexStream · player paused · baseline",
    widthDp = 412,
    heightDp = 220,
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = false,
)
@Composable
fun vexStreamPausedMiniPlayerGolden() {
    VexLifeTheme(
        darkTheme = true,
    ) {
        Surface {
            VexStreamMiniPlayerContent(
                playback =
                    VexStreamPlaybackUiState(
                        currentMediaId = "preview.streamed",
                        currentTitle = "VexStream Mobile",
                        currentArtist = "Vex",
                        isPlaying = false,
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
                    ElementActionHandler {
                        // Golden capture has no side effects.
                    },
                shuffleSelected = false,
                onSeek = {},
            )
        }
    }
}
