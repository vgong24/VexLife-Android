package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * Immutable relational blueprint for one exact Behavior family.
 */
class VexBehaviorBlueprint<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> internal constructor(
    val behaviorRef: String,
    val states: Set<KClass<out State>>,
    val relationships:
        List<
            VexBehaviorRelationship<
                Family,
                State,
                Act,
                Effect
            >
        >,
) {
    init {
        require(behaviorRef.isNotBlank()) {
            "behaviorRef must not be blank"
        }
        require(states.isNotEmpty()) {
            "behavior blueprint requires at least one state"
        }

        relationships.forEach { relationship ->
            require(relationship.from in states) {
                "relationship source is not a registered state: " +
                    relationship.from
            }
            require(
                relationship.admittedStateKind in
                    states,
            ) {
                "relationship admitted state is not registered: " +
                    relationship.admittedStateKind
            }
        }
    }

    fun outgoing(
        state: KClass<out State>,
    ): List<
        VexBehaviorRelationship<
            Family,
            State,
            Act,
            Effect
        >
    > =
        relationships.filter {
            it.from == state
        }

    fun incoming(
        state: KClass<out State>,
    ): List<
        VexBehaviorRelationship<
            Family,
            State,
            Act,
            Effect
        >
    > =
        relationships.filter {
            it.admittedStateKind == state
        }

    fun availableRelationships(
        current: State,
    ): List<
        VexBehaviorRelationship<
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

    /**
     * Every Act that has exactly one lawful relationship from this exact current State value.
     *
     * Multiple relationships may share the same source State KIND + Act when their predicates are
     * mutually exclusive for State flavor. If more than one becomes valid for one current value,
     * the blueprint rejects that ambiguity instead of picking one by order.
     */
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
    ): VexBehaviorResolution<
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

        return VexBehaviorResolution(
            effect = relationship.effect,
            admittedStateKind =
                relationship.admittedStateKind,
        )
    }

    private fun requireNoActAmbiguity(
        current: State,
        available:
            List<
                VexBehaviorRelationship<
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
                        " has ambiguous act " +
                        act +
                        " from current " +
                        current::class.simpleName +
                        "; " +
                        matches.size +
                        " conditional relationships are simultaneously available"
                }
            }
    }

    /**
     * Bind one complete enum Control vocabulary from the SAME Behavior family.
     *
     * Enum is required here, not on VexPort, because only this builder needs exhaustive enumeration.
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
        VexBehaviorControlBlueprintBuilder<
            Family,
            Act,
            Control
        >(
            expectedControls =
                enumValues<Control>()
                    .toSet(),
        )
            .apply(block)
            .build(this)

    fun describe(): String =
        buildString {
            appendLine(
                behaviorRef,
            )
            states.forEach { state ->
                appendLine(
                    state.simpleName.orEmpty(),
                )
                outgoing(
                    state,
                ).forEach { relationship ->
                    appendLine(
                        "  " +
                            relationship,
                    )
                }
            }
        }.trimEnd()
}

fun <
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
> vexBehaviorBlueprint(
    behaviorRef: String,
    block:
        VexBehaviorBlueprintBuilder<
            Family,
            State,
            Act,
            Effect
        >.() -> Unit,
): VexBehaviorBlueprint<
    Family,
    State,
    Act,
    Effect
> =
    VexBehaviorBlueprintBuilder<
        Family,
        State,
        Act,
        Effect
    >(
        behaviorRef = behaviorRef,
    )
        .apply(block)
        .build()
