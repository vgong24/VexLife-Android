package com.vextreme.vexlife.android.elements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VexStableLazyListTest {
    @Test
    fun stableItemRefsPreserveSemanticIdentityInsteadOfPosition() {
        data class Item(
            val ref: String,
            val value: String,
        )

        val first =
            listOf(
                Item("event.a", "A"),
                Item("event.b", "B"),
            )
        val reordered =
            listOf(
                Item("event.b", "B changed"),
                Item("event.a", "A"),
            )

        assertEquals(
            setOf("event.a", "event.b"),
            stableItemRefs(first, Item::ref),
        )
        assertEquals(
            setOf("event.a", "event.b"),
            stableItemRefs(reordered, Item::ref),
        )
    }

    @Test
    fun duplicateStableRefsFailClosed() {
        val failure =
            runCatching {
                stableItemRefs(
                    items =
                        listOf(
                            "event.a",
                            "event.a",
                        ),
                    itemRef = { it },
                )
            }.exceptionOrNull()

        requireNotNull(failure)
        assertTrue(
            failure.message.orEmpty()
                .contains("must be unique"),
        )
    }
}

// [VXG RealForever]
