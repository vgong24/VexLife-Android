package com.vextreme.vexlife.android.vexarchitecture

import kotlin.reflect.KClass

/**
 * Global contract implemented only by architecture roots.
 *
 * "Find implementations" here should answer "what architecture realms exist?", not return every
 * Case/Control/Placement in the application.
 */
interface VexArchitectureContract {
    val architectureRef: String
    val enabled: Boolean
    val map: VexArchitectureMap
}

/**
 * One visible, typed relationship between two ordinary domain Cases inside one architecture realm.
 *
 * A named current-Case condition may further narrow when this relationship is available. Conditions
 * select a lawful edge; they never perform its platform/domain effect.
 */
class VexTransition<Before : Any, After : Any> @PublishedApi internal constructor(
    val architecture: VexArchitectureContract,
    val transitionRef: String,
    val before: KClass<Before>,
    val after: KClass<After>,
    val conditionRef: String?,
    private val whenCurrent: (Before) -> Boolean,
) {
    init {
        require(transitionRef.isNotBlank()) {
            "transitionRef must be non-blank"
        }
        require(
            conditionRef == null ||
                conditionRef.isNotBlank(),
        ) {
            "conditionRef must be null or non-blank"
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun isAvailableFrom(
        current: Any,
    ): Boolean =
        before.isInstance(
            current,
        ) &&
            whenCurrent(
                current as Before,
            )

    override fun toString(): String =
        "$transitionRef: ${before.simpleName} -> ${after.simpleName}" +
            (
                conditionRef?.let {
                    " when $it"
                } ?: ""
            )
}

/** Create one unconditional typed doorway owned by this exact architecture realm. */
inline fun <
    reified Before : Any,
    reified After : Any,
> VexArchitectureContract.transition(
    transitionRef: String,
): VexTransition<Before, After> =
    VexTransition(
        architecture = this,
        transitionRef = transitionRef,
        before = Before::class,
        after = After::class,
        conditionRef = null,
        whenCurrent = {
            true
        },
    )

/**
 * Create one typed doorway whose availability depends on facts carried by the exact current Case.
 */
inline fun <
    reified Before : Any,
    reified After : Any,
> VexArchitectureContract.transition(
    transitionRef: String,
    conditionRef: String,
    noinline whenCurrent: (Before) -> Boolean,
): VexTransition<Before, After> =
    VexTransition(
        architecture = this,
        transitionRef = transitionRef,
        before = Before::class,
        after = After::class,
        conditionRef = conditionRef,
        whenCurrent = whenCurrent,
    )

interface VexPlacementView {
    val architecture: VexArchitectureContract
    val caseType: KClass<*>
    val arrivals: List<VexTransition<*, *>>
    val transitions: List<VexTransition<*, *>>
}

class VexPlacement<Current : Any> @PublishedApi internal constructor(
    override val architecture: VexArchitectureContract,
    override val caseType: KClass<Current>,
    val typedArrivals: List<VexTransition<*, Current>>,
    val typedTransitions: List<VexTransition<Current, *>>,
) : VexPlacementView {
    override val arrivals: List<VexTransition<*, *>>
        get() = typedArrivals

    override val transitions: List<VexTransition<*, *>>
        get() = typedTransitions
}

class VexPlacementBuilder<Current : Any> @PublishedApi internal constructor(
    private val architecture: VexArchitectureContract,
    private val caseType: KClass<Current>,
) {
    private val arrivalsByRef =
        linkedMapOf<String, VexTransition<*, Current>>()
    private val transitionsByRef =
        linkedMapOf<String, VexTransition<Current, *>>()

    fun <Before : Any> arrivedBy(
        transition: VexTransition<Before, Current>,
    ) {
        check(transition.architecture === architecture) {
            "${transition.transitionRef} belongs to ${transition.architecture.architectureRef}, " +
                "not ${architecture.architectureRef}"
        }
        check(transition.after == caseType) {
            "${transition.transitionRef} does not arrive at ${caseType.simpleName}"
        }
        check(
            arrivalsByRef.put(
                transition.transitionRef,
                transition,
            ) == null,
        ) {
            "duplicate arrival transitionRef: ${transition.transitionRef}"
        }
    }

    fun <After : Any> can(
        transition: VexTransition<Current, After>,
    ) {
        check(transition.architecture === architecture) {
            "${transition.transitionRef} belongs to ${transition.architecture.architectureRef}, " +
                "not ${architecture.architectureRef}"
        }
        check(transition.before == caseType) {
            "${transition.transitionRef} does not leave ${caseType.simpleName}"
        }
        check(
            transitionsByRef.put(
                transition.transitionRef,
                transition,
            ) == null,
        ) {
            "duplicate outgoing transitionRef: ${transition.transitionRef}"
        }
    }

    @PublishedApi
    internal fun build(): VexPlacement<Current> =
        VexPlacement(
            architecture = architecture,
            caseType = caseType,
            typedArrivals = arrivalsByRef.values.toList(),
            typedTransitions = transitionsByRef.values.toList(),
        )
}

inline fun <reified Current : Any> VexArchitectureContract.placement(
    block: VexPlacementBuilder<Current>.() -> Unit,
): VexPlacement<Current> =
    VexPlacementBuilder(
        architecture = this,
        caseType = Current::class,
    ).apply(block)
        .build()

/**
 * One semantic Control owned by one architecture realm.
 *
 * The exact current Case + Placement resolves at most one candidate after evaluating each candidate's
 * named current-Case condition.
 */
class VexControl @PublishedApi internal constructor(
    val architecture: VexArchitectureContract,
    val controlRef: String,
    val candidates: List<VexTransition<*, *>>,
) {
    init {
        require(controlRef.isNotBlank()) {
            "controlRef must be non-blank"
        }
        require(candidates.isNotEmpty()) {
            "control must contain at least one transition"
        }
        require(
            candidates.all {
                it.architecture === architecture
            },
        ) {
            "every control transition must belong to ${architecture.architectureRef}"
        }
        require(
            candidates
                .map { it.transitionRef }
                .distinct()
                .size == candidates.size,
        ) {
            "control transition refs must be unique"
        }
    }

    internal fun resolveFrom(
        currentCase: Any,
        currentPlacement: VexPlacementView,
    ): VexTransition<*, *>? {
        check(currentPlacement.architecture === architecture) {
            "control $controlRef belongs to ${architecture.architectureRef}, " +
                "not ${currentPlacement.architecture.architectureRef}"
        }

        if (!architecture.enabled) {
            return null
        }

        val availableByRef =
            currentPlacement.transitions.associateBy {
                it.transitionRef
            }

        val matches =
            candidates.mapNotNull { candidate ->
                availableByRef[candidate.transitionRef]
                    ?.also { available ->
                        check(available === candidate) {
                            "transitionRef ${candidate.transitionRef} was rebound to another relationship"
                        }
                    }
                    ?.takeIf {
                        candidate.isAvailableFrom(
                            currentCase,
                        )
                    }
            }

        check(matches.size <= 1) {
            "control $controlRef is ambiguous for ${currentPlacement.caseType.simpleName}: " +
                matches.joinToString {
                    it.transitionRef
                }
        }

        return matches.singleOrNull()
    }
}

fun VexArchitectureContract.control(
    controlRef: String,
    vararg candidates: VexTransition<*, *>,
): VexControl =
    VexControl(
        architecture = this,
        controlRef = controlRef,
        candidates = candidates.toList(),
    )

class VexCurrent<Current : Any> @PublishedApi internal constructor(
    val case: Current,
    val placement: VexPlacement<Current>,
) {
    fun availableTransition(
        control: VexControl,
    ): VexTransition<*, *>? =
        control.resolveFrom(
            currentCase = case,
            currentPlacement = placement,
        )
}

fun <Current : Any> vexCurrent(
    case: Current,
    placement: VexPlacement<Current>,
): VexCurrent<Current> =
    VexCurrent(
        case = case,
        placement = placement,
    )

/**
 * Derived orientation surface. The same Transition object must be named from both ends.
 */
class VexArchitectureMap @PublishedApi internal constructor(
    val architecture: VexArchitectureContract,
    val placements: List<VexPlacementView>,
    val controls: List<VexControl>,
    val transitions: List<VexTransition<*, *>>,
) {
    val cases: List<KClass<*>>
        get() = placements.map {
            it.caseType
        }

    fun describe(): String =
        buildString {
            appendLine(
                architecture.architectureRef +
                    " enabled=" +
                    architecture.enabled,
            )
            placements.forEach { placement ->
                appendLine(
                    placement.caseType.simpleName.orEmpty(),
                )
                placement.arrivals.forEach { transition ->
                    appendLine(
                        "  arrivedBy " +
                            transition,
                    )
                }
                placement.transitions.forEach { transition ->
                    appendLine(
                        "  can " +
                            transition,
                    )
                }
            }
            if (controls.isNotEmpty()) {
                appendLine("controls")
                controls.forEach { control ->
                    appendLine(
                        "  " +
                            control.controlRef +
                            " -> " +
                            control.candidates.joinToString {
                                it.transitionRef
                            },
                    )
                }
            }
        }.trimEnd()
}

fun VexArchitectureContract.architectureMap(
    placements: List<VexPlacementView>,
    controls: List<VexControl>,
): VexArchitectureMap {
    require(architectureRef.isNotBlank()) {
        "architectureRef must be non-blank"
    }
    require(placements.isNotEmpty()) {
        "architecture map requires at least one Placement"
    }
    require(
        placements.all {
            it.architecture === this
        },
    ) {
        "every Placement must belong to $architectureRef"
    }
    require(
        controls.all {
            it.architecture === this
        },
    ) {
        "every Control must belong to $architectureRef"
    }

    val byCase =
        placements.associateBy {
            it.caseType
        }
    check(byCase.size == placements.size) {
        "architecture $architectureRef contains duplicate Case placements"
    }

    val relationshipOccurrences =
        placements.flatMap {
            it.arrivals + it.transitions
        }
    relationshipOccurrences
        .groupBy {
            it.transitionRef
        }
        .forEach { (ref, relationships) ->
            val first = relationships.first()
            check(
                relationships.all {
                    it === first
                },
            ) {
                "architecture $architectureRef re-declared transitionRef $ref"
            }
        }

    val outgoing =
        placements.flatMap {
            it.transitions
        }.distinctBy {
            it.transitionRef
        }

    placements.forEach { placement ->
        placement.transitions.forEach { transition ->
            val destination =
                byCase[transition.after]
                    ?: error(
                        "architecture $architectureRef has no Placement for destination " +
                            transition.after.simpleName,
                    )
            check(
                destination.arrivals.any {
                    it === transition
                },
            ) {
                "${transition.transitionRef} leaves ${transition.before.simpleName} " +
                    "but ${transition.after.simpleName} does not name the same arrival"
            }
        }

        placement.arrivals.forEach { transition ->
            val source =
                byCase[transition.before]
                    ?: error(
                        "architecture $architectureRef has no Placement for source " +
                            transition.before.simpleName,
                    )
            check(
                source.transitions.any {
                    it === transition
                },
            ) {
                "${transition.transitionRef} arrives at ${transition.after.simpleName} " +
                    "but ${transition.before.simpleName} does not name the same exit"
            }
        }
    }

    controls.forEach { control ->
        control.candidates.forEach { candidate ->
            check(
                outgoing.any {
                    it === candidate
                },
            ) {
                "control ${control.controlRef} references transition " +
                    "${candidate.transitionRef} that is not in the architecture map"
            }
        }
    }

    return VexArchitectureMap(
        architecture = this,
        placements = placements.toList(),
        controls = controls.toList(),
        transitions = outgoing,
    )
}

// [VXG RealForever]
