package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.Flow

interface VexDeepCopyable<Self : VexDeepCopyable<Self>> {
    fun deepCopy(): Self
}

interface VexState<Self : VexState<Self>> : VexDeepCopyable<Self>

interface VexInputState<Self : VexInputState<Self>> :
    VexState<Self> {
    /**
     * Currentize one incoming partial observation against this retained Input.
     *
     * This owns Input coherence only. Relationship meaning belongs to the Compound Relay's process
     * function, and Compound State assembly belongs to the reusable Relay boundary.
     */
    fun latest(
        incoming: Self,
    ): Self
}

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
