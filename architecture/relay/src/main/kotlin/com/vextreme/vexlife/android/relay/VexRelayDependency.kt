package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/**
 * One continuously observed dependency of a Compound Relay.
 *
 * Each dependency is started in its own coroutine by [VexCompoundRelay.activate], so a feature
 * author cannot accidentally place two never-ending Flow.collect calls sequentially in one
 * coroutine and starve the second dependency.
 */
class VexRelayDependency<InputState : VexInputState<InputState>> internal constructor(
    val dependencyRef: String,
    internal val collectInto: suspend (
        send: (InputState) -> Unit,
    ) -> Unit,
) {
    init {
        require(dependencyRef.isNotBlank()) {
            "dependencyRef must not be blank"
        }
    }
}

/**
 * Declarative dependency builder for one [VexCompoundRelay].
 *
 * Feature code names the dependency, names the observable source, and maps one observed value into
 * one partial Compound InputState. Coroutine launch/collect/send mechanics remain framework-owned.
 */
class VexRelayDependencies<InputState : VexInputState<InputState>> internal constructor() {
    private val dependencies =
        mutableListOf<VexRelayDependency<InputState>>()

    fun <Value> observe(
        dependencyRef: String,
        flow: Flow<Value>,
        toInputState: (Value) -> InputState,
    ) {
        require(
            dependencies.none { dependency ->
                dependency.dependencyRef == dependencyRef
            },
        ) {
            "duplicate Compound Relay dependencyRef: $dependencyRef"
        }

        dependencies +=
            VexRelayDependency(
                dependencyRef = dependencyRef,
            ) { send ->
                flow.collect { value ->
                    send(
                        toInputState(value),
                    )
                }
            }
    }

    internal fun build(): List<VexRelayDependency<InputState>> =
        dependencies.toList()
}

fun <InputState : VexInputState<InputState>> vexDependencies(
    block: VexRelayDependencies<InputState>.() -> Unit,
): List<VexRelayDependency<InputState>> =
    VexRelayDependencies<InputState>()
        .apply(block)
        .build()

// [VXG RealForever]
