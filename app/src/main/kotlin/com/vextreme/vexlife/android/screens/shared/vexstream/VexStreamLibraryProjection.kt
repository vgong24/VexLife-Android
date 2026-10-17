package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState

/**
 * Read-only library projection for VexStream Android presentation.
 *
 * Repository state remains authoritative. This projection answers only which tracks can currently
 * be presented, which match local search text, and which form the Downloads view.
 */
internal data class VexStreamLibraryProjection(
    val playableTracks: List<VexStreamTrackState>,
    val searchResults: List<VexStreamTrackState>,
)

internal fun VexStreamMusicState.toVexStreamLibraryProjection(
    searchQuery: String,
): VexStreamLibraryProjection {
    val playable =
        playableTracks()

    return VexStreamLibraryProjection(
        playableTracks = playable,
        searchResults =
            playable.filter {
                it.matchesVexStreamSearch(
                    searchQuery,
                )
            },
    )
}

internal fun List<VexStreamTrackState>.vexStreamSortedSongs():
    List<VexStreamTrackState> =
    sortedBy {
        it.track.title.lowercase()
    }

internal fun List<VexStreamTrackState>.vexStreamSortedDownloads():
    List<VexStreamTrackState> =
    filter {
        it.localCopy is
            VexStreamLocalCopyState.Downloaded
    }.sortedBy {
        it.track.title.lowercase()
    }

private fun VexStreamTrackState.matchesVexStreamSearch(
    query: String,
): Boolean {
    val normalized =
        query.trim()
    if (normalized.isBlank()) {
        return true
    }

    return sequenceOf(
        track.title,
        track.artist,
        track.album,
        track.genre,
        track.year,
    ).any {
        it.contains(
            normalized,
            ignoreCase = true,
        )
    }
}

// [VXG RealForever]
