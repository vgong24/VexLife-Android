package com.vextreme.vex.android.architecture.relay

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

/**
 * A Relay-owned state value that knows how to detach itself from mutable caller/consumer ownership.
 *
 * The self type keeps deepCopy type-safe:
 *
 * data class ItemVexState(...) : VexState<ItemVexState> {
 *     override fun deepCopy(): ItemVexState = ...
 * }
 */
interface VexState<Self : VexState<Self>> {
    fun deepCopy(): Self
}

/**
 * Input family for a compound Relay.
 *
 * A component may use nullable top-level fields as "no update supplied" patch markers.
 * If null is real domain meaning, model that meaning inside the nested state instead of overloading
 * the top-level null marker.
 */
interface VexInputState<Self : VexInputState<Self>> : VexState<Self>

/** Output family produced by a compound Relay. */
interface VexOutputState<Self : VexOutputState<Self>> : VexState<Self>

/** Common observable shape so a Relay can become an input to another Relay. */
interface VexObservableRelay<State : VexState<State>> {
    val currentState: StateFlow<State>

    val current: State
        get() = currentState.value.deepCopy()
}

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
