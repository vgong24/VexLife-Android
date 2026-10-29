package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused proof of the human-readable Family surface.
 */
class VexFamilyHighConfidenceTest {
    private class ValueFamily(
        initialState:
            State,
        executionRoot:
            VexExecutionRoot,
        override val store:
            Store? =
            null,
        logger:
            VexLogger.Sink =
            VexLogger.Sink.None,
    ) :
        VexFamily<
            ValueFamily,
            ValueFamily.State,
            ValueFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
            logger =
                logger,
        ) {
        sealed interface State :
            VexFamily.State<ValueFamily> {
            data object Empty :
                State

            data class Ready(
                val value:
                    String,
            ) :
                State
        }

        sealed interface Control :
            VexFamily.Control<ValueFamily> {
            data class Set(
                val value:
                    String,
            ) :
                Control

            data class Commit(
                val value:
                    String,
            ) :
                Control
        }

        enum class ExecutionId :
            VexExecution.Id {
            Save,
        }

        interface Store :
            VexFamily.Store<State>

        data class DisplayState(
            val value:
                String?,
        ) :
            VexFamily.DisplayState<State>

        /**
         * This is the function Family.activate(...) calls.
         *
         * State and Control traversal are ordinary Kotlin sealed-type when expressions.
         */
        override fun act(
            currentState: State,
            control: Control,
        ):
            VexFamily.Act<State>? =
            when (
                currentState
            ) {
                State.Empty ->
                    when (
                        control
                    ) {
                        is Control.Set ->
                            admit(
                                State.Ready(
                                    value =
                                        control.value,
                                ),
                            )

                        is Control.Commit ->
                            stay(
                                begin =
                                    beginQueued(
                                        id =
                                            ExecutionId.Save,
                                    ) {
                                        save(
                                            State.Ready(
                                                value =
                                                    control.value,
                                            ),
                                        ).results {
                                            success {
                                                admit(
                                                    State.Ready(
                                                        value =
                                                            control.value,
                                                    ),
                                                )
                                            }

                                            failure {
                                            }
                                        }
                                    },
                            )
                    }

                is State.Ready ->
                    when (
                        control
                    ) {
                        is Control.Set ->
                            admit(
                                currentState.copy(
                                    value =
                                        control.value,
                                ),
                            )

                        is Control.Commit ->
                            noAct()
                    }
            }

        override val displayState:
            DisplayState
            get() =
                DisplayState(
                    value =
                        when (
                            val state =
                                currentState
                        ) {
                            State.Empty ->
                                null

                            is State.Ready ->
                                state.value
                        },
                )
    }

    private class MinimalFamily(
        initialState:
            State,
        executionRoot:
            VexExecutionRoot,
    ) :
        VexFamily<
            MinimalFamily,
            MinimalFamily.State,
            MinimalFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
            logger =
                VexLogger.Sink.None,
        ) {
        sealed interface State :
            VexFamily.State<MinimalFamily> {
            data object Idle :
                State
        }

        sealed interface Control :
            VexFamily.Control<MinimalFamily> {
            data object Keep :
                Control
        }

        override fun act(
            currentState: State,
            control: Control,
        ):
            VexFamily.Act<State>? =
            when (
                currentState
            ) {
                State.Idle ->
                    when (
                        control
                    ) {
                        Control.Keep ->
                            stay()
                    }
            }

    }

    private class GatedStore :
        ValueFamily.Store {
        val gate =
            CompletableDeferred<Unit>()

        override suspend fun save(
            state:
                ValueFamily.State,
        ):
            VexFamily.Store.WriteResult {
            gate.await()
            return VexFamily.Store.WriteResult.Success
        }
    }

    private class RecordingLogger(
        private val onRecord:
            (
                VexLogger.Event,
            ) -> Unit,
    ) :
        VexLogger.Sink {
        override fun record(
            event:
                VexLogger.Event,
        ) {
            onRecord(
                event,
            )
        }
    }

    @Test
    fun activateCallsTheConcreteFamilyActFunctionThenAdmitsIntoTheSameStateRelay() {
        val root =
            VexExecutionRoot()

        try {
            val family =
                ValueFamily(
                    initialState =
                        ValueFamily.State.Empty,
                    executionRoot =
                        root,
                )

            assertTrue(
                family.activate(
                    ValueFamily.Control.Set(
                        value =
                            "one",
                    ),
                ),
            )
            assertEquals(
                ValueFamily.State.Ready(
                    value =
                        "one",
                ),
                family.currentState,
            )
            assertEquals(
                family.currentState,
                family.states.value,
            )

            assertTrue(
                family.activate(
                    ValueFamily.Control.Set(
                        value =
                            "two",
                    ),
                ),
            )
            assertEquals(
                ValueFamily.State.Ready(
                    value =
                        "two",
                ),
                family.currentState,
            )
            assertEquals(
                family.currentState,
                family.states.value,
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun availabilityAndActivationCallTheSameActFunctionWithoutASeparateActsTable() {
        val root =
            VexExecutionRoot()

        try {
            val family =
                ValueFamily(
                    initialState =
                        ValueFamily.State.Ready(
                            value =
                                "one",
                        ),
                    executionRoot =
                        root,
                )

            val sameValue =
                ValueFamily.Control.Set(
                    value =
                        "one",
                )
            val change =
                ValueFamily.Control.Set(
                    value =
                        "two",
                )

            assertTrue(
                family.isAvailable(
                    sameValue,
                ),
            )
            assertTrue(
                family.activate(
                    sameValue,
                ),
            )
            assertEquals(
                ValueFamily.State.Ready(
                    value =
                        "one",
                ),
                family.currentState,
            )

            assertTrue(
                family.isAvailable(
                    change,
                ),
            )
            assertTrue(
                family.activate(
                    change,
                ),
            )
            assertEquals(
                ValueFamily.State.Ready(
                    value =
                        "two",
                ),
                family.currentState,
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun displayStateDefaultsToStateOnlyWhenNoRicherClassificationIsEarned() {
        val root =
            VexExecutionRoot()

        try {
            val family =
                MinimalFamily(
                    initialState =
                        MinimalFamily.State.Idle,
                    executionRoot =
                        root,
                )

            assertEquals(
                VexFamily.DisplayState.StateOnly(
                    MinimalFamily.State.Idle,
                ),
                family.displayState,
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun beginSaveSuccessMayAdmitTheAuthoredLaterState() {
        val root =
            VexExecutionRoot()
        val store =
            GatedStore()
        val laterAdmission =
            CountDownLatch(
                1,
            )
        val logger =
            RecordingLogger { event ->
                if (
                    event is
                        VexLogger.Event.Family.StateAdmitted
                ) {
                    laterAdmission.countDown()
                }
            }

        try {
            val family =
                ValueFamily(
                    initialState =
                        ValueFamily.State.Empty,
                    executionRoot =
                        root,
                    store =
                        store,
                    logger =
                        logger,
                )

            assertTrue(
                family.activate(
                    ValueFamily.Control.Commit(
                        value =
                            "loaded",
                    ),
                ),
            )
            assertEquals(
                ValueFamily.State.Empty,
                family.currentState,
            )

            store.gate.complete(
                Unit,
            )

            assertTrue(
                "later State admission was not observed",
                laterAdmission.await(
                    5,
                    TimeUnit.SECONDS,
                ),
            )
            assertEquals(
                ValueFamily.State.Ready(
                    value =
                        "loaded",
                ),
                family.currentState,
            )
        } finally {
            root.forceStopAll()
        }
    }
}

// [VXG RealForever]
