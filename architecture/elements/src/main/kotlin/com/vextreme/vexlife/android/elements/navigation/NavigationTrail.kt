package com.vextreme.vexlife.android.elements.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * A small reusable trail for navigation that happens *inside* one app page.
 *
 * Mental model:
 *
 * App page navigation:
 *   Events -> Gallery                     owned by VexRuntime/AppNavigationState
 *
 * Local layered navigation inside Gallery:
 *   HomeScreen node -> Toolbar node
 *                  -> BackButton node     owned by NavigationTrail
 *
 * Those are deliberately different depths. Primary tabs and pushed app pages must NOT be stuffed
 * into this trail merely because they also have a Back behavior.
 *
 * Why this exists:
 * - one local path owns current/depth/back instead of scattered booleans;
 * - the visible "Up" control and Android/toolbar Back can consume the same [BackLayer];
 * - [direction] is changed by the same push/pop operation that changes [path], so animation cannot
 *   accidentally say "forward" while state moved backward.
 *
 * Edge cases intentionally handled here:
 * - root starts as the only path entry, so local Back is false at depth 0;
 * - pushing the current destination again is ignored, preventing trivial duplicate depth;
 * - Back at root returns false so the outer runtime can continue resolving Back at the next layer.
 *
 * Edge cases intentionally NOT owned here:
 * - graph cycles such as A -> B -> A must be rejected/represented by the graph feature before push;
 * - durable restoration across Activity/process death is not provided by this class;
 * - primary Home/Events/Settings semantics belong to AppNavigationState.
 *
 * Lifetime:
 * NavigationTrail is usually created with Compose [remember]. That keeps local depth across ordinary
 * recomposition while the view remains mounted, but leaving/recreating that view may reset the trail.
 * Promote its state to a longer-lived owner only when a real product requirement needs that memory.
 *
 * Example consumer: AppGraph descends Home -> Toolbar -> BackButton without creating three app pages.
 */
interface BackLayer {
    val canGoBack: Boolean
    fun goBack(): Boolean
}

enum class NavigationDirection {
    Forward,
    Backward,
}

class NavigationTrail<T>(
    root: T,
) : BackLayer {
    // Ordered local breadcrumb from the feature's root to what is currently visible.
    // The list is intentionally small/local; it is not application navigation history.
    var path by mutableStateOf(listOf(root))
        private set

    // Direction describes the transition that produced the current path and is consumed by the
    // renderer below. Keeping it beside path mutation prevents animation/state disagreement.
    var direction by mutableStateOf(NavigationDirection.Forward)
        private set

    val current: T
        get() = path.last()

    val depth: Int
        get() = path.lastIndex

    override val canGoBack: Boolean
        get() = path.size > 1

    fun push(destination: T) {
        if (destination == current) return
        direction = NavigationDirection.Forward
        path = path + destination
    }

    override fun goBack(): Boolean {
        if (!canGoBack) return false
        direction = NavigationDirection.Backward
        path = path.dropLast(1)
        return true
    }
}

/**
 * Standard directional transition for a [NavigationTrail].
 *
 * Forward enters from the right and leaves toward the left. Backward is the exact spatial reverse:
 * the previous layer re-enters from the left while the current layer exits to the right.
 */
@Composable
fun <T> NavigationTrailContent(
    trail: NavigationTrail<T>,
    modifier: Modifier = Modifier,
    label: String,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = trail.current,
        modifier = modifier,
        transitionSpec = {
            when (trail.direction) {
                NavigationDirection.Forward ->
                    (slideInHorizontally { it } + fadeIn())
                        .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut())

                NavigationDirection.Backward ->
                    (slideInHorizontally { -it / 3 } + fadeIn())
                        .togetherWith(slideOutHorizontally { it } + fadeOut())
            }
        },
        label = label,
        content = { destination -> content(destination) },
    )
}
