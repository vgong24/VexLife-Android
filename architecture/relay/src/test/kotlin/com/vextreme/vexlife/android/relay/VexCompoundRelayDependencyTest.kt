package com.vextreme.vexlife.android.relay

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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

    private data class ChildInput(
        val value: Int? = null,
        val context: String? = null,
    ) : VexInputState<ChildInput>

    private data class ChildOutput(
        val bucket: Int = 0,
    ) : VexOutputState<ChildOutput>

    private class ChildCompoundRelay :
        VexCompoundRelay<ChildInput, ChildOutput>(
            relayRef = "relay.test.nested.child",
            initialInputState = ChildInput(),
            initialOutputState = ChildOutput(),
        ) {
        val processCount =
            AtomicInteger(0)

        fun update(
            value: Int? = null,
            context: String? = null,
        ) {
            send(
                ChildInput(
                    value = value,
                    context = context,
                ),
            )
        }

        override fun processStates(
            inputState: ChildInput,
            currentState: VexCompoundState<ChildInput, ChildOutput>,
        ): VexCompoundState<ChildInput, ChildOutput> {
            processCount.incrementAndGet()

            val previous =
                currentState.inputState
            val coherent =
                previous.copy(
                    value =
                        inputState.value
                            ?: previous.value,
                    context =
                        inputState.context
                            ?: previous.context,
                )

            return currentState.copy(
                inputState = coherent,
                outputState =
                    ChildOutput(
                        bucket =
                            (coherent.value ?: 0) / 10,
                    ),
            )
        }
    }

    private data class ParentInput(
        val child: ChildOutput? = null,
        val multiplier: Int? = null,
    ) : VexInputState<ParentInput>

    private data class ParentOutput(
        val score: Int = 0,
    ) : VexOutputState<ParentOutput>

    private class ParentCompoundRelay(
        private val child: ChildCompoundRelay,
        private val multiplier: MutableStateFlow<Int>,
    ) : VexCompoundRelay<ParentInput, ParentOutput>(
        relayRef = "relay.test.nested.parent",
        initialInputState = ParentInput(),
        initialOutputState = ParentOutput(),
    ) {
        val processCount =
            AtomicInteger(0)

        override fun dependencies() =
            vexDependencies<ParentInput> {
                observe(
                    dependencyRef = "child.output",
                    flow = child.outputStates,
                ) { output ->
                    ParentInput(
                        child = output,
                    )
                }

                observe(
                    dependencyRef = "multiplier",
                    flow = multiplier,
                ) { value ->
                    ParentInput(
                        multiplier = value,
                    )
                }
            }

        override fun processStates(
            inputState: ParentInput,
            currentState: VexCompoundState<ParentInput, ParentOutput>,
        ): VexCompoundState<ParentInput, ParentOutput> {
            processCount.incrementAndGet()

            val previous =
                currentState.inputState
            val coherent =
                previous.copy(
                    child =
                        inputState.child
                            ?: previous.child,
                    multiplier =
                        inputState.multiplier
                            ?: previous.multiplier,
                )

            return currentState.copy(
                inputState = coherent,
                outputState =
                    ParentOutput(
                        score =
                            (coherent.child?.bucket ?: 0) *
                                (coherent.multiplier ?: 0),
                    ),
            )
        }
    }

    @Test
    fun nestedCompoundConsumesOnlyChangedChildMeaningAndSharesImmutableOutput() = runBlocking {
        val child =
            ChildCompoundRelay()
        val multiplier =
            MutableStateFlow(2)
        val parent =
            ParentCompoundRelay(
                child = child,
                multiplier = multiplier,
            )

        try {
            parent.activate(this)

            withTimeout(1_000L) {
                parent.states.first { state ->
                    state.inputState.child != null &&
                        state.inputState.multiplier == 2
                }
            }

            assertSame(
                child.currentOutput,
                parent.currentInput.child,
            )

            val parentProcessesBeforeInputOnlyChildChange =
                parent.processCount.get()

            /*
             * Child retained input changes (0 -> 7), but its semantic output bucket remains 0.
             * outputStates owns distinct-by-output, so the parent does not reprocess.
             */
            child.update(
                value = 7,
                context = "changed-without-output-change",
            )
            yield()

            assertEquals(
                parentProcessesBeforeInputOnlyChildChange,
                parent.processCount.get(),
            )
            assertSame(
                child.currentOutput,
                parent.currentInput.child,
            )

            child.update(
                value = 10,
            )

            withTimeout(1_000L) {
                parent.outputStates.first { output ->
                    output.score == 2
                }
            }

            assertEquals(
                parentProcessesBeforeInputOnlyChildChange + 1,
                parent.processCount.get(),
            )
            assertSame(
                child.currentOutput,
                parent.currentInput.child,
            )
        } finally {
            parent.deactivate()
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
