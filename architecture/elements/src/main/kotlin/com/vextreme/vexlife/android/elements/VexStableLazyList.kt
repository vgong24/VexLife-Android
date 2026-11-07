package com.vextreme.vexlife.android.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Reusable Android-native list realization for runtime-data items with stable semantic identity.
 *
 * This component deliberately owns only mechanics that every dynamic list should not have to
 * rediscover:
 *
 * - one stable semantic ref per item becomes the Compose lazy-list key;
 * - callers may supply [contentType] so Compose only reuses item compositions across compatible
 *   row shapes;
 * - an optional [requestedVisibleItemRef] lets a scenario or feature bring one stable item into the
 *   viewport without exposing visual list position as product identity;
 * - [userScrollEnabled] supports an explicit blocking presentation that locks the already-targeted
 *   list projection until its owning feature releases the condition;
 * - the list itself has one stable semantic ref/test tag;
 * - current visible item refs are projected as a Set for relationships such as anchored attention;
 * - an optional [headerContent] scrolls with the list ahead of feature-owned items without becoming
 *   one of those items or entering the visible-item projection;
 * - item meaning, actions, row layout, repositories, ordering policy, and presentation eligibility
 *   remain with the feature that supplied [items] and [itemContent].
 *
 * Real consumers include Events (visibility/anchored-attention pressure), Family Conversation
 * (runtime messages), VexStream Search (runtime track identity/QA addressing), and VexStream library
 * panels that preserve in-list navigation before stable repeated rows. Their different domain meaning
 * earns this as stable repeated-item mechanics rather than a feature-specific list abstraction.
 *
 * A downstream projector can therefore recover one truthful repeated-item relationship from the same
 * source contract without registering runtime rows as fake static Screen elements.
 *
 * Useful torture conditions for a caller:
 *
 * 1. insert/reorder items while keeping the same item ref;
 * 2. scroll a selected item off/on screen and observe [onVisibleItemRefsChanged];
 * 3. change font/locale/viewport while keys remain stable;
 * 4. ensure duplicate refs fail immediately instead of becoming ambiguous Compose keys.
 */
@Composable
fun <Item> VexStableLazyColumn(
    listRef: String,
    items: List<Item>,
    itemRef: (Item) -> String,
    contentType: (Item) -> Any? = { null },
    requestedVisibleItemRef: String? = null,
    userScrollEnabled: Boolean = true,
    modifier: Modifier = VexModifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(12.dp),
    onVisibleItemRefsChanged: (Set<String>) -> Unit = {},
    headerContent: (@Composable LazyItemScope.() -> Unit)? = null,
    emptyContent: @Composable LazyItemScope.() -> Unit = {},
    itemContent: @Composable LazyItemScope.(Item) -> Unit,
) {
    require(listRef.isNotBlank()) {
        "stable list ref must not be blank"
    }

    val refs =
        stableItemRefs(
            items = items,
            itemRef = itemRef,
        )
    val headerKey =
        headerContent?.let {
            stableListHeaderKey(
                listRef = listRef,
                itemRefs = refs,
            )
        }
    val listState =
        rememberLazyListState()
    val currentVisibleCallback by
        rememberUpdatedState(onVisibleItemRefsChanged)
    val currentRefs by
        rememberUpdatedState(refs)

    /*
     * Keep one observer alive for the lifetime of this LazyListState. Provider snapshots may add
     * rows every few seconds; restarting this collector for every ref-set change can expose a brief
     * pre/post-layout visibility gap and blink an otherwise-stable anchored candidate.
     */
    LaunchedEffect(
        listState,
        requestedVisibleItemRef,
        refs,
    ) {
        val requested =
            requestedVisibleItemRef
                ?: return@LaunchedEffect

        val index =
            items.indexOfFirst { item ->
                itemRef(item) == requested
            }

        if (index >= 0) {
            listState.animateScrollToItem(
                stableLazyItemIndex(
                    itemIndex = index,
                    hasHeader = headerKey != null,
                ),
            )
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { item ->
                    item.key as? String
                }
                .toSet()
                .intersect(currentRefs)
        }
            .distinctUntilChanged()
            .collect { visibleRefs ->
                currentVisibleCallback(visibleRefs)
            }
    }

    LazyColumn(
        state = listState,
        modifier =
            modifier.vex(
listRef,
),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        userScrollEnabled = userScrollEnabled,
    ) {
        if (
            headerContent != null &&
            headerKey != null
        ) {
            item(
                key = headerKey,
                content = headerContent,
            )
        }

        if (items.isEmpty()) {
            item(
                key = "$listRef.empty",
                content = emptyContent,
            )
        } else {
            items(
                items = items,
                key = itemRef,
                contentType = contentType,
                itemContent = itemContent,
            )
        }
    }
}

/**
 * Lazy-list position of one feature item when an optional non-item header precedes the item set.
 */
internal fun stableLazyItemIndex(
    itemIndex: Int,
    hasHeader: Boolean,
): Int {
    require(itemIndex >= 0) {
        "stable list item index must be >= 0"
    }

    return itemIndex +
        if (hasHeader) {
            1
        } else {
            0
        }
}

/**
 * Compose-only key for the optional list header.
 *
 * Feature-owned item refs remain sovereign. If one already occupies the reserved derived key, fail
 * closed instead of silently creating duplicate LazyColumn keys.
 */
internal fun stableListHeaderKey(
    listRef: String,
    itemRefs: Set<String>,
): String {
    require(listRef.isNotBlank()) {
        "stable list ref must not be blank"
    }

    val key = "$listRef.header"
    require(key !in itemRefs) {
        "stable list header key collides with feature item ref: $key"
    }
    return key
}

internal fun <Item> stableItemRefs(
    items: List<Item>,
    itemRef: (Item) -> String,
): Set<String> {
    val refs =
        items.map { item ->
            itemRef(item).also { ref ->
                require(ref.isNotBlank()) {
                    "stable list item ref must not be blank"
                }
            }
        }

    require(refs.distinct().size == refs.size) {
        "stable list item refs must be unique: " +
            refs.groupingBy { it }
                .eachCount()
                .filterValues { count -> count > 1 }
                .keys
    }

    return refs.toSet()
}

// [VXG RealForever]
