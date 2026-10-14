package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.Flow

/**
 * Produces a value that is safe to hand to another ownership boundary.
 *
 * Historical Relay code calls this deepCopy because mutable-backed values may need to detach nested
 * collections/objects. Relay State itself is immutable by contract, so ordinary VexState
 * implementations inherit an identity snapshot. Mutable-backed exceptions must override deepCopy()
 * and detach every mutable descendant they expose.
 */
interface VexDeepCopyable<Self : VexDeepCopyable<Self>> {
    fun deepCopy(): Self
}

/**
 * Base Relay state contract.
 *
 * Default rule:
 *
 *     VexState is transitively immutable
 *     -> deepCopy() may safely return this
 *
 * A State that contains mutable-backed data is an explicit exception and must override deepCopy()
 * to produce a detached ownership snapshot. Prefer normalizing third-party/provider-owned mutable
 * objects before they enter Relay rather than spreading mutable state through the graph.
 */
interface VexState<Self : VexState<Self>> : VexDeepCopyable<Self> {
    @Suppress("UNCHECKED_CAST")
    override fun deepCopy(): Self =
        this as Self
}

interface VexInputState<Self : VexInputState<Self>> : VexState<Self>
interface VexOutputState<Self : VexOutputState<Self>> : VexState<Self>

/**
 * Minimal Relay source contract.
 *
 * Every Relay is input-capable by role: another coordinator/Compound Relay may consume [states] or
 * [current] as one of its dependencies. "Input Relay" is therefore not a separate subclass.
 *
 * For a [VexCompoundRelay], its retained InputState is a coherent dependency snapshot; downstream
 * consumers should use that Relay's inputStates/outputStates/currentInput/currentOutput projections
 * according to what they actually need rather than unpacking full compound state by habit.
 */
interface VexObservableRelay<State : VexState<State>> {
    val states: Flow<State>
    val current: State
}

// [VXG RealForever]
