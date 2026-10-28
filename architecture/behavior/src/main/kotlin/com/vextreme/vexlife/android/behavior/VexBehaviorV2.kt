package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * V2 sibling of the completed Origin-Walk two-scope Behavior circuit.
 *
 * V2 does not invent a feature-local Effect enum for ThemeMode. The SCAE Effect is the structured
 * begin(...) scope itself, and the operation inside it is Save(State).
 */
class VexBehaviorV2<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
> internal constructor(
    val behaviorRef: String,
    val states: Set<KClass<out State>>,
    val relationships:
        List<
            VexBehaviorV2Relationship<
                Family,
                State,
                Act
            >
        >,
) {
    val v1:
        VexBehaviorBlueprint<
            Family,
            State,
            Act,
            VexBehaviorV2Begin<
                Family,
                State
            >
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
            "behavior V2 requires at least one state"
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

            require(
                relationship
                    .effect
                    .save
                    .state
                    .let {
                        it::class in states
                    },
            ) {
                "Save State is not registered in " +
                    behaviorRef
            }

            relationship
                .effect
                .save
                .results
                .let { results ->
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
                                    "later admitted state is not registered: " +
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
                                relationship.effect,
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
            Act
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
        State
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
                relationship.effect,
            immediateDisposition =
                relationship
                    .immediateDisposition,
        )
    }

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
        VexBehaviorV2Begin<
            Family,
            State
        >,
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
                    " / begin[",
                )
                append(
                    relationship
                        .effect
                        .mode,
                )
                append(
                    "] Save(",
                )
                append(
                    relationship
                        .effect
                        .save
                        .state,
                )
                append(
                    ")--> ",
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

                val results =
                    relationship
                        .effect
                        .save
                        .results

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
                    Act
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
                        current::class.simpleName
                }
            }
    }
}

fun <
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
> vexBehaviorV2(
    behaviorRef: String,
    block:
        VexBehaviorV2Builder<
            Family,
            State,
            Act
        >.() -> Unit,
): VexBehaviorV2<
    Family,
    State,
    Act
> =
    VexBehaviorV2Builder<
        Family,
        State,
        Act
    >(
        behaviorRef =
            behaviorRef,
    )
        .apply(
            block,
        )
        .build()

// [VXG RealForever]
