package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * Immutable relational blueprint for one bounded behavior.
 *
 * The blueprint owns geometry, not runtime truth:
 *
 * current state + human intention
 * -> one lawful relationship
 * -> effect meaning + expected destination state kind
 *
 * A provider, reducer, or service may realize the effect and produce the next actual state value.
 * The blueprint does not become a second state store or effect owner.
 */
class VexBehaviorBlueprint<
    State : Any,
    Intention : Any,
    Effect : Any,
> internal constructor(
    val behaviorRef: String,
    val states: Set<KClass<out State>>,
    val relationships: List<VexBehaviorRelationship<State, Intention, Effect>>,
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
                it.from to it.intention
            }
            .forEach { (coordinate, matches) ->
                require(matches.size == 1) {
                    "behavior " +
                        behaviorRef +
                        " has ambiguous intention " +
                        coordinate.second +
                        " from " +
                        coordinate.first.simpleName
                }
            }
    }

    fun outgoing(
        state: KClass<out State>,
    ): List<VexBehaviorRelationship<State, Intention, Effect>> =
        relationships.filter {
            it.from == state
        }

    fun incoming(
        state: KClass<out State>,
    ): List<VexBehaviorRelationship<State, Intention, Effect>> =
        relationships.filter {
            it.to == state
        }

    /**
     * Resolve one human intention against current state without performing the effect.
     *
     * Null means the intention is not lawful or available from this current state.
     */
    fun resolve(
        current: State,
        intention: Intention,
    ): VexBehaviorResolution<State, Effect>? {
        val currentType =
            current::class

        require(currentType in states) {
            "current state is not registered in " + behaviorRef + ": " + currentType
        }

        val relationship =
            relationships
                .singleOrNull {
                    it.from == currentType &&
                        it.intention == intention
                }
                ?: return null

        if (!relationship.isAvailableFrom(current)) {
            return null
        }

        return VexBehaviorResolution(
            effect = relationship.effect,
            destination = relationship.to,
        )
    }

    /** Human/debug view derived from the authored graph; never an execution surface. */
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
 * Feature code should keep the resulting instance at the lifetime that actually owns the behavior;
 * this factory does not create a global singleton.
 */
fun <
    State : Any,
    Intention : Any,
    Effect : Any,
> vexBehaviorBlueprint(
    behaviorRef: String,
    block: VexBehaviorBlueprintBuilder<State, Intention, Effect>.() -> Unit,
): VexBehaviorBlueprint<State, Intention, Effect> =
    VexBehaviorBlueprintBuilder<State, Intention, Effect>(
        behaviorRef = behaviorRef,
    )
        .apply(block)
        .build()
