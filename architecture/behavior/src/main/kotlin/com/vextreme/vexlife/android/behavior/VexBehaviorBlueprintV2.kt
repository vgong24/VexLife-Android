package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * V2 is a sibling experiment over the accepted V1 SCAE graph, not a replacement fork.
 *
 * V1 remains the production-used immediate relationship contract:
 *
 *     State + Act -> Effect + immediate admitted State kind
 *
 * V2 reuses that exact immediate graph and adds only the execution/result information earned by the
 * ThemeMode Origin Walk:
 *
 *     immediate decision -> stay/admit
 *     begin(mode = Replaceable | Sequenced) -> later-completing Effect
 *     Success / Failure -> optional later admit<State>()
 *
 * Keeping V1 intact lets existing VexStream/Queue consumers remain stable while V2 proves the more
 * complete two-scope circuit in Kotlin.
 */
enum class VexBehaviorExecutionModeV2 {
    Replaceable,
    Sequenced,
}

enum class VexBehaviorStateDispositionKindV2 {
    Stay,
    Admit,
}

data class VexBehaviorStateDispositionV2<State : Any>(
    val kind: VexBehaviorStateDispositionKindV2,
    val stateKind: KClass<out State>,
)

data class VexBehaviorAsyncResultV2<State : Any>(
    /**
     * Null means this result produces no additional State admission.
     *
     * Result scopes deliberately do not require a redundant stay(); stay/admit remains mandatory
     * only for the immediate on(...) decision.
     */
    val admittedStateKind: KClass<out State>?,
)

data class VexBehaviorAsyncResultsV2<State : Any>(
    val success: VexBehaviorAsyncResultV2<State>,
    val failure: VexBehaviorAsyncResultV2<State>,
)

data class VexBehaviorEffectPlanV2<
    State : Any,
    Effect,
> internal constructor(
    val effect: Effect,
    val executionMode: VexBehaviorExecutionModeV2?,
    val results: VexBehaviorAsyncResultsV2<State>?,
) {
    val isAsync: Boolean
        get() = executionMode != null
}

class VexBehaviorRelationshipV2<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> internal constructor(
    val from: KClass<out State>,
    val act: Act,
    val effectPlan: VexBehaviorEffectPlanV2<State, Effect>,
    val immediateDisposition: VexBehaviorStateDispositionV2<State>,
    val conditionName: String?,
    private val whenCurrent: (State) -> Boolean,
) {
    internal fun isAvailableFrom(
        current: State,
    ): Boolean =
        from.isInstance(current) &&
            whenCurrent(current)
}

data class VexBehaviorResolutionV2<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
>(
    val effect: Effect,
    val immediateDisposition: VexBehaviorStateDispositionV2<State>,
    val executionMode: VexBehaviorExecutionModeV2?,
    val results: VexBehaviorAsyncResultsV2<State>?,
)

/**
 * V2 wraps an ordinary V1 immediate graph so existing Control projection/resolution remains reusable.
 *
 * The additional V2 data answers only questions V1 intentionally does not answer:
 * - how repeated async episodes relate to one another;
 * - whether a later Success/Failure result may emit another State admission.
 */
class VexBehaviorBlueprintV2<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> internal constructor(
    val behaviorRef: String,
    val states: Set<KClass<out State>>,
    val relationships:
        List<
            VexBehaviorRelationshipV2<
                Family,
                State,
                Act,
                Effect
            >
        >,
) {
    val v1:
        VexBehaviorBlueprint<
            Family,
            State,
            Act,
            Effect
        >

    init {
        require(behaviorRef.isNotBlank()) {
            "behaviorRef must not be blank"
        }
        require(states.isNotEmpty()) {
            "behavior blueprint V2 requires at least one state"
        }

        relationships.forEach { relationship ->
            require(relationship.from in states) {
                "relationship source is not a registered state: " +
                    relationship.from
            }
            require(
                relationship.immediateDisposition.stateKind in
                    states,
            ) {
                "relationship immediate state is not registered: " +
                    relationship.immediateDisposition.stateKind
            }

            relationship.effectPlan.results
                ?.let { results ->
                    listOf(
                        results.success,
                        results.failure,
                    ).forEach { result ->
                        result.admittedStateKind
                            ?.let { laterState ->
                                require(laterState in states) {
                                    "relationship later admitted state is not registered: " +
                                        laterState
                                }
                            }
                    }
                }
        }

        v1 =
            VexBehaviorBlueprint(
                behaviorRef = behaviorRef,
                states = states,
                relationships =
                    relationships.map { relationship ->
                        VexBehaviorRelationship(
                            from = relationship.from,
                            act = relationship.act,
                            effect =
                                relationship.effectPlan.effect,
                            admittedStateKind =
                                relationship
                                    .immediateDisposition
                                    .stateKind,
                            conditionName =
                                relationship.conditionName,
                            whenCurrent = { current ->
                                relationship
                                    .isAvailableFrom(
                                        current,
                                    )
                            },
                        )
                    },
            )
    }

    fun availableRelationships(
        current: State,
    ): List<
        VexBehaviorRelationshipV2<
            Family,
            State,
            Act,
            Effect
        >
    > {
        val currentType =
            current::class

        require(currentType in states) {
            "current state is not registered in " +
                behaviorRef +
                ": " +
                currentType
        }

        return relationships.filter { relationship ->
            relationship.from == currentType &&
                relationship.isAvailableFrom(
                    current,
                )
        }
    }

    fun availableActs(
        current: State,
    ): List<Act> {
        val available =
            availableRelationships(
                current,
            )

        requireNoActAmbiguity(
            current = current,
            available = available,
        )

        return available
            .map {
                it.act
            }
            .distinct()
    }

    fun resolve(
        current: State,
        act: Act,
    ): VexBehaviorResolutionV2<
        Family,
        State,
        Effect
    >? {
        val available =
            availableRelationships(
                current,
            )

        requireNoActAmbiguity(
            current = current,
            available = available,
        )

        val relationship =
            available
                .singleOrNull {
                    it.act == act
                }
                ?: return null

        return VexBehaviorResolutionV2(
            effect =
                relationship.effectPlan.effect,
            immediateDisposition =
                relationship.immediateDisposition,
            executionMode =
                relationship.effectPlan.executionMode,
            results =
                relationship.effectPlan.results,
        )
    }

    /**
     * Reuse V1 Control projection/resolution instead of cloning SCAE Control semantics into V2.
     */
    inline fun <reified Control> controls(
        block:
            VexBehaviorControlBlueprintBuilder<
                Family,
                Act,
                Control
            >.() -> Unit,
    ): VexBehaviorControlBlueprint<
        Family,
        State,
        Act,
        Effect,
        Control
    >
        where Control : Enum<Control>,
              Control : VexBehaviorFamily.Control<Family> =
        v1.controls(
            block,
        )

    fun describe(): String =
        buildString {
            appendLine(
                behaviorRef + " [V2]",
            )
            relationships.forEach { relationship ->
                append(
                    relationship.from.simpleName,
                )
                append(" --")
                append(
                    relationship.act,
                )
                append(" / ")
                append(
                    relationship.effectPlan.effect,
                )

                relationship.effectPlan.executionMode
                    ?.let { mode ->
                        append(" begin[")
                        append(mode)
                        append("]")
                    }

                append("--> ")
                append(
                    relationship
                        .immediateDisposition
                        .kind,
                )
                append(" ")
                append(
                    relationship
                        .immediateDisposition
                        .stateKind
                        .simpleName,
                )

                relationship.effectPlan.results
                    ?.let { results ->
                        append(
                            " ; Success=",
                        )
                        append(
                            results.success
                                .admittedStateKind
                                ?.simpleName
                                ?: "NO_STATE_CHANGE",
                        )
                        append(
                            " ; Failure=",
                        )
                        append(
                            results.failure
                                .admittedStateKind
                                ?.simpleName
                                ?: "NO_STATE_CHANGE",
                        )
                    }

                relationship.conditionName
                    ?.let { name ->
                        append(" when ")
                        append(name)
                    }

                appendLine()
            }
        }.trimEnd()

    private fun requireNoActAmbiguity(
        current: State,
        available:
            List<
                VexBehaviorRelationshipV2<
                    Family,
                    State,
                    Act,
                    Effect
                >
            >,
    ) {
        available
            .groupBy {
                it.act
            }
            .forEach { (act, matches) ->
                require(matches.size == 1) {
                    "behavior " +
                        behaviorRef +
                        " V2 has ambiguous act " +
                        act +
                        " from current " +
                        current::class.simpleName +
                        "; " +
                        matches.size +
                        " conditional relationships are simultaneously available"
                }
            }
    }
}

fun <
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> vexBehaviorBlueprintV2(
    behaviorRef: String,
    block:
        VexBehaviorBlueprintBuilderV2<
            Family,
            State,
            Act,
            Effect
        >.() -> Unit,
): VexBehaviorBlueprintV2<
    Family,
    State,
    Act,
    Effect
> =
    VexBehaviorBlueprintBuilderV2<
        Family,
        State,
        Act,
        Effect
    >(
        behaviorRef = behaviorRef,
    )
        .apply(block)
        .build()

// [VXG RealForever]
