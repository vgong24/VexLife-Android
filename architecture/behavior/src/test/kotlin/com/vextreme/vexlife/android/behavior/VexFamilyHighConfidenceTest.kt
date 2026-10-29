package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            VexFamilyLogger =
            VexFamilyLogger.None,
    ) :
        VexFamily<
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
        sealed interface State {
            data object Empty :
                State

            data class Ready(
                val value:
                    String,
            ) :
                State
        }

        sealed interface Control {
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

        interface Store :
            VexFamilyStateStore<State>

        data class DisplayState(
            val value:
                String?,
        ) :
            VexDisplayState<State>

        /**
         * This is the function Family.activate(...) calls.
         *
         * State and Control traversal are ordinary Kotlin sealed-type when expressions.
         */
        override fun act(
            currentState: State,
            control: Control,
        ):
            VexAct<State>? =
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
                                    begin(
                                        mode =
                                            VexBeginMode.Sequenced,
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
                            null
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
            MinimalFamily.State,
            MinimalFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
            logger =
                VexFamilyLogger.None,
        ) {
        sealed interface State {
            data object Idle :
                State
        }

        sealed interface Control {
            data object Keep :
                Control
        }

        override fun act(
            currentState: State,
            control: Control,
        ):
            VexAct<State>? =
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
        private val mutableState =
            MutableStateFlow<ValueFamily.State>(
                ValueFamily.State.Empty,
            )

        override val observableState =
            mutableState.asStateFlow()

        val gate =
            CompletableDeferred<Unit>()

        override suspend fun get():
            ValueFamily.State =
            mutableState.value

        override suspend fun save(
            state:
                ValueFamily.State,
        ):
            VexStoreWriteResult {
            gate.await()
            mutableState.value =
                state
            return VexStoreWriteResult.Success
        }

        override suspend fun forget():
            VexStoreWriteResult {
            mutableState.value =
                ValueFamily.State.Empty
            return VexStoreWriteResult.Success
        }
    }

    private class RecordingLogger(
        private val onRecord:
            (
                VexFamilyLogEvent,
            ) -> Unit,
    ) :
        VexFamilyLogger {
        override fun record(
            event:
                VexFamilyLogEvent,
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
    fun displayStateRoleRemainsPresentWhenNoRicherProjectionIsEarned() {
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
                VexDisplayState.Empty(
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
                        VexFamilyLogEvent.StateAdmitted
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
