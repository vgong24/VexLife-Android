package com.vextreme.vexlife.android.relay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class VexCompoundRelayDependencyTest {
    private data class DependencyInput(
        val left: Int? = null,
        val right: Int? = null,
    ) : VexInputState<DependencyInput> {
        override fun deepCopy(): DependencyInput =
            copy()
    }

    private data class DependencyOutput(
        val sum: Int = 0,
    ) : VexOutputState<DependencyOutput> {
        override fun deepCopy(): DependencyOutput =
            copy()
    }

    private class DependencyRelay(
        private val left: MutableStateFlow<Int>,
        private val right: MutableStateFlow<Int>,
    ) : VexCompoundRelay<DependencyInput, DependencyOutput>(
        relayRef = "relay.test.dependencies",
        initialInputState = DependencyInput(),
        initialOutputState = DependencyOutput(),
    ) {
        override fun dependencies() =
            vexDependencies<DependencyInput> {
                observe(
                    dependencyRef = "left",
                    flow = left,
                ) { value ->
                    DependencyInput(
                        left = value,
                    )
                }

                observe(
                    dependencyRef = "right",
                    flow = right,
                ) { value ->
                    DependencyInput(
                        right = value,
                    )
                }
            }

        override fun processStates(
            inputState: DependencyInput,
            currentState: VexCompoundState<DependencyInput, DependencyOutput>,
        ): VexCompoundState<DependencyInput, DependencyOutput> {
            val currentInput =
                currentState.inputState

            val coherent =
                DependencyInput(
                    left =
                        inputState.left
                            ?: currentInput.left,
                    right =
                        inputState.right
                            ?: currentInput.right,
                )

            return currentState.copy(
                inputState = coherent,
                outputState =
                    DependencyOutput(
                        sum =
                            (coherent.left ?: 0) +
                                (coherent.right ?: 0),
                    ),
            )
        }
    }

    @Test
    fun eachDeclaredDependencyCollectsIndependentlyAndConvergesAtomically() = runBlocking {
        val left =
            MutableStateFlow(10)
        val right =
            MutableStateFlow(32)
        val relay =
            DependencyRelay(
                left = left,
                right = right,
            )

        try {
            relay.activate(this)

            val initial =
                withTimeout(1_000L) {
                    relay.states.first { state ->
                        state.inputState.left == 10 &&
                            state.inputState.right == 32
                    }
                }

            assertEquals(42, initial.outputState.sum)

            left.value = 7
            right.value = 5

            val changed =
                withTimeout(1_000L) {
                    relay.states.first { state ->
                        state.inputState.left == 7 &&
                            state.inputState.right == 5
                    }
                }

            assertEquals(12, changed.outputState.sum)
        } finally {
            relay.deactivate()
        }
    }
}

// [VXG RealForever]
