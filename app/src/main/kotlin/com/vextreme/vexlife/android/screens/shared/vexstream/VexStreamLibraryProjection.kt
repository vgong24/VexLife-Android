package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackAvailability
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState

/**
 * Human-selected ordering for one current VexStream library projection.
 *
 * Sorting changes traversal/presentation order only. It never changes catalog or collection
 * membership.
 */
internal enum class VexStreamLibrarySort {
    TITLE,
    ARTIST,
    ALBUM,
    YEAR,
    DURATION,
}

/**
 * Numeric duration predicate for the current library listening set.
 *
 * Unknown duration is represented by durationSeconds == 0.0 and deliberately does not match an
 * active threshold in either direction.
 */
internal enum class VexStreamDurationComparison {
    ANY,
    LESS_THAN,
    GREATER_THAN,
}

/**
 * Screen-local organization choices for the current VexStream library.
 *
 * Empty categorical sets mean "any". The repository remains the owner of track/catalog truth; this
 * value is only a read-only lens over current playable tracks.
 */
internal data class VexStreamLibraryOrganization(
    val sort: VexStreamLibrarySort = VexStreamLibrarySort.TITLE,
    val durationComparison: VexStreamDurationComparison =
        VexStreamDurationComparison.ANY,
    val durationThresholdMinutes: Int = 30,
    val artists: Set<String> = emptySet(),
    val genres: Set<String> = emptySet(),
    val availability: Set<VexStreamTrackAvailability> = emptySet(),
) {
    init {
        require(durationThresholdMinutes > 0) {
            "duration threshold minutes must be positive"
        }
    }

    val hasActiveFilters: Boolean
        get() =
            durationComparison != VexStreamDurationComparison.ANY ||
                artists.isNotEmpty() ||
                genres.isNotEmpty() ||
                availability.isNotEmpty()

    fun clearFilters(): VexStreamLibraryOrganization =
        copy(
            durationComparison = VexStreamDurationComparison.ANY,
            artists = emptySet(),
            genres = emptySet(),
            availability = emptySet(),
        )

    fun toggleArtist(
        artist: String,
    ): VexStreamLibraryOrganization =
        copy(
            artists =
                artists.toggled(
                    artist,
                ),
        )

    fun toggleGenre(
        genre: String,
    ): VexStreamLibraryOrganization =
        copy(
            genres =
                genres.toggled(
                    genre,
                ),
        )

    fun toggleAvailability(
        value: VexStreamTrackAvailability,
    ): VexStreamLibraryOrganization =
        copy(
            availability =
                availability.toggled(
                    value,
                ),
        )
}

internal data class VexStreamLibraryOrganizationOptions(
    val artists: List<String>,
    val genres: List<String>,
    val availability: List<VexStreamTrackAvailability>,
)

/**
 * Read-only library projection for VexStream Android presentation.
 *
 * Repository state remains authoritative. The current organization lens first narrows playable
 * tracks, then orders that exact listening set, and Search evaluates inside the same set.
 */
internal data class VexStreamLibraryProjection(
    val playableTracks: List<VexStreamTrackState>,
    val searchResults: List<VexStreamTrackState>,
)

internal fun VexStreamMusicState.toVexStreamLibraryProjection(
    searchQuery: String,
    organization: VexStreamLibraryOrganization =
        VexStreamLibraryOrganization(),
): VexStreamLibraryProjection {
    val organized =
        playableTracks()
            .filter {
                it.matchesVexStreamOrganization(
                    organization,
                )
            }
            .vexStreamSortedSongs(
                organization.sort,
            )

    return VexStreamLibraryProjection(
        playableTracks = organized,
        searchResults =
            organized.filter {
                it.matchesVexStreamSearch(
                    searchQuery,
                )
            },
    )
}

internal fun VexStreamMusicState.toVexStreamLibraryOrganizationOptions():
    VexStreamLibraryOrganizationOptions {
    val playable =
        playableTracks()

    return VexStreamLibraryOrganizationOptions(
        artists =
            playable
                .map {
                    it.track.artist.trim()
                }
                .filter(String::isNotBlank)
                .distinctBy(String::lowercase)
                .sortedBy(String::lowercase),
        genres =
            playable
                .map {
                    it.track.genre.trim()
                }
                .filter(String::isNotBlank)
                .distinctBy(String::lowercase)
                .sortedBy(String::lowercase),
        availability =
            playable
                .map {
                    it.availability
                }
                .distinct()
                .sortedBy {
                    it.ordinal
                },
    )
}

internal fun List<VexStreamTrackState>.vexStreamSortedSongs(
    sort: VexStreamLibrarySort = VexStreamLibrarySort.TITLE,
): List<VexStreamTrackState> =
    when (sort) {
        VexStreamLibrarySort.TITLE ->
            sortedWith(
                compareBy<VexStreamTrackState> {
                    it.track.title.lowercase()
                }.thenBy {
                    it.track.artist.lowercase()
                },
            )

        VexStreamLibrarySort.ARTIST ->
            sortedWith(
                compareBy<VexStreamTrackState> {
                    it.track.artist.lowercase()
                }.thenBy {
                    it.track.title.lowercase()
                },
            )

        VexStreamLibrarySort.ALBUM ->
            sortedWith(
                compareBy<VexStreamTrackState> {
                    it.track.album.lowercase()
                }.thenBy {
                    it.track.title.lowercase()
                },
            )

        VexStreamLibrarySort.YEAR ->
            sortedWith(
                compareBy<VexStreamTrackState> {
                    it.track.year.toIntOrNull()
                        ?: Int.MAX_VALUE
                }.thenBy {
                    it.track.title.lowercase()
                },
            )

        VexStreamLibrarySort.DURATION ->
            sortedWith(
                compareBy<VexStreamTrackState> {
                    it.track.durationSeconds
                        .takeIf { duration ->
                            duration > 0.0
                        }
                        ?: Double.POSITIVE_INFINITY
                }.thenBy {
                    it.track.title.lowercase()
                },
            )
    }

internal fun List<VexStreamTrackState>.vexStreamSortedDownloads(
    sort: VexStreamLibrarySort = VexStreamLibrarySort.TITLE,
): List<VexStreamTrackState> =
    filter {
        it.localCopy is
            VexStreamLocalCopyState.Downloaded
    }.vexStreamSortedSongs(
        sort,
    )

private fun VexStreamTrackState.matchesVexStreamOrganization(
    organization: VexStreamLibraryOrganization,
): Boolean {
    val durationMatches =
        when (organization.durationComparison) {
            VexStreamDurationComparison.ANY ->
                true

            VexStreamDurationComparison.LESS_THAN ->
                track.durationSeconds > 0.0 &&
                    track.durationSeconds <
                    organization.durationThresholdMinutes * 60.0

            VexStreamDurationComparison.GREATER_THAN ->
                track.durationSeconds > 0.0 &&
                    track.durationSeconds >
                    organization.durationThresholdMinutes * 60.0
        }

    return durationMatches &&
        (
            organization.artists.isEmpty() ||
                organization.artists.any {
                    it.equals(
                        track.artist,
                        ignoreCase = true,
                    )
                }
        ) &&
        (
            organization.genres.isEmpty() ||
                organization.genres.any {
                    it.equals(
                        track.genre,
                        ignoreCase = true,
                    )
                }
        ) &&
        (
            organization.availability.isEmpty() ||
                track.availability in
                organization.availability
        )
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

private fun <T> Set<T>.toggled(
    value: T,
): Set<T> =
    if (value in this) {
        this - value
    } else {
        this + value
    }

// [VXG RealForever]
