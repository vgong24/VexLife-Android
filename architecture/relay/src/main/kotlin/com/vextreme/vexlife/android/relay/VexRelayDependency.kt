package com.vextreme.vexlife.android.relay

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.flow.Flow

/**
 * One continuously observed dependency of a Compound Relay.
 *
 * The dependency owns only its exact observable source + Value -> InputState mapping. VexExecution
 * owns the maintained observation Job, cancellation, bounded source recovery, and root lifetime.
 */
abstract class VexRelayDependency<InputState : VexInputState<InputState>> internal constructor(
    val dependencyRef: String,
) {
    init {
        require(dependencyRef.isNotBlank()) {
            "dependencyRef must not be blank"
        }
    }

    internal abstract fun observeWith(
        executionRoot: VexExecutionRoot,
        owner: Any,
        send: (InputState) -> Unit,
    )
}

private class VexFlowRelayDependency<
    InputState : VexInputState<InputState>,
    Value,
>(
    dependencyRef: String,
    private val flow: Flow<Value>,
    private val toInputState: (Value) -> InputState,
) : VexRelayDependency<InputState>(
        dependencyRef,
    ) {
    private fun sourceForExecution(): Flow<Value> =
        flow

    override fun observeWith(
        executionRoot: VexExecutionRoot,
        owner: Any,
        send: (InputState) -> Unit,
    ) {
        VexExecution.observe(
            executionRoot =
                executionRoot,
            owner =
                owner,
            source =
                this::sourceForExecution,
        ) { value ->
            send(
                toInputState(
                    value,
                ),
            )
        }
    }
}

/**
 * Declarative dependency builder for one [VexCompoundRelay].
 *
 * Feature code names the dependency, names the observable source, and maps one observed value into
 * one partial Compound InputState. Execution custody stays with VexExecution.
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
            VexFlowRelayDependency(
                dependencyRef =
                    dependencyRef,
                flow =
                    flow,
                toInputState =
                    toInputState,
            )
    }

    internal fun build(): List<VexRelayDependency<InputState>> =
        dependencies.toList()
}

fun <InputState : VexInputState<InputState>> vexObserveDependencies(
    block: VexRelayDependencies<InputState>.() -> Unit,
): List<VexRelayDependency<InputState>> =
    VexRelayDependencies<InputState>()
        .apply(block)
        .build()

// [VXG RealForever]
