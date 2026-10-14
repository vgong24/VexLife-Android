package com.vextreme.vexlife.android.vexarchitecture

import kotlin.reflect.KClass

/**
 * One visible, typed relationship between two ordinary domain conditions.
 *
 * The generic parameters are the human-readable contract: [Before] -> [After]. Domain cases do not
 * need to inherit a Vex framework type merely to participate in architecture.
 */
class VexTransition<Before : Any, After : Any> @PublishedApi internal constructor(
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

inline fun <
    reified Before : Any,
    reified After : Any,
> vexTransition(
    transitionRef: String,
): VexTransition<Before, After> =
    VexTransition(
        transitionRef = transitionRef,
        before = Before::class,
        after = After::class,
    )

/**
 * Read-only runtime projection of one Case's local relational neighborhood.
 *
 * A whole-system graph may be derived from placements later; the graph is not the source of truth.
 */
interface VexPlacementView {
    val caseType: KClass<*>
    val arrivals: List<VexTransition<*, *>>
    val transitions: List<VexTransition<*, *>>
}

class VexPlacement<Current : Any> @PublishedApi internal constructor(
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
 * Current is implicit in the builder. Every relationship listed here must fit that node.
 */
class VexPlacementBuilder<Current : Any> @PublishedApi internal constructor(
    private val caseType: KClass<Current>,
) {
    private val arrivalsByRef =
        linkedMapOf<String, VexTransition<*, Current>>()
    private val transitionsByRef =
        linkedMapOf<String, VexTransition<Current, *>>()

    fun <Before : Any> arrivedBy(
        transition: VexTransition<Before, Current>,
    ) {
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
            caseType = caseType,
            typedArrivals = arrivalsByRef.values.toList(),
            typedTransitions = transitionsByRef.values.toList(),
        )
}

inline fun <reified Current : Any> vexPlacement(
    block: VexPlacementBuilder<Current>.() -> Unit,
): VexPlacement<Current> =
    VexPlacementBuilder(
        caseType = Current::class,
    ).apply(block)
        .build()

/**
 * One semantic/UI location that may represent different transitions in different current Cases.
 *
 * Example: one physical Play/Pause control can contain Play and Pause. Current Placement selects the
 * one transition actually available; the UI does not reinterpret booleans to rediscover it.
 */
class VexTransitionSlot @PublishedApi internal constructor(
    val slotRef: String,
    val candidates: List<VexTransition<*, *>>,
) {
    init {
        require(slotRef.isNotBlank()) {
            "slotRef must be non-blank"
        }
        require(candidates.isNotEmpty()) {
            "transition slot must contain at least one candidate"
        }
        require(
            candidates
                .map { it.transitionRef }
                .distinct()
                .size == candidates.size,
        ) {
            "transition slot candidate refs must be unique"
        }
    }

    /**
     * Read which transition from this slot is available from one current Placement.
     *
     * This is read-only architecture meaning. Resolving a transition does not execute it and does
     * not grant a UI surface platform/service authority.
     */
    internal fun resolveFrom(
        current: VexPlacementView,
    ): VexTransition<*, *>? {
        val availableByRef =
            current.transitions.associateBy {
                it.transitionRef
            }

        val matches =
            candidates.mapNotNull { candidate ->
                availableByRef[candidate.transitionRef]
                    ?.also { available ->
                        check(
                            available.before == candidate.before &&
                                available.after == candidate.after,
                        ) {
                            "transitionRef ${candidate.transitionRef} was rebound to a different relationship"
                        }
                    }
            }

        check(matches.size <= 1) {
            "slot $slotRef is ambiguous for ${current.caseType.simpleName}: " +
                matches.joinToString {
                    it.transitionRef
                }
        }

        return matches.singleOrNull()
    }
}

fun vexTransitionSlot(
    slotRef: String,
    vararg candidates: VexTransition<*, *>,
): VexTransitionSlot =
    VexTransitionSlot(
        slotRef = slotRef,
        candidates = candidates.toList(),
    )

/**
 * One ordinary domain value bound to the exact Placement that describes its current Case.
 */
class VexCurrent<Current : Any> @PublishedApi internal constructor(
    /** The ordinary domain Case that is current. */
    val case: Current,

    /** The typed relationships that are lawful from that exact Case. */
    val placement: VexPlacement<Current>,
) {
    /**
     * Answer the presentation/architecture question:
     *
     *     "Which transition in this semantic slot is meaningful from what is current?"
     *
     * The answer is descriptive only. A separate rightful owner performs effects.
     */
    fun availableTransition(
        slot: VexTransitionSlot,
    ): VexTransition<*, *>? =
        slot.resolveFrom(
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

// [VXG RealForever]
