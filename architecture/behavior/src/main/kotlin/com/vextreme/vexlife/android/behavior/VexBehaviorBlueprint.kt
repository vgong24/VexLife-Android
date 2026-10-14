package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * Immutable relational blueprint for one bounded behavior.
 *
 * The blueprint owns semantic geometry, not runtime truth:
 *
 * current State
 * + Act
 * -> one lawful Relationship
 * -> Effect meaning
 * -> expected next State kind
 *
 * The next observed State becomes the causal context for whatever can happen after that. The
 * blueprint therefore does not need hidden "previous action" history when current State already
 * carries the meaning required to choose the next relationship.
 *
 * A provider, reducer, or service may realize the Effect and report the next actual State. The
 * blueprint does not become a second state store or effect owner.
 */
class VexBehaviorBlueprint<
    State : Any,
    Act : Any,
    Effect : Any,
> internal constructor(
    val behaviorRef: String,
    val states: Set<KClass<out State>>,
    val relationships: List<VexBehaviorRelationship<State, Act, Effect>>,
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
                "relationship source is not a registered state: " + relationship.from
            }
            require(relationship.to in states) {
                "relationship destination is not a registered state: " + relationship.to
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

    /** Every structurally authored relationship leaving one State kind. */
    fun outgoing(
        state: KClass<out State>,
    ): List<VexBehaviorRelationship<State, Act, Effect>> =
        relationships.filter {
            it.from == state
        }

    /** Every structurally authored relationship arriving at one State kind. */
    fun incoming(
        state: KClass<out State>,
    ): List<VexBehaviorRelationship<State, Act, Effect>> =
        relationships.filter {
            it.to == state
        }

    /**
     * Relationships that are structurally present AND conditionally available from this exact
     * current State value.
     *
     * This is the inverse read of the authored graph: "what can I do from here?"
     */
    fun availableRelationships(
        current: State,
    ): List<VexBehaviorRelationship<State, Act, Effect>> {
        val currentType =
            current::class

        require(currentType in states) {
            "current state is not registered in " + behaviorRef + ": " + currentType
        }

        return relationships.filter { relationship ->
            relationship.from == currentType &&
                relationship.isAvailableFrom(current)
        }
    }

    /**
     * Semantic Acts currently available from [current].
     *
     * Controls and higher projections may consume this set; UI code does not need to re-encode the
     * State -> Act table.
     */
    fun availableActs(
        current: State,
    ): List<Act> =
        availableRelationships(current)
            .map {
                it.act
            }

    /**
     * Resolve one Act against current State without performing the Effect.
     *
     * Null means the Act has no lawful/available relationship from this current State.
     */
    fun resolve(
        current: State,
        act: Act,
    ): VexBehaviorResolution<State, Effect>? {
        val relationship =
            availableRelationships(current)
                .singleOrNull {
                    it.act == act
                }
                ?: return null

        return VexBehaviorResolution(
            effect = relationship.effect,
            destination = relationship.to,
        )
    }

    /** Human/debug projection derived from authored State neighborhoods. */
    fun describe(): String =
        buildString {
            appendLine(behaviorRef)
            states.forEach { state ->
                appendLine(state.simpleName.orEmpty())
                outgoing(state).forEach { relationship ->
                    appendLine("  " + relationship)
                }
            }
        }.trimEnd()
}

/**
 * Construct one immutable behavior blueprint.
 *
 * Feature code keeps the resulting instance at the lifetime that actually owns the behavior. This
 * factory does not create a global singleton.
 */
fun <
    State : Any,
    Act : Any,
    Effect : Any,
> vexBehaviorBlueprint(
    behaviorRef: String,
    block: VexBehaviorBlueprintBuilder<State, Act, Effect>.() -> Unit,
): VexBehaviorBlueprint<State, Act, Effect> =
    VexBehaviorBlueprintBuilder<State, Act, Effect>(
        behaviorRef = behaviorRef,
    )
        .apply(block)
        .build()
