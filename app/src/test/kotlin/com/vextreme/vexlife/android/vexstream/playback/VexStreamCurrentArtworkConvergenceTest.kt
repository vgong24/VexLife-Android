package com.vextreme.vexlife.android.vexstream.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class VexStreamCurrentArtworkConvergenceTest {
    @Test
    fun currentTrackWithNewArtworkUpdatesMetadata() {
        assertEquals(
            VexStreamCurrentArtworkCommitPlan.UPDATE,
            vexStreamCurrentArtworkCommitPlan(
                expectedMediaId =
                    "track.current",
                expectedIndex = 2,
                liveMediaId =
                    "track.current",
                liveIndex = 2,
                liveArtworkUri = null,
                desiredArtworkUri =
                    "file:///art/current",
                changeMediaItemsAvailable =
                    true,
            ),
        )
    }

    @Test
    fun currentTrackAlreadyShowingExactArtworkDoesNotReplaceItself() {
        assertEquals(
            VexStreamCurrentArtworkCommitPlan.ALREADY_CURRENT,
            vexStreamCurrentArtworkCommitPlan(
                expectedMediaId =
                    "track.current",
                expectedIndex = 2,
                liveMediaId =
                    "track.current",
                liveIndex = 2,
                liveArtworkUri =
                    "file:///art/current",
                desiredArtworkUri =
                    "file:///art/current",
                changeMediaItemsAvailable =
                    true,
            ),
        )
    }

    @Test
    fun lateArtworkFetchCannotMutateANewerCurrentTrack() {
        assertEquals(
            VexStreamCurrentArtworkCommitPlan.PLAYER_CHANGED,
            vexStreamCurrentArtworkCommitPlan(
                expectedMediaId =
                    "track.old",
                expectedIndex = 1,
                liveMediaId =
                    "track.new",
                liveIndex = 2,
                liveArtworkUri = null,
                desiredArtworkUri =
                    "file:///art/old",
                changeMediaItemsAvailable =
                    true,
            ),
        )
    }

    @Test
    fun widgetRefreshFollowsOnlyConvergedMedia3ArtworkTruth() {
        assertEquals(
            true,
            VexStreamCurrentArtworkConvergence
                .UPDATED
                .shouldRefreshWidgetProjection,
        )
        assertEquals(
            true,
            VexStreamCurrentArtworkConvergence
                .ALREADY_CURRENT
                .shouldRefreshWidgetProjection,
        )
        listOf(
            VexStreamCurrentArtworkConvergence.NO_CURRENT_ITEM,
            VexStreamCurrentArtworkConvergence.TRACK_NOT_FOUND,
            VexStreamCurrentArtworkConvergence.NO_ARTWORK_SOURCE,
            VexStreamCurrentArtworkConvergence.ARTWORK_UNAVAILABLE,
            VexStreamCurrentArtworkConvergence.PLAYER_CHANGED,
            VexStreamCurrentArtworkConvergence.COMMAND_UNAVAILABLE,
            VexStreamCurrentArtworkConvergence.CONTROLLER_UNAVAILABLE,
        ).forEach { result ->
            assertEquals(
                result.name,
                false,
                result.shouldRefreshWidgetProjection,
            )
        }
    }

    @Test
    fun providerCommandAvailabilityStillOwnsPlaylistMutation() {
        assertEquals(
            VexStreamCurrentArtworkCommitPlan.COMMAND_UNAVAILABLE,
            vexStreamCurrentArtworkCommitPlan(
                expectedMediaId =
                    "track.current",
                expectedIndex = 2,
                liveMediaId =
                    "track.current",
                liveIndex = 2,
                liveArtworkUri = null,
                desiredArtworkUri =
                    "file:///art/current",
                changeMediaItemsAvailable =
                    false,
            ),
        )
    }
}

// [VXG RealForever]
