package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamArtworkStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import java.io.File

/**
 * Resolves one view-facing artwork model without making presentation an artwork owner.
 *
 * A verified downloaded track prefers its durable app-owned companion file. Remote HTTPS artwork
 * remains the fallback while reachable. Missing artwork remains an ordinary placeholder condition.
 */
internal fun VexStreamTrackState.vexStreamArtworkModel(
    contextFilesDir: File,
): Any? {
    val localArtwork =
        VexStreamArtworkStore
            .production(
                contextFilesDir,
            )
            .fileFor(
                track,
            )

    return localArtwork
        ?: track.artworkUrl
}

// [VXG RealForever]
