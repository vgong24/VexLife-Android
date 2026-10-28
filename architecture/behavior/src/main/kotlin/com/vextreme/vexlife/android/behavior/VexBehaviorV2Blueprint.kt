package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * V2 is a sibling experiment over the accepted V1 SCAE graph, not a replacement fork.
 *
 * Generic ordering intentionally follows the V1 Family language:
 *
 *     Family, State, Act, Effect
 *
 * Control remains attached separately through controls<Control>(), exactly as in V1.
 */
class VexBehaviorV2Blueprint<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> internal constructor(
    val behaviorRef: String,
    val states: Set<KClass<out State>>,
    val relationships:
        List<
            VexBehaviorV2Relationship<
                Family,
                State,
                Act,
                Effect
            >
        >,
) {
    /**
     * The V1-compatible immediate graph.
     *
     * Existing Control projection/resolution can therefore stay on the current foundation instead
     * of being cloned into a second SCAE vocabulary.
     */
    val v1:
        VexBehaviorBlueprint<
            Family,
            State,
            Act,
            Effect
        >

    init {
        require(
            behaviorRef.isNotBlank(),
        ) {
            "behaviorRef must not be blank"
        }
        require(
            states.isNotEmpty(),
        ) {
            "behavior V2 blueprint requires at least one state"
        }

        relationships.forEach { relationship ->
            require(
                relationship.from in
                    states,
            ) {
                "relationship source is not a registered state: " +
                    relationship.from
            }
            require(
                relationship
                    .immediateDisposition
                    .stateKind in
                    states,
            ) {
                "relationship immediate state is not registered: " +
                    relationship
                        .immediateDisposition
                        .stateKind
            }

            relationship
                .effectPlan
                .results
                ?.let { results ->
                    listOf(
                        results.success,
                        results.failure,
                    ).forEach { result ->
                        result
                            .admittedStateKind
                            ?.let { laterState ->
                                require(
                                    laterState in
                                        states,
                                ) {
                                    "relationship later admitted state is not registered: " +
                                        laterState
                                }
                            }
                    }
                }
        }

        v1 =
            VexBehaviorBlueprint(
                behaviorRef =
                    behaviorRef,
                states =
                    states,
                relationships =
                    relationships.map { relationship ->
                        VexBehaviorRelationship(
                            from =
                                relationship.from,
                            act =
                                relationship.act,
                            effect =
                                relationship
                                    .effectPlan
                                    .effect,
                            admittedStateKind =
                                relationship
                                    .immediateDisposition
                                    .stateKind,
                            conditionName =
                                relationship
                                    .conditionName,
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
        VexBehaviorV2Relationship<
            Family,
            State,
            Act,
            Effect
        >
    > {
        val currentType =
            current::class

        require(
            currentType in
                states,
        ) {
            "current state is not registered in " +
                behaviorRef +
                ": " +
                currentType
        }

        return relationships.filter { relationship ->
            relationship.from ==
                currentType &&
                relationship
                    .isAvailableFrom(
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
    ): VexBehaviorV2Resolution<
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
                    it.act ==
                        act
                }
                ?: return null

        return VexBehaviorV2Resolution(
            effect =
                relationship
                    .effectPlan
                    .effect,
            immediateDisposition =
                relationship
                    .immediateDisposition,
            executionMode =
                relationship
                    .effectPlan
                    .executionMode,
            results =
                relationship
                    .effectPlan
                    .results,
        )
    }

    /**
     * Reuse V1 Control projection/resolution instead of creating another Control language in V2.
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
                behaviorRef +
                    " [V2]",
            )
            relationships.forEach { relationship ->
                append(
                    relationship
                        .from
                        .simpleName,
                )
                append(
                    " --",
                )
                append(
                    relationship.act,
                )
                append(
                    " / ",
                )
                append(
                    relationship
                        .effectPlan
                        .effect,
                )

                relationship
                    .effectPlan
                    .executionMode
                    ?.let { mode ->
                        append(
                            " begin[",
                        )
                        append(
                            mode,
                        )
                        append(
                            "]",
                        )
                    }

                append(
                    "--> ",
                )
                append(
                    relationship
                        .immediateDisposition
                        .kind,
                )
                append(
                    " ",
                )
                append(
                    relationship
                        .immediateDisposition
                        .stateKind
                        .simpleName,
                )

                relationship
                    .effectPlan
                    .results
                    ?.let { results ->
                        append(
                            " ; Success=",
                        )
                        append(
                            results
                                .success
                                .admittedStateKind
                                ?.simpleName
                                ?: "NO_STATE_CHANGE",
                        )
                        append(
                            " ; Failure=",
                        )
                        append(
                            results
                                .failure
                                .admittedStateKind
                                ?.simpleName
                                ?: "NO_STATE_CHANGE",
                        )
                    }

                relationship
                    .conditionName
                    ?.let { name ->
                        append(
                            " when ",
                        )
                        append(
                            name,
                        )
                    }

                appendLine()
            }
        }.trimEnd()

    private fun requireNoActAmbiguity(
        current: State,
        available:
            List<
                VexBehaviorV2Relationship<
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
                require(
                    matches.size ==
                        1,
                ) {
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
> vexBehaviorV2Blueprint(
    behaviorRef: String,
    block:
        VexBehaviorV2BlueprintBuilder<
            Family,
            State,
            Act,
            Effect
        >.() -> Unit,
): VexBehaviorV2Blueprint<
    Family,
    State,
    Act,
    Effect
> =
    VexBehaviorV2BlueprintBuilder<
        Family,
        State,
        Act,
        Effect
    >(
        behaviorRef =
            behaviorRef,
    )
        .apply(
            block,
        )
        .build()

// [VXG RealForever]
