package com.vextreme.vexlife.android.vexarchitecture

import kotlin.reflect.KClass

/**
 * Global contract implemented only by architecture roots.
 *
 * "Find implementations" on this contract should answer:
 *
 *     What architecture realms exist?
 *
 * Cases, controls and placements deliberately do NOT implement this global contract. A feature root
 * defines its own bounded child contracts so navigation stays inside that realm.
 */
interface VexArchitectureContract {
    /** Stable semantic path for this architecture realm. */
    val architectureRef: String

    /**
     * Root-level participation flag.
     *
     * A disabled architecture exposes no available Controls. The flag is intentionally separate from
     * runtime availability/authorization; those are different truths and must not be collapsed here.
     */
    val enabled: Boolean

    /**
     * Derived local map for orientation/debugging.
     *
     * The map is a projection of typed Placements and Controls, never a second state-machine owner.
     */
    val map: VexArchitectureMap
}

/**
 * One visible, typed relationship between two ordinary domain Cases inside one architecture realm.
 */
class VexTransition<Before : Any, After : Any> @PublishedApi internal constructor(
    val architecture: VexArchitectureContract,
    val transitionRef: String,
    val before: KClass<Before>,
    val after: KClass<After>,
) {
    init {
        require(transitionRef.isNotBlank()) {
            "transitionRef must be non-blank"
        }
    }

    override fun toString(): String =
        "$transitionRef: ${before.simpleName} -> ${after.simpleName}"
}

/** Create one typed doorway owned by this exact architecture realm. */
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
    )

/** Read-only runtime projection of one Case's local relational neighborhood. */
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

/**
 * Current is implicit in the builder. Every listed relationship must fit both this Case and realm.
 */
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
 * A Control may offer one or several candidate transitions. The exact current Placement resolves at
 * most one. Example: one Play/Pause Control contains Play and Pause relationships.
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
        current: VexPlacementView,
    ): VexTransition<*, *>? {
        check(current.architecture === architecture) {
            "control $controlRef belongs to ${architecture.architectureRef}, " +
                "not ${current.architecture.architectureRef}"
        }

        // Root feature gating happens once here. Descendants do not repeat the flag.
        if (!architecture.enabled) {
            return null
        }

        val availableByRef =
            current.transitions.associateBy {
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
            }

        check(matches.size <= 1) {
            "control $controlRef is ambiguous for ${current.caseType.simpleName}: " +
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

/** One ordinary domain Case bound to the exact Placement that describes its relationships. */
class VexCurrent<Current : Any> @PublishedApi internal constructor(
    val case: Current,
    val placement: VexPlacement<Current>,
) {
    /** Descriptive only; a rightful domain/platform owner still performs the effect. */
    fun availableTransition(
        control: VexControl,
    ): VexTransition<*, *>? =
        control.resolveFrom(
            placement,
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
 * Derived orientation surface for one architecture root.
 *
 * It validates that every outgoing edge is the SAME transition object named as an arrival by the
 * destination Case, and vice versa.
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

    /** Human-readable root orientation, not another execution model. */
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
                            transition.transitionRef +
                            " (" +
                            transition.before.simpleName +
                            " -> " +
                            transition.after.simpleName +
                            ")",
                    )
                }
                placement.transitions.forEach { transition ->
                    appendLine(
                        "  can " +
                            transition.transitionRef +
                            " (" +
                            transition.before.simpleName +
                            " -> " +
                            transition.after.simpleName +
                            ")",
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

/** Derive and validate one root architecture map from local Placements + Controls. */
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
