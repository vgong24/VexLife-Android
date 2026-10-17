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
 * The golden reuses the feature-owned paired preview state. It proves the current Android library
 * presentation after extraction; repository/network/Media3 behavior remains outside this pixel
 * contract.
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
        id = "vexstream-library-golden",
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
