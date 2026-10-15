package com.vextreme.vexlife.android.screens.shared.vexstream

import com.vextreme.vexlife.android.data.vexstream.VexStreamTrackRef
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
                trackRef,
            )
        val second =
            VexStreamPresentationRefs.trackRow(
                trackRef,
            )

        assertEquals(first, second)
        assertTrue(
            first.contains(trackRef.value),
        )
    }

    @Test
    fun differentOpaqueTrackRefsCannotCollapseToOneRowAddress() {
        val left =
            VexStreamPresentationRefs.trackRow(
                VexStreamTrackRef("a:1:b"),
            )
        val right =
            VexStreamPresentationRefs.trackRow(
                VexStreamTrackRef("a"),
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
                trackRef = trackRef,
                action = VexStreamMusicScreen.PlayTrack,
            )

        assertTrue(
            ref.startsWith(
                VexStreamMusicScreen.PlayTrack.elementId + ".dynamic.",
            ),
        )
        assertTrue(
            ref.endsWith(trackRef.value),
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
                trackRef = trackRef,
                action = VexStreamMusicScreen.PlayTrack,
            )
        val menu =
            VexStreamPresentationRefs.trackMenuAction(
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

}

// [VXG RealForever]
