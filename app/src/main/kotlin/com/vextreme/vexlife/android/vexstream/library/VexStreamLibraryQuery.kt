package com.vextreme.vexlife.android.vexstream.library

import com.vextreme.vexlife.android.data.vexstream.VexStreamLocalCopyState
import com.vextreme.vexlife.android.data.vexstream.VexStreamMusicState
import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackState

/**
 * Human question: which known music matches what someone asked for?
 *
 * This is the one VexStream library text-matching law. The human Search sheet and non-UI callers
 * (a Vex request such as "play Alright" or "play my downloads") consume the same law, so a track is
 * findable by one caller exactly when it is findable by the other.
 *
 * Repository state remains the only owner of library truth. This file owns no State, performs no
 * effect, and grants no playback capability: a match is identity for the existing Track /
 * Collection playback request path, which still re-resolves the source and re-checks provider
 * capability at activation.
 */
internal enum class VexStreamLibraryMatchField {
    Title,
    Artist,
    Album,
    Genre,
    Year,
}

/** Declaration order is rank order: earlier is stronger. */
internal enum class VexStreamLibraryMatchStrength {
    Exact,
    Prefix,
    Contains,
}

/** Which current tracks a query may consider. Both scopes start from repository playable truth. */
internal enum class VexStreamLibraryScope {
    /** Every track with at least one current source (paired Hub and/or verified local copy). */
    Playable,

    /** Only tracks with a verified local copy on this phone ("my downloads"). */
    Downloaded,
}

/**
 * One track's strongest match for one query.
 *
 * Title outranks other fields at equal strength because a spoken or typed request names a song far
 * more often than a genre or year.
 */
internal data class VexStreamLibraryMatch(
    val track: VexStreamTrackState,
    val field: VexStreamLibraryMatchField,
    val strength: VexStreamLibraryMatchStrength,
)

/**
 * Ranked result of one library query.
 *
 * [best] holds every match that shares the strongest rank. [ambiguous] means more than one distinct
 * track is equally plausible ("Alright" by two artists): a caller must clarify rather than guess.
 */
internal data class VexStreamLibraryQueryResult(
    val query: String,
    val matches: List<VexStreamLibraryMatch>,
) {
    val best: List<VexStreamLibraryMatch> =
        matches
            .firstOrNull()
            ?.let { strongest ->
                matches.takeWhile {
                    it.strength ==
                        strongest.strength &&
                        it.field ==
                        strongest.field
                }
            }
            .orEmpty()

    val single: VexStreamTrackState? =
        best
            .singleOrNull()
            ?.track

    val ambiguous: Boolean
        get() =
            best.size > 1

    val isEmpty: Boolean
        get() =
            matches.isEmpty()
}

/**
 * Query current repository truth for [text] inside [scope].
 *
 * Blank text matches nothing here: an empty request is a question for the caller, not "everything".
 * Within one rank, tracks keep the repository's current order so results are deterministic.
 */
internal fun VexStreamMusicState.queryVexStreamLibrary(
    text: String,
    scope: VexStreamLibraryScope =
        VexStreamLibraryScope.Playable,
): VexStreamLibraryQueryResult {
    val normalized =
        text.trim()

    if (normalized.isEmpty()) {
        return VexStreamLibraryQueryResult(
            query =
                normalized,
            matches =
                emptyList(),
        )
    }

    return VexStreamLibraryQueryResult(
        query =
            normalized,
        matches =
            tracksIn(
                scope,
            )
                .mapNotNull {
                    it.vexStreamLibraryMatch(
                        normalized,
                    )
                }
                .sortedWith(
                    compareBy(
                        { it.strength.ordinal },
                        { it.field.ordinal },
                    ),
                ),
    )
}

/** Current tracks inside [scope], in repository order. */
internal fun VexStreamMusicState.tracksIn(
    scope: VexStreamLibraryScope,
): List<VexStreamTrackState> =
    when (
        scope
    ) {
        VexStreamLibraryScope.Playable ->
            playableTracks()

        VexStreamLibraryScope.Downloaded ->
            playableTracks()
                .filter {
                    it.localCopy is
                        VexStreamLocalCopyState.Downloaded
                }
    }

/**
 * The strongest match of this track for an already-trimmed, non-blank [normalizedQuery], or null.
 *
 * Membership law (unchanged from the original Search sheet): case-insensitive substring over title,
 * artist, album, genre and year. Strength only ranks members; it never adds or removes one.
 */
internal fun VexStreamTrackState.vexStreamLibraryMatch(
    normalizedQuery: String,
): VexStreamLibraryMatch? {
    require(
        normalizedQuery.isNotBlank() &&
            normalizedQuery ==
            normalizedQuery.trim(),
    ) {
        "library match requires a trimmed, non-blank query"
    }

    return sequenceOf(
        VexStreamLibraryMatchField.Title to track.title,
        VexStreamLibraryMatchField.Artist to track.artist,
        VexStreamLibraryMatchField.Album to track.album,
        VexStreamLibraryMatchField.Genre to track.genre,
        VexStreamLibraryMatchField.Year to track.year,
    )
        .mapNotNull { (field, value) ->
            value
                .matchStrength(
                    normalizedQuery,
                )
                ?.let { strength ->
                    VexStreamLibraryMatch(
                        track =
                            this,
                        field =
                            field,
                        strength =
                            strength,
                    )
                }
        }
        .minWithOrNull(
            compareBy(
                { it.strength.ordinal },
                { it.field.ordinal },
            ),
        )
}

private fun String.matchStrength(
    normalizedQuery: String,
): VexStreamLibraryMatchStrength? =
    when {
        equals(
            normalizedQuery,
            ignoreCase = true,
        ) ->
            VexStreamLibraryMatchStrength.Exact

        startsWith(
            normalizedQuery,
            ignoreCase = true,
        ) ->
            VexStreamLibraryMatchStrength.Prefix

        contains(
            normalizedQuery,
            ignoreCase = true,
        ) ->
            VexStreamLibraryMatchStrength.Contains

        else ->
            null
    }

// [VXG RealForever][VEXSTREAM][LIBRARY][QUERY]
