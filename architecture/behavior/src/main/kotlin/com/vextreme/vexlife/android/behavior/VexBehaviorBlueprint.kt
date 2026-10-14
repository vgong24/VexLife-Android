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
            require(relationship.to in states) {
                "relationship destination is not a registered state: " +
                    relationship.to
            }
        }

        relationships
            .groupBy {
                it.from to it.act
            }
            .forEach { (coordinate, matches) ->
                require(matches.size == 1) {
                    "behavior " +
                        behaviorRef +
                        " has ambiguous act " +
                        coordinate.second +
                        " from " +
                        coordinate.first.simpleName
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
            it.to == state
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

    fun availableActs(
        current: State,
    ): List<Act> =
        availableRelationships(
            current,
        ).map {
            it.act
        }

    fun resolve(
        current: State,
        act: Act,
    ): VexBehaviorResolution<
        Family,
        State,
        Effect
    >? {
        val relationship =
            availableRelationships(
                current,
            )
                .singleOrNull {
                    it.act == act
                }
                ?: return null

        return VexBehaviorResolution(
            effect = relationship.effect,
            destination = relationship.to,
        )
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
