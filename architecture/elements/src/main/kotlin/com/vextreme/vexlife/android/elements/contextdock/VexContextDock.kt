package com.vextreme.vexlife.android.elements.contextdock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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
 * How much of the selected page the dock reveals, lowest to highest.
 *
 * Reveal is presentation of a feature-owned page, never the feature's state: tucking the music page
 * does not pause music.
 */
enum class VexContextDockReveal {
    /** Dismissed to a slim handle so the stage behind is fully visible. */
    Tucked,

    /** One glanceable row: what is this, and its one most likely action. */
    Peek,

    /** The page's controls and the next few things. */
    Open,

    /** The page takes the foreground over the stage. */
    Full,
    ;

    /** Compatibility reading for pages written against the original two-level dock. */
    val isExpanded: Boolean
        get() = this >= Open
}

/**
 * Which reveal levels a host admits. A map with its own controls may forbid [VexContextDockReveal.Full];
 * a host with nothing behind the dock may forbid [VexContextDockReveal.Tucked].
 */
data class VexContextDockLevels(
    val allowTuck: Boolean,
    val allowFull: Boolean,
) {
    val admitted: List<VexContextDockReveal> =
        VexContextDockReveal.entries.filter { level ->
            when (level) {
                VexContextDockReveal.Tucked -> allowTuck
                VexContextDockReveal.Full -> allowFull
                VexContextDockReveal.Peek,
                VexContextDockReveal.Open,
                -> true
            }
        }

    fun coerce(
        reveal: VexContextDockReveal,
    ): VexContextDockReveal =
        when {
            reveal in admitted -> reveal
            reveal == VexContextDockReveal.Tucked -> VexContextDockReveal.Peek
            else -> VexContextDockReveal.Open
        }

    fun raised(
        from: VexContextDockReveal,
    ): VexContextDockReveal {
        val index = admitted.indexOf(coerce(from))
        return admitted[(index + 1).coerceAtMost(admitted.lastIndex)]
    }

    fun lowered(
        from: VexContextDockReveal,
    ): VexContextDockReveal {
        val index = admitted.indexOf(coerce(from))
        return admitted[(index - 1).coerceAtLeast(0)]
    }

    /**
     * Vertical drag law: a drag upward past [thresholdPx] raises one level, downward lowers one
     * level, anything smaller leaves the reveal unchanged. Negative [dragPx] is upward.
     */
    fun afterDrag(
        from: VexContextDockReveal,
        dragPx: Float,
        thresholdPx: Float,
    ): VexContextDockReveal =
        when {
            dragPx <= -thresholdPx -> raised(from)
            dragPx >= thresholdPx -> lowered(from)
            else -> coerce(from)
        }

    /** Handle tap: a quick look and back, never jumping to the foreground or dismissing. */
    fun afterHandleTap(
        from: VexContextDockReveal,
    ): VexContextDockReveal =
        when (coerce(from)) {
            VexContextDockReveal.Tucked -> VexContextDockReveal.Peek
            VexContextDockReveal.Peek -> VexContextDockReveal.Open
            VexContextDockReveal.Open -> VexContextDockReveal.Peek
            VexContextDockReveal.Full -> VexContextDockReveal.Open
        }

    companion object {
        /** The original dock: peek and open only. */
        val Compact =
            VexContextDockLevels(
                allowTuck = false,
                allowFull = false,
            )

        /** A dock layered over a stage: may be dismissed and may take the foreground. */
        val Layered =
            VexContextDockLevels(
                allowTuck = true,
                allowFull = true,
            )
    }
}

/**
 * Hoistable dock state: which page is selected and how much of it is revealed.
 *
 * A host that must recognize what is in front of the human (a shell deciding whether the stage is
 * covered, a journey asserting the music page is open) hoists this; otherwise the dock remembers it.
 */
@Stable
class VexContextDockState(
    initialPageRef: VexContextDockPageRef?,
    initialReveal: VexContextDockReveal,
) {
    var selectedPageRef: VexContextDockPageRef? by mutableStateOf(initialPageRef)
        private set

    var reveal: VexContextDockReveal by mutableStateOf(initialReveal)
        private set

    fun select(
        pageRef: VexContextDockPageRef,
    ) {
        selectedPageRef = pageRef
    }

    fun show(
        level: VexContextDockReveal,
    ) {
        reveal = level
    }

    companion object {
        val Saver: Saver<VexContextDockState, List<String>> =
            Saver(
                save = { state ->
                    listOf(
                        state.reveal.name,
                        state.selectedPageRef?.value.orEmpty(),
                    )
                },
                restore = { saved ->
                    VexContextDockState(
                        initialPageRef =
                            saved
                                .getOrNull(1)
                                ?.takeIf(String::isNotBlank)
                                ?.let(::VexContextDockPageRef),
                        initialReveal =
                            VexContextDockReveal.entries
                                .firstOrNull { it.name == saved.firstOrNull() }
                                ?: VexContextDockReveal.Peek,
                    )
                },
            )
    }
}

@Composable
fun rememberVexContextDockState(
    initialPageRef: VexContextDockPageRef? = null,
    initialReveal: VexContextDockReveal = VexContextDockReveal.Peek,
): VexContextDockState =
    rememberSaveable(
        saver = VexContextDockState.Saver,
    ) {
        VexContextDockState(
            initialPageRef = initialPageRef,
            initialReveal = initialReveal,
        )
    }

/**
 * One feature-owned projection admitted into a host-owned [VexContextDock].
 *
 * The dock owns paging and reveal mechanics only. The contributing feature continues to own the
 * page's state, actions, and meaning. [label] names the page on the dock's tab row; [live] marks a
 * page whose owner has something happening right now.
 */
class VexContextDockPage(
    val pageRef: VexContextDockPageRef,
    val label: String? = null,
    val live: Boolean = false,
    val content: @Composable (reveal: VexContextDockReveal) -> Unit,
)

/** Host-localized accessibility names for the reveal actions. Null leaves the action unnamed. */
data class VexContextDockLabels(
    val showMore: String? = null,
    val showLess: String? = null,
    val dismiss: String? = null,
    val restore: String? = null,
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

const val VEX_CONTEXT_DOCK_TEST_TAG = "vex-context-dock"

/**
 * Reusable contextual surface with two independent axes:
 *
 * horizontal swipe / tab -> which feature/context is being projected?
 * vertical drag / handle -> how much of that context should be revealed?
 *
 * Hosting this component does not transfer semantic ownership into the host. Pages remain
 * feature-owned projections. The dock's semantics always state the selected page and reveal so the
 * arrangement in front of the human is recognizable, not inferred from pixels.
 */
@Composable
fun VexContextDock(
    pages: List<VexContextDockPage>,
    preferredPageRef: VexContextDockPageRef? = null,
    onSelectedPageChanged: (VexContextDockPageRef) -> Unit = {},
    modifier: Modifier = Modifier,
    state: VexContextDockState =
        rememberVexContextDockState(
            initialPageRef = preferredPageRef,
        ),
    levels: VexContextDockLevels = VexContextDockLevels.Compact,
    labels: VexContextDockLabels = VexContextDockLabels(),
    fullHeightFraction: Float = 0.86f,
    onRevealChanged: (VexContextDockReveal) -> Unit = {},
) {
    if (pages.isEmpty()) {
        return
    }

    val pageRefs =
        remember(pages) {
            pages.map(VexContextDockPage::pageRef)
        }
    val pagerState =
        rememberPagerState(
            initialPage =
                resolveVexContextDockPageIndex(
                    pageRefs = pageRefs,
                    preferredPageRef = state.selectedPageRef ?: preferredPageRef,
                ),
            pageCount = { pages.size },
        )
    val scope = rememberCoroutineScope()
    val reveal = levels.coerce(state.reveal)
    val selectedIndex =
        resolveVexContextDockPageIndex(
            pageRefs = pageRefs,
            preferredPageRef = state.selectedPageRef,
        )
    val selectedPage = pages[selectedIndex.coerceIn(pages.indices)]

    fun change(
        to: VexContextDockReveal,
    ) {
        val admitted = levels.coerce(to)
        if (admitted != state.reveal) {
            state.show(admitted)
            onRevealChanged(admitted)
        }
    }

    // A host's preferred page (for example "a search result was just chosen") selects that page.
    LaunchedEffect(preferredPageRef) {
        preferredPageRef
            ?.takeIf { it in pageRefs }
            ?.let(state::select)
    }

    // Selected identity -> pager position. Survives pages appearing and disappearing.
    LaunchedEffect(
        pageRefs,
        state.selectedPageRef,
    ) {
        val target =
            resolveVexContextDockPageIndex(
                pageRefs = pageRefs,
                preferredPageRef = state.selectedPageRef,
            )
        if (
            target in pages.indices &&
            target != pagerState.settledPage &&
            !pagerState.isScrollInProgress
        ) {
            pagerState.scrollToPage(target)
        }
    }

    // Human swipe -> selected identity.
    LaunchedEffect(
        pagerState,
        pageRefs,
    ) {
        snapshotFlow {
            pagerState.settledPage to pagerState.isScrollInProgress
        }
            .distinctUntilChanged()
            .collect { (pageIndex, scrolling) ->
                if (!scrolling && pageIndex in pageRefs.indices) {
                    val ref = pageRefs[pageIndex]
                    if (ref != state.selectedPageRef) {
                        state.select(ref)
                    }
                    onSelectedPageChanged(ref)
                }
            }
    }

    var verticalDrag by remember { mutableFloatStateOf(0f) }
    val dragThresholdPx = with(LocalDensity.current) { 28.dp.toPx() }
    val stateText =
        "${selectedPage.label ?: selectedPage.pageRef.value} · ${reveal.name}"
    val actions =
        buildList {
            if (levels.raised(reveal) != reveal) {
                labels.showMore?.let { label ->
                    add(CustomAccessibilityAction(label) { change(levels.raised(reveal)); true })
                }
            }
            if (levels.lowered(reveal) != reveal && reveal != VexContextDockReveal.Peek) {
                labels.showLess?.let { label ->
                    add(CustomAccessibilityAction(label) { change(levels.lowered(reveal)); true })
                }
            }
            if (levels.allowTuck && reveal != VexContextDockReveal.Tucked) {
                labels.dismiss?.let { label ->
                    add(CustomAccessibilityAction(label) { change(VexContextDockReveal.Tucked); true })
                }
            }
        }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val fullHeight: Dp =
            if (maxHeight.value.isFinite()) maxHeight * fullHeightFraction else 560.dp

        Surface(
            modifier =
                Modifier
                    .then(
                        if (reveal == VexContextDockReveal.Tucked) Modifier else Modifier.fillMaxWidth(),
                    )
                    .testTag(VEX_CONTEXT_DOCK_TEST_TAG)
                    .semantics {
                        stateDescription = stateText
                        customActions = actions
                    }
                    .animateContentSize(
                        animationSpec = spring(dampingRatio = 0.86f, stiffness = 420f),
                    )
                    .pointerInput(pages, reveal, levels) {
                        detectVerticalDragGestures(
                            onDragStart = { verticalDrag = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                verticalDrag += dragAmount
                            },
                            onDragEnd = {
                                change(
                                    levels.afterDrag(
                                        from = reveal,
                                        dragPx = verticalDrag,
                                        thresholdPx = dragThresholdPx,
                                    ),
                                )
                                verticalDrag = 0f
                            },
                            onDragCancel = { verticalDrag = 0f },
                        )
                    },
            shape =
                if (reveal == VexContextDockReveal.Tucked) {
                    RoundedCornerShape(50)
                } else {
                    RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp,
                    )
                },
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
        ) {
            if (reveal == VexContextDockReveal.Tucked) {
                VexContextDockTucked(
                    page = selectedPage,
                    restoreLabel = labels.restore,
                    onRestore = { change(VexContextDockReveal.Peek) },
                )
            } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (reveal == VexContextDockReveal.Full) {
                                Modifier.height(fullHeight)
                            } else {
                                Modifier.heightIn(
                                    min = 82.dp,
                                    max =
                                        if (reveal == VexContextDockReveal.Open) 420.dp else 168.dp,
                                )
                            },
                        )
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VexContextDockHandle(
                    expanded = reveal.isExpanded,
                    label =
                        if (reveal.isExpanded) labels.showLess else labels.showMore,
                    onTap = { change(levels.afterHandleTap(reveal)) },
                )

                if (pages.any { it.label != null } && pages.size > 1) {
                    VexContextDockTabs(
                        pages = pages,
                        selectedIndex = selectedIndex,
                        onSelect = { index ->
                            state.select(pageRefs[index])
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f, fill = reveal == VexContextDockReveal.Full),
                    key = { index -> pages[index].pageRef.value },
                    verticalAlignment = Alignment.Top,
                ) { index ->
                    AnimatedContent(
                        targetState = reveal,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "context-dock-reveal",
                    ) { shown ->
                        pages[index].content(shown)
                    }
                }

                if (pages.size > 1 && pages.none { it.label != null }) {
                    VexContextDockDots(
                        count = pages.size,
                        selectedIndex = pagerState.settledPage,
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun VexContextDockHandle(
    expanded: Boolean,
    label: String?,
    onTap: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clickable(onClickLabel = label, onClick = onTap)
                .semantics {
                    stateDescription = if (expanded) "expanded" else "collapsed"
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        shape = CircleShape,
                    ),
        )
    }
}

@Composable
private fun VexContextDockTabs(
    pages: List<VexContextDockPage>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        pages.forEachIndexed { index, page ->
            val selected = index == selectedIndex
            Row(
                modifier =
                    Modifier
                        .background(
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                                },
                            shape = RoundedCornerShape(50),
                        )
                        .clickable { onSelect(index) }
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (page.live) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
                Text(
                    text = page.label ?: page.pageRef.value,
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun VexContextDockDots(
    count: Int,
    selectedIndex: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Surface(
                modifier =
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (index == selectedIndex) 7.dp else 5.dp),
                shape = CircleShape,
                color =
                    if (index == selectedIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
            ) {}
        }
    }
}

@Composable
private fun VexContextDockTucked(
    page: VexContextDockPage,
    restoreLabel: String?,
    onRestore: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .clickable(onClickLabel = restoreLabel, onClick = onRestore)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 24.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), CircleShape),
        )
        if (page.live) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
        page.label?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// [VXG RealForever]
