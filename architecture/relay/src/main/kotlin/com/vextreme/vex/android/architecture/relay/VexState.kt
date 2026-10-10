package com.vextreme.vex.android.architecture.relay

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow

/**
 * Stable semantic identity for one Relay owner within its declared scope.
 *
 * Use a bounded owner identity such as relay.events.items or relay.session.account rather than
 * per-transition, timestamp, list-item, or attempt identity. Concurrent Relay instances that can
 * appear in one dependency graph need distinct scoped refs.
 */
@JvmInline
value class VexRelayRef(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "VexRelayRef must be non-blank"
        }
        require(value.length <= 256) {
            "VexRelayRef must be at most 256 characters"
        }
    }

    override fun toString(): String = value
}

/**
 * Explicit transitive copy contract.
 *
 * Kotlin data-class copy() is shallow. If an object contains mutable nested ownership, its
 * deepCopy() must recursively detach that ownership instead of assuming child copy() is sufficient.
 *
 * Not every deep-copyable value is independently observable state, so this contract is separate
 * from VexState.
 */
interface VexDeepCopyable<Self : VexDeepCopyable<Self>> {
    fun deepCopy(): Self
}

/**
 * A Relay-owned state value that knows how to detach itself from mutable caller/consumer ownership.
 */
interface VexState<Self : VexState<Self>> : VexDeepCopyable<Self> {
    /**
     * Defines whether two detached values carry the same semantic state.
     *
     * Data classes get the ordinary structural-equality behavior for free. A state with different
     * equality needs may broaden equality (treat equals()-different values as the same meaning), but
     * must never treat equals()-equal values as different meaning because StateFlow itself conflates
     * by equals(). Relay checks that compatibility before publication.
     */
    fun hasSameMeaningAs(other: Self): Boolean =
        this == other
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
sealed interface VexObservableRelay<State : VexState<State>> {
    val relayRef: VexRelayRef
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
