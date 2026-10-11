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
import androidx.compose.ui.platform.testTag
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
 * - the list itself has one stable semantic ref/test tag;
 * - current visible item refs are projected as a Set for relationships such as anchored attention;
 * - item meaning, actions, row layout, repositories, ordering policy, and presentation eligibility
 *   remain with the feature that supplied [items] and [itemContent].
 *
 * The first real consumer is Events. A future Messaging/Maps/etc. surface may reuse this only when
 * it has the same repeated-identity problem; this is not a generic domain-list abstraction.
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
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(12.dp),
    onVisibleItemRefsChanged: (Set<String>) -> Unit = {},
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
            listState.animateScrollToItem(index)
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
            modifier.testTag(listRef),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
    ) {
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
