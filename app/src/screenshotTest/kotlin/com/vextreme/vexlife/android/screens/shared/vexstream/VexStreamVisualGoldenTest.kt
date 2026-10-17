package com.vextreme.vexlife.android.screens.shared.vexstream

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.runtime.AppActionRegistrations
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreens
import com.vextreme.vexlife.android.ui.VexLifeTheme

/**
 * Semantic visual case: vexstream.library.songs
 *
 * The golden begins at the extracted library presentation seam instead of instantiating the whole
 * Screen. Repository track truth is feature-owned fixture data; playback/source capability is
 * deliberately unavailable, so Layoutlib is never asked to imitate Android media/file ownership.
 */
@PreviewTest
@Preview(
    name = "VexStream · songs library · baseline",
    widthDp = 412,
    heightDp = 915,
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = false,
)
@Composable
fun vexStreamSongsLibraryGolden() {
    val state =
        remember {
            vexStreamPreviewState()
        }
    val repository =
        remember {
            InMemoryVexStreamMusicRepository(
                initialState = state,
            )
        }
    val runtime =
        remember {
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

    VexLifeTheme(
        darkTheme = true,
    ) {
        Surface {
            VexStreamSongsPane(
                runtime = runtime,
                tracks = state.playableTracks(),
                repository = repository,
                controller = null,
                resolver = null,
                onVisitArtist = {},
            )
        }
    }
}

// [VXG RealForever]
