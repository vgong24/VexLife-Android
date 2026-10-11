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
 * Example: AppGraph descends Home -> Toolbar -> BackButton without creating three app pages.
 * The same trail is consumed by its visible "Up" control and by VexRuntime's Back chain, so local
 * navigation cannot drift into a separate Back behavior.
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
    var path by mutableStateOf(listOf(root))
        private set

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
