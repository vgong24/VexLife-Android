package com.vextreme.vexlife.android.testing.journey

import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ScreenElement
import com.vextreme.vexlife.android.elements.TextFieldElement
import com.vextreme.vexlife.android.elements.UIElement

data class VexJourney(
    val id: String,
    val title: String,
    val environments: List<VexJourneyEnvironment> = listOf(
        VexJourneyEnvironments.Baseline,
    ),
    val walk: VexJourneyScope.() -> Unit,
) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9-]*"))) {
            "journey id must be lowercase kebab-case: $id"
        }
        require(environments.isNotEmpty()) {
            "journey must declare at least one environment"
        }
        require(
            environments.map { it.id }.distinct().size ==
                environments.size,
        ) {
            "journey environments must have unique IDs: $title"
        }
    }

    override fun toString(): String = title
}

fun vexJourney(
    id: String,
    title: String,
    environments: List<VexJourneyEnvironment> = listOf(
        VexJourneyEnvironments.Baseline,
    ),
    walk: VexJourneyScope.() -> Unit,
): VexJourney =
    VexJourney(
        id = id,
        title = title,
        environments = environments,
        walk = walk,
    )

data class VexDynamicElementRef(
    val elementId: String,
) {
    init {
        require(elementId.isNotBlank()) {
            "dynamic journey element ID must not be blank"
        }
    }
}

fun dynamicElement(
    elementId: String,
): VexDynamicElementRef =
    VexDynamicElementRef(elementId)

class VexJourneySignal internal constructor(
    val signalRef: String,
    internal val applyTo: (AppRepositories) -> Unit,
) {
    init {
        require(signalRef.isNotBlank()) {
            "journey signal ref must not be blank"
        }
    }
}

fun journeySignal(
    signalRef: String,
    applyTo: (AppRepositories) -> Unit,
): VexJourneySignal =
    VexJourneySignal(
        signalRef = signalRef,
        applyTo = applyTo,
    )

/**
 * One human-readable semantic walk.
 *
 * [signal] is a deterministic test-side external cause. It does not pretend the human clicked
 * something and therefore forms no expected VexLogger interaction by itself.
 *
 * [tapDynamic] is for runtime-data identity such as one event row action that cannot be a permanent
 * static registry element. Its element ID must still be production-formed and stable.
 *
 * [expectVisible] lets the same production identity act as a QA observation without inventing an
 * interaction or relying on text/list position.
 */
interface VexJourneyScope {
    fun start(screen: ScreenElement)

    fun signal(signal: VexJourneySignal)

    fun tap(element: ActionElement)

    fun tapDynamic(element: VexDynamicElementRef)

    /**
     * Observes one production-formed runtime identity and requires it to be visibly rendered.
     *
     * This is not an interaction and therefore forms no expected VexLogger record.
     */
    fun expectVisible(element: VexDynamicElementRef)

    fun type(
        field: TextFieldElement,
        value: String,
    )

    fun submit(element: ActionElement)

    fun dismiss(element: UIElement)

    fun back()
}

// [VXG RealForever]
