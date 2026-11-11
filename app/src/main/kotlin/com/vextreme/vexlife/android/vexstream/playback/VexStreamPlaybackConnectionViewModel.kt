package com.vextreme.vexlife.android.vexstream.playback

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import com.vextreme.vexlife.android.vexstream.VexStreamMusicViewModel

/**
 * Presentation read access to the configuration-owned MediaController.
 *
 * Semantic Playback meaning no longer lives in this helper or in a controller-local semantic
 * Connection. Payload/read boundaries such as progress and Seek may still consume the controller.
 */
@Composable
internal fun rememberVexStreamMediaController():
    MediaController? {
    val owner:
        VexStreamMusicViewModel =
        viewModel()

    return owner.controller
}

// [VXG RealForever][VEXSTREAM][PLAYBACK][CONTROLLER-READ]
