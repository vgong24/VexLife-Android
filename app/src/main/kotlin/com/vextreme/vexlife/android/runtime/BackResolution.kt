package com.vextreme.vexlife.android.runtime

import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.navigation.BackLayer

/**
 * The app-navigation part of Back.
 *
 * Primary peers are not stored in pageStack, so returning Events/Settings to Home is represented
 * explicitly instead of pretending those peers were pushed pages.
 */
sealed interface AppNavigationBack {
    val target: ScreenElement

    data class PopPage(
        override val target: ScreenElement,
    ) : AppNavigationBack

    data class ReturnToPrimaryRoot(
        override val target: ScreenElement,
    ) : AppNavigationBack
}

/**
 * One resolved answer to: "What would Back do right now?"
 *
 * VexRuntime resolves this once in priority order. Android Back, toolbar Back visibility, logging,
 * and the actual mutation all consume this same shape rather than maintaining parallel canGoBack
 * condition trees.
 */
sealed interface BackResolution {
    val showToolbarBack: Boolean
    val screenAfter: ScreenElement?

    data object DismissPresentation : BackResolution {
        override val showToolbarBack = false
        override val screenAfter: ScreenElement? = null
    }

    data class PopLocalLayer(
        val layer: BackLayer,
    ) : BackResolution {
        override val showToolbarBack = true
        override val screenAfter: ScreenElement? = null
    }

    data class AppNavigation(
        val step: AppNavigationBack,
    ) : BackResolution {
        override val showToolbarBack: Boolean
            get() = step is AppNavigationBack.PopPage

        override val screenAfter: ScreenElement
            get() = step.target
    }
}
