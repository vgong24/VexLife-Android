package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

/**
 * Explicit transitive copy contract for Relay-owned values.
 *
 * Kotlin data-class copy() is shallow. A state containing mutable nested ownership must recursively
 * detach it here rather than sharing caller/consumer-owned collections.
 */
interface VexDeepCopyable<Self : VexDeepCopyable<Self>> {
    fun deepCopy(): Self
}

/**
 * Current state owned by one Relay.
 *
 * StateFlow is only the latest projection. A Relay transition sequence is not an event ledger.
 */
interface VexState<Self : VexState<Self>> : VexDeepCopyable<Self>

/**
 * Input family for a Compound Relay.
 *
 * Nullable top-level fields may be used by a concrete input type as "no new patch for this
 * dependency". If null is meaningful domain truth, that meaning belongs inside the nested state
 * rather than being overloaded here.
 */
interface VexInputState<Self : VexInputState<Self>> : VexState<Self>

/** Output family derived by a Compound Relay. */
interface VexOutputState<Self : VexOutputState<Self>> : VexState<Self>

/**
 * Common current-state projection used when one Relay later becomes a dependency of another.
 */
interface VexObservableRelay<State : VexState<State>> {
    val currentState: StateFlow<State>

    val current: State
        get() = currentState.value.deepCopy()
}

/**
 * Prevent callers and collectors from acquiring mutable ownership of Relay-retained state.
 */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class DefensiveVexStateFlow<State : VexState<State>>(
    private val source: StateFlow<State>,
) : StateFlow<State> {
    override val replayCache: List<State>
        get() = listOf(value)

    override val value: State
        get() = source.value.deepCopy()

    override suspend fun collect(
        collector: FlowCollector<State>,
    ): Nothing =
        source.collect(
            object : FlowCollector<State> {
                override suspend fun emit(value: State) {
                    collector.emit(value.deepCopy())
                }
            },
        )
}

// [VXG RealForever]
