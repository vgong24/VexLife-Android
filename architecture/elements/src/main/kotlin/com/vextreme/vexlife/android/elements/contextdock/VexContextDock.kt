package com.vextreme.vexlife.android.elements.contextdock

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Stable semantic identity for one contextual page.
 *
 * Page order is presentation. Consumers should remember this identity rather than a pager index so a
 * page may appear/disappear (for example Now Playing) without changing the meaning of another page.
 */
@JvmInline
value class VexContextDockPageRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "context dock page ref must not be blank" }
    }
}

/**
 * One feature-owned projection admitted into a host-owned [VexContextDock].
 *
 * The dock owns paging and peek/expanded mechanics only. The contributing feature continues to own
 * the page's state, actions, and meaning.
 */
class VexContextDockPage(
    val pageRef: VexContextDockPageRef,
    val content: @Composable (expanded: Boolean) -> Unit,
)

fun resolveVexContextDockPageIndex(
    pageRefs: List<VexContextDockPageRef>,
    preferredPageRef: VexContextDockPageRef?,
): Int {
    if (pageRefs.isEmpty()) return 0

    return preferredPageRef
        ?.let(pageRefs::indexOf)
        ?.takeIf { it >= 0 }
        ?: 0
}

/**
 * Reusable contextual surface with two independent axes:
 *
 * horizontal swipe -> which feature/context is being projected?
 * vertical drag    -> how much of that context should be revealed?
 *
 * Hosting this component does not transfer semantic ownership into the host. Pages remain
 * feature-owned projections.
 */
@Composable
fun VexContextDock(
    pages: List<VexContextDockPage>,
    preferredPageRef: VexContextDockPageRef? = null,
    onSelectedPageChanged: (VexContextDockPageRef) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (pages.isEmpty()) {
        return
    }

    val pageRefs =
        remember(pages) {
            pages.map(VexContextDockPage::pageRef)
        }
    val initialPage =
        resolveVexContextDockPageIndex(
            pageRefs = pageRefs,
            preferredPageRef = preferredPageRef,
        )
    val pagerState =
        rememberPagerState(
            initialPage = initialPage,
            pageCount = { pages.size },
        )

    var expanded by remember {
        mutableStateOf(false)
    }
    var verticalDrag by remember {
        mutableFloatStateOf(0f)
    }
    val dragThresholdPx =
        with(LocalDensity.current) {
            28.dp.toPx()
        }

    LaunchedEffect(
        pageRefs,
        preferredPageRef,
    ) {
        val target =
            resolveVexContextDockPageIndex(
                pageRefs = pageRefs,
                preferredPageRef = preferredPageRef,
            )
        if (
            target in pages.indices &&
            target != pagerState.settledPage &&
            !pagerState.isScrollInProgress
        ) {
            pagerState.scrollToPage(target)
        }
    }

    LaunchedEffect(
        pagerState,
        pageRefs,
    ) {
        snapshotFlow {
            pagerState.settledPage to
                pagerState.isScrollInProgress
        }
            .distinctUntilChanged()
            .collect { (pageIndex, scrolling) ->
                if (
                    !scrolling &&
                    pageIndex in pageRefs.indices
                ) {
                    onSelectedPageChanged(
                        pageRefs[pageIndex],
                    )
                }
            }
    }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize()
                .pointerInput(
                    pages,
                    expanded,
                ) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            verticalDrag = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            verticalDrag += dragAmount
                        },
                        onDragEnd = {
                            when {
                                verticalDrag <= -dragThresholdPx ->
                                    expanded = true

                                verticalDrag >= dragThresholdPx ->
                                    expanded = false
                            }
                            verticalDrag = 0f
                        },
                        onDragCancel = {
                            verticalDrag = 0f
                        },
                    )
                },
        shape = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 18.dp,
            bottomEnd = 18.dp,
        ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = 82.dp,
                        max =
                            if (expanded) {
                                360.dp
                            } else {
                                142.dp
                            },
                    )
                    .padding(
                        start = 14.dp,
                        end = 14.dp,
                        bottom = 10.dp,
                    ),
            verticalArrangement =
                Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clickable {
                            expanded = !expanded
                        },
                contentAlignment =
                    Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(
                                width = 34.dp,
                                height = 4.dp,
                            )
                            .background(
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                                shape = CircleShape,
                            ),
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                key = { index ->
                    pages[index].pageRef.value
                },
            ) { index ->
                pages[index].content(expanded)
            }

            if (pages.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.Center,
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    pages.forEachIndexed { index, _ ->
                        Surface(
                            modifier =
                                Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(
                                        if (
                                            index ==
                                            pagerState.settledPage
                                        ) {
                                            7.dp
                                        } else {
                                            5.dp
                                        },
                                    ),
                            shape = CircleShape,
                            color =
                                if (
                                    index ==
                                    pagerState.settledPage
                                ) {
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .outlineVariant
                                },
                        ) {}
                    }
                }
            }
        }
    }
}

// [VXG RealForever]
