package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.Flow

/**
 * Produces a value that is safe to hand to another ownership boundary.
 *
 * Historical Relay code calls this deepCopy because mutable-backed states must detach their nested
 * collections/objects. A transitively immutable state may safely return itself: sharing an object
 * that cannot mutate does not create shared mutable ownership.
 */
interface VexDeepCopyable<Self : VexDeepCopyable<Self>> {
    fun deepCopy(): Self
}

interface VexState<Self : VexState<Self>> : VexDeepCopyable<Self>
interface VexInputState<Self : VexInputState<Self>> : VexState<Self>
interface VexOutputState<Self : VexOutputState<Self>> : VexState<Self>

/**
 * Opt-in contract for a transitively immutable ordinary Relay state.
 *
 * Implement this only when every reachable value is immutable for the lifetime of the state. The
 * same instance may then cross Relay ownership/observation boundaries without defensive allocation.
 */
interface VexImmutableState<Self : VexImmutableState<Self>> : VexState<Self> {
    @Suppress("UNCHECKED_CAST")
    override fun deepCopy(): Self =
        this as Self
}

/** Transitively immutable Compound input state. */
interface VexImmutableInputState<Self : VexImmutableInputState<Self>> :
    VexInputState<Self>,
    VexImmutableState<Self>

/** Transitively immutable Compound output state. */
interface VexImmutableOutputState<Self : VexImmutableOutputState<Self>> :
    VexOutputState<Self>,
    VexImmutableState<Self>

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
