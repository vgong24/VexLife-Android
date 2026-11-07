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
    fun headerOffsetsLazyPositionWithoutEnteringFeatureItemIdentity() {
        assertEquals(
            0,
            stableLazyItemIndex(
                itemIndex = 0,
                hasHeader = false,
            ),
        )
        assertEquals(
            1,
            stableLazyItemIndex(
                itemIndex = 0,
                hasHeader = true,
            ),
        )
        assertEquals(
            7,
            stableLazyItemIndex(
                itemIndex = 6,
                hasHeader = true,
            ),
        )
        assertEquals(
            "element.screen.body.list.header",
            stableListHeaderKey(
                listRef = "element.screen.body.list",
                itemRefs =
                    setOf(
                        "item.a",
                        "item.b",
                    ),
            ),
        )
    }

    @Test
    fun headerKeyCollisionWithFeatureItemFailsClosed() {
        val failure =
            runCatching {
                stableListHeaderKey(
                    listRef =
                        "element.screen.body.list",
                    itemRefs =
                        setOf(
                            "element.screen.body.list.header",
                        ),
                )
            }.exceptionOrNull()

        requireNotNull(failure)
        assertTrue(
            failure.message.orEmpty()
                .contains("collides"),
        )
    }

    @Test
    fun negativeStableLazyItemIndexFailsClosed() {
        val failure =
            runCatching {
                stableLazyItemIndex(
                    itemIndex = -1,
                    hasHeader = true,
                )
            }.exceptionOrNull()

        requireNotNull(failure)
        assertTrue(
            failure.message.orEmpty()
                .contains(">= 0"),
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
