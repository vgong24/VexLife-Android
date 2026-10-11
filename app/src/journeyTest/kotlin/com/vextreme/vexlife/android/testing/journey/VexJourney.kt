package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.TextFieldElement
import com.vextreme.vexlife.android.elements.UIElement

data class VexJourney(
    val title: String,
    val walk: VexJourneyScope.() -> Unit,
) {
    override fun toString(): String = title
}

fun vexJourney(
    title: String,
    walk: VexJourneyScope.() -> Unit,
): VexJourney =
    VexJourney(
        title = title,
        walk = walk,
    )

/**
 * One human-readable semantic walk.
 *
 * Implementations decide HOW each step is realized:
 * - JVM coverage mode records which production semantics the journey references;
 * - Android instrumentation mode performs real Compose input while independently building the
 *   semantic history that should result.
 */
interface VexJourneyScope {
    fun start(screen: ScreenElement)

    fun tap(element: ActionElement)

    fun type(
        field: TextFieldElement,
        value: String,
    )

    fun submit(element: ActionElement)

    fun dismiss(element: UIElement)

    fun back()
}
