package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import com.vextreme.vexlife.android.relay.VexCompoundRelay
import com.vextreme.vexlife.android.relay.VexCompoundState
import com.vextreme.vexlife.android.relay.VexInputState
import com.vextreme.vexlife.android.relay.VexOutputState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-root proof of the readable Family / Compound binding.
 *
 * Compound owns relationship output. It never writes Family State. When the output asks another
 * Family to act, feature composition forms that Family's Control; Family re-resolves CURRENT State,
 * admits its own State, and that new owner truth returns as a fresh Compound input.
 */
class VexFamilyCompoundBindingTest {
    private class TargetFamily(
        executionRoot:
            VexExecutionRoot,
    ) :
        VexFamily<
            TargetFamily,
            TargetFamily.State,
            TargetFamily.Control
        >(
            initialState =
                State.Empty,
            executionRoot =
                executionRoot,
            logger =
                VexLogger.Sink.None,
        ) {
        sealed interface State :
            VexFamily.State<TargetFamily> {
            data object Empty :
                State

            data class Current(
                val value:
                    String,
            ) :
                State
        }

        sealed interface Control :
            VexFamily.Control<TargetFamily> {
            data class Apply(
                val value:
                    String,
            ) :
                Control
        }

        override fun act(
            currentState:
                State,
            control:
                Control,
        ):
            VexFamily.Act<State>? =
            when (
                currentState
            ) {
                State.Empty ->
                    when (
                        control
                    ) {
                        is Control.Apply ->
                            admit(
                                State.Current(
                                    control.value,
                                ),
                            )
                    }

                is State.Current ->
                    when (
                        control
                    ) {
                        is Control.Apply ->
                            if (
                                currentState.value ==
                                control.value
                            ) {
                                stay()
                            } else {
                                admit(
                                    State.Current(
                                        control.value,
                                    ),
                                )
                            }
                    }
            }
    }

    private data class InputState(
        val desired:
            String? =
            null,
        val actual:
            String? =
            null,
    ) :
        VexInputState<
            InputState
        > {
        override fun deepCopy():
            InputState =
            copy()
    }

    private sealed interface OutputState :
        VexOutputState<
            OutputState
        > {
        data object Unobserved :
            OutputState

        data class NeedsApply(
            val value:
                String,
        ) :
            OutputState

        data class Settled(
            val value:
                String,
        ) :
            OutputState

        override fun deepCopy():
            OutputState =
            when (
                this
            ) {
                Unobserved ->
                    Unobserved

                is NeedsApply ->
                    copy()

                is Settled ->
                    copy()
            }
    }

    private class Relationship :
        VexCompoundRelay<
            InputState,
            OutputState
        >(
            relayRef =
                "family.compound-binding",
            initialInputState =
                InputState(),
            initialOutputState =
                OutputState.Unobserved,
        ) {
        fun desired(
            value:
                String,
        ) {
            send(
                InputState(
                    desired =
                        value,
                ),
            )
        }

        fun actual(
            value:
                String,
        ) {
            send(
                InputState(
                    actual =
                        value,
                ),
            )
        }

        override fun processStates(
            inputState:
                InputState,
            currentState:
                VexCompoundState<
                    InputState,
                    OutputState
                >,
        ):
            VexCompoundState<
                InputState,
                OutputState
            > {
            val currentInput =
                currentState.inputState

            val coherent =
                InputState(
                    desired =
                        inputState.desired
                            ?: currentInput.desired,
                    actual =
                        inputState.actual
                            ?: currentInput.actual,
                )

            val output =
                when (
                    val desired =
                        coherent.desired
                ) {
                    null ->
                        OutputState.Unobserved

                    else ->
                        if (
                            coherent.actual ==
                            desired
                        ) {
                            OutputState.Settled(
                                desired,
                            )
                        } else {
                            OutputState.NeedsApply(
                                desired,
                            )
                        }
                }

            return currentState.copy(
                inputState =
                    coherent,
                outputState =
                    output,
            )
        }
    }

    @Test
    fun compoundOutputRequestsTargetFamilyThroughControlAndObservedTruthReturnsAsNewInput() {
        val root =
            VexExecutionRoot()

        try {
            val target =
                TargetFamily(
                    executionRoot =
                        root,
                )
            val relationship =
                Relationship()

            relationship.desired(
                "dark",
            )

            assertEquals(
                OutputState.NeedsApply(
                    "dark",
                ),
                relationship.currentOutput,
            )

            val request =
                requireNotNull(
                    relationship.currentOutput as?
                        OutputState.NeedsApply,
                )

            assertTrue(
                target.activate(
                    TargetFamily.Control.Apply(
                        request.value,
                    ),
                ),
            )

            val actual =
                requireNotNull(
                    target.currentState as?
                        TargetFamily.State.Current,
                )

            relationship.actual(
                actual.value,
            )

            assertEquals(
                OutputState.Settled(
                    "dark",
                ),
                relationship.currentOutput,
            )
            assertEquals(
                TargetFamily.State.Current(
                    "dark",
                ),
                target.currentState,
            )
        } finally {
            root.forceStopAll()
        }
    }
}

// [VXG RealForever]
