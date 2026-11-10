package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamArtworkStore
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState
import java.io.File

/**
 * Resolves one view-facing artwork model without making presentation an artwork owner.
 *
 * A verified downloaded track prefers its durable app-owned companion file. Remote HTTPS artwork
 * remains the fallback while reachable. Missing artwork remains an ordinary placeholder condition.
 *
 * [contextFilesDir] is nullable because presentation may execute in a render/inspection environment
 * that cannot expose app-private filesystem custody. Absence of that outside source means only that
 * durable local artwork cannot be consulted; it does not make the track or its presentation invalid.
 */
internal fun VexStreamTrackState.vexStreamArtworkModel(
    contextFilesDir: File?,
): Any? {
    val localArtwork =
        contextFilesDir
            ?.let { filesDir ->
                VexStreamArtworkStore
                    .production(
                        filesDir,
                    )
                    .fileFor(
                        track,
                    )
            }

    return localArtwork
        ?: track.artworkUrl
}

// [VXG RealForever]
