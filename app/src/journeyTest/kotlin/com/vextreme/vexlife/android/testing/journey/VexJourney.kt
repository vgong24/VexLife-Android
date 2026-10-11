package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.TextFieldElement
import com.vextreme.vexlife.android.elements.UIElement

data class VexJourney(
    val title: String,
    val environments: List<VexJourneyEnvironment> = listOf(
        VexJourneyEnvironments.Baseline,
    ),
    val walk: VexJourneyScope.() -> Unit,
) {
    init {
        require(environments.isNotEmpty()) { "journey must declare at least one environment" }
        require(environments.map { it.id }.distinct().size == environments.size) {
            "journey environments must have unique IDs: $title"
        }
    }

    override fun toString(): String = title
}

fun vexJourney(
    title: String,
    environments: List<VexJourneyEnvironment> = listOf(
        VexJourneyEnvironments.Baseline,
    ),
    walk: VexJourneyScope.() -> Unit,
): VexJourney =
    VexJourney(
        title = title,
        environments = environments,
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
