package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
import com.vextreme.vexlife.android.vexstream.queue.VexStreamQueueControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStreamPresentationRefsTest {
    @Test
    fun trackRowIdentityFollowsTrackRefNotPresentationTextOrPosition() {
        val trackRef =
            VexStreamTrackRef(
                "catalog:/artist/album/track#1",
            )

        val first =
            VexStreamPresentationRefs.trackRow(
                scope = VexStreamTrackPresentationScope.Songs,
                trackRef = trackRef,
            )
        val second =
            VexStreamPresentationRefs.trackRow(
                scope = VexStreamTrackPresentationScope.Songs,
                trackRef = trackRef,
            )

        assertEquals(first, second)
        assertTrue(
            first.startsWith(
                "element.vexstream-music.track.dynamic.utf8hex.",
            ),
        )
        assertTrue(
            first.endsWith(
                trackRef.value.utf8Hex(),
            ),
        )
    }

    @Test
    fun sameTrackInDifferentPresentationScopesHasDifferentAddress() {
        val trackRef =
            VexStreamTrackRef(
                "journey.track.one",
            )

        val songs =
            VexStreamPresentationRefs.trackRow(
                scope = VexStreamTrackPresentationScope.Songs,
                trackRef = trackRef,
            )
        val search =
            VexStreamPresentationRefs.trackRow(
                scope = VexStreamTrackPresentationScope.Search,
                trackRef = trackRef,
            )

        assertNotEquals(
            songs,
            search,
        )
    }

    @Test
    fun differentOpaqueTrackRefsCannotCollapseToOneRowAddress() {
        val left =
            VexStreamPresentationRefs.trackRow(
                scope = VexStreamTrackPresentationScope.Songs,
                trackRef = VexStreamTrackRef("a:1:b"),
            )
        val right =
            VexStreamPresentationRefs.trackRow(
                scope = VexStreamTrackPresentationScope.Songs,
                trackRef = VexStreamTrackRef("a"),
            )

        assertNotEquals(left, right)
    }
    @Test
    fun trackActionKeepsStaticActionOwnerAndAddsExactRuntimeTrack() {
        val trackRef =
            VexStreamTrackRef(
                "journey.track.one",
            )

        val ref =
            VexStreamPresentationRefs.trackRowAction(
                scope = VexStreamTrackPresentationScope.Songs,
                trackRef = trackRef,
                action = VexStreamMusicScreen.PlayTrack,
            )

        assertTrue(
            ref.startsWith(
                VexStreamMusicScreen.PlayTrack.elementId +
                    ".row.dynamic.utf8hex.",
            ),
        )
        assertTrue(
            ref.endsWith(
                trackRef.value.utf8Hex(),
            ),
        )
    }

    @Test
    fun collectionOpenKeepsStaticActionMeaningAndExactDynamicCollectionKey() {
        val collectionKey =
            "album:Journey Album"

        val row =
            VexStreamPresentationRefs.collectionRow(
                collectionKey,
            )
        val action =
            VexStreamPresentationRefs.collectionRowAction(
                collectionKey = collectionKey,
                action = VexStreamMusicScreen.OpenCollection,
            )

        assertTrue(
            row.startsWith(
                "element.vexstream-music.collection.dynamic.utf8hex.",
            ),
        )
        assertTrue(
            action.startsWith(
                VexStreamMusicScreen.OpenCollection.elementId +
                    ".collection-row.dynamic.utf8hex.",
            ),
        )
        assertTrue(
            row.endsWith(
                collectionKey.utf8Hex(),
            ),
        )
        assertTrue(
            action.endsWith(
                collectionKey.utf8Hex(),
            ),
        )
    }

    @Test
    fun queueOccurrenceIdentityIsExplicitAndOpaqueSafe() {
        val stableKey =
            VexStreamPresentationRefs.queueEntryKey(
                mediaId = "catalog:/track#one",
                occurrence = 2,
            )

        assertEquals(
            "catalog:/track#one#2",
            stableKey,
        )
        assertTrue(
            VexStreamPresentationRefs
                .queueEntry(stableKey)
                .endsWith(
                    stableKey.utf8Hex(),
                ),
        )
    }

    @Test
    fun queueActionKeepsStaticAuthorityAndExactOccurrence() {
        val stableKey =
            VexStreamPresentationRefs.queueEntryKey(
                mediaId = "journey.track.one",
                occurrence = 1,
            )
        val ref =
            VexStreamPresentationRefs.queueEntryAction(
                stableKey = stableKey,
                action = VexStreamMusicScreen.QueuePlay,
            )

        assertTrue(
            ref.startsWith(
                VexStreamMusicScreen.QueuePlay.elementId +
                    ".queue-entry.dynamic.utf8hex.",
            ),
        )
        assertTrue(
            ref.endsWith(
                stableKey.utf8Hex(),
            ),
        )
    }

    @Test
    fun fullPlayerControlAddressDistinguishesExpandedSurfaceFromMiniPlayer() {
        val ref =
            VexStreamPresentationRefs.fullPlayerAction(
                VexStreamMusicScreen.ShowQueue,
            )

        assertTrue(
            ref.startsWith(
                "element.vexstream-music.full-player.action.dynamic.utf8hex.",
            ),
        )
        assertTrue(
            ref.endsWith(
                VexStreamMusicScreen.ShowQueue.elementId.utf8Hex(),
            ),
        )
        assertNotEquals(
            VexStreamMusicScreen.ShowQueue.elementId,
            ref,
        )
    }

    @Test
    fun fullPlayerSwipeAddressKeepsQueueControlMeaningExplicit() {
        assertEquals(
            "presentation.vexstream.full-player.swipe.previous",
            VexStreamPresentationRefs.fullPlayerSwipe(
                VexStreamQueueControl.Previous,
            ),
        )
        assertEquals(
            "presentation.vexstream.full-player.swipe.next",
            VexStreamPresentationRefs.fullPlayerSwipe(
                VexStreamQueueControl.Next,
            ),
        )
    }

    @Test
    fun invalidQueueOccurrenceFailsClosed() {
        val failure =
            runCatching {
                VexStreamPresentationRefs.queueEntryKey(
                    mediaId = "journey.track.one",
                    occurrence = 0,
                )
            }.exceptionOrNull()

        requireNotNull(failure)
        assertTrue(
            failure.message.orEmpty()
                .contains("occurrence"),
        )
    }

    @Test
    fun rowAndMenuAffordancesRemainDistinctForTheSameTrackAction() {
        val trackRef =
            VexStreamTrackRef(
                "journey.track.one",
            )

        val row =
            VexStreamPresentationRefs.trackRowAction(
                scope = VexStreamTrackPresentationScope.Songs,
                trackRef = trackRef,
                action = VexStreamMusicScreen.PlayTrack,
            )
        val menu =
            VexStreamPresentationRefs.trackMenuAction(
                scope = VexStreamTrackPresentationScope.Songs,
                trackRef = trackRef,
                action = VexStreamMusicScreen.PlayTrack,
            )

        assertNotEquals(row, menu)
        assertTrue(
            row.startsWith(
                VexStreamMusicScreen.PlayTrack.elementId + ".row.dynamic.",
            ),
        )
        assertTrue(
            menu.startsWith(
                VexStreamMusicScreen.PlayTrack.elementId + ".menu.dynamic.",
            ),
        )
    }

    private fun String.utf8Hex(): String =
        encodeToByteArray()
            .joinToString(
                separator = "",
            ) { byte ->
                byte.toUByte()
                    .toString(16)
                    .padStart(
                        length = 2,
                        padChar = '0',
                    )
            }
}

// [VXG RealForever]
