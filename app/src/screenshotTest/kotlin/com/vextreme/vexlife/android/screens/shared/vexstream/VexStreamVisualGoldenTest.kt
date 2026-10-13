package com.vextreme.vexlife.android.screens.shared.vexstream

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.vextreme.vexlife.android.data.vexstream.InMemoryVexStreamMusicRepository
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexPreviewRepositories

/**
 * Semantic visual case: vexstream.library.paired
 *
 * This is intentionally a controlled visual contract, not a Journey and not a playback-domain test.
 * The feature-owned preview fixture supplies known repository truth; this case proves the approved
 * rendered result at one fixed render configuration.
 *
 * System UI is excluded so host clock/status-bar changes cannot become golden input.
 */
@PreviewTest
@Preview(
    name = "VexStream · paired library · baseline",
    widthDp = 412,
    heightDp = 915,
    fontScale = 1.0f,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = false,
)
@Composable
fun vexStreamPairedLibraryGolden() {
    VexPreviewHost(
        id = "golden-vexstream-library-paired",
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
            repository = context.repositories.vexStream,
        )
    }
}

// [VXG RealForever]
