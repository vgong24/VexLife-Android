package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
        private val store:
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
            store =
                store,
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

                        is Control.Commit -> {
                            val nextState =
                                State.Ready(
                                    value =
                                        control.value,
                                )

                            admit(
                                nextState,
                                saveState =
                                    nextState,
                            )
                        }
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

    private class CounterFamily(
        executionRoot:
            VexExecutionRoot,
    ) :
        VexFamily<
            CounterFamily,
            CounterFamily.State,
            CounterFamily.Control
        >(
            initialState =
                State(
                    count =
                        0,
                ),
            executionRoot =
                executionRoot,
            logger =
                VexLogger.Sink.None,
        ) {
        data class State(
            val count:
                Int,
        ) :
            VexFamily.State<CounterFamily>

        sealed interface Control :
            VexFamily.Control<CounterFamily> {
            data object Increment :
                Control
        }

        override fun act(
            currentState:
                State,
            control:
                Control,
        ):
            VexFamily.Act<State> =
            when (
                control
            ) {
                Control.Increment ->
                    admit(
                        currentState.copy(
                            count =
                                currentState.count +
                                    1,
                        ),
                    )
            }
    }

    private class GatedStore :
        ValueFamily.Store {
        val started =
            CountDownLatch(
                1,
            )

        val gate =
            CompletableDeferred<Unit>()

        override suspend fun save(
            state:
                ValueFamily.State,
        ) {
            started.countDown()
            gate.await()
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
    fun availabilityMayInspectAnActWithSaveWithoutStartingItsExecution() {
        val root =
            VexExecutionRoot()
        val store =
            GatedStore()

        try {
            val family =
                ValueFamily(
                    initialState =
                        ValueFamily.State.Empty,
                    executionRoot =
                        root,
                    store =
                        store,
                )

            assertTrue(
                family.isAvailable(
                    ValueFamily.Control.Commit(
                        value =
                            "pending",
                    ),
                ),
            )
            assertEquals(
                1L,
                store.started.count,
            )
            assertEquals(
                ValueFamily.State.Empty,
                family.currentState,
            )

            assertTrue(
                family.activate(
                    ValueFamily.Control.Commit(
                        value =
                            "pending",
                    ),
                ),
            )
            assertEquals(
                ValueFamily.State.Ready(
                    value =
                        "pending",
                ),
                family.currentState,
            )

            assertTrue(
                "activated Save did not reach Store.save",
                store.started.await(
                    5,
                    TimeUnit.SECONDS,
                ),
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
    fun throwingLoggerCannotChangeFamilyControlStateOrStoreSemantics() {
        val root =
            VexExecutionRoot()
        val logger =
            VexLogger.Sink {
                throw IllegalStateException(
                    "synthetic logger failure",
                )
            }

        try {
            val immediate =
                ValueFamily(
                    initialState =
                        ValueFamily.State.Empty,
                    executionRoot =
                        root,
                    logger =
                        logger,
                )

            assertTrue(
                immediate.activate(
                    ValueFamily.Control.Set(
                        value =
                            "direct",
                    ),
                ),
            )
            assertEquals(
                ValueFamily.State.Ready(
                    value =
                        "direct",
                ),
                immediate.currentState,
            )

            val store =
                GatedStore()
            val saved =
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
                saved.activate(
                    ValueFamily.Control.Commit(
                        value =
                            "persisted",
                    ),
                ),
            )
            assertTrue(
                "throwing logger prevented Store.save",
                store.started.await(
                    5,
                    TimeUnit.SECONDS,
                ),
            )

            store.gate.complete(
                Unit,
            )

            runBlocking {
                withTimeout(
                    5_000,
                ) {
                    saved.states.first { state ->
                        state ==
                            ValueFamily.State.Ready(
                                value =
                                    "persisted",
                            )
                    }
                }
            }

            assertEquals(
                ValueFamily.State.Ready(
                    value =
                        "persisted",
                ),
                saved.currentState,
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun concurrentActivationsResolveAgainstOneSerializedCurrentState() {
        val root =
            VexExecutionRoot()
        val family =
            CounterFamily(
                executionRoot =
                    root,
            )
        val count =
            48
        val ready =
            CountDownLatch(
                count,
            )
        val go =
            CountDownLatch(
                1,
            )
        val executor =
            Executors.newFixedThreadPool(
                count,
            )

        try {
            val futures =
                (0 until count).map {
                    executor.submit(
                        Callable<Boolean> {
                            ready.countDown()
                            go.await()
                            family.activate(
                                CounterFamily.Control.Increment,
                            )
                        },
                    )
                }

            assertTrue(
                ready.await(
                    5,
                    TimeUnit.SECONDS,
                ),
            )
            go.countDown()

            futures.forEach { future ->
                assertTrue(
                    future.get(
                        5,
                        TimeUnit.SECONDS,
                    ),
                )
            }

            assertEquals(
                CounterFamily.State(
                    count =
                        count,
                ),
                family.currentState,
            )
            assertEquals(
                family.currentState,
                family.states.value,
            )
        } finally {
            executor.shutdownNow()
            root.forceStopAll()
        }
    }

    @Test
    fun olderSaveCompletionCannotOverwriteNewerFamilyState() {
        val root =
            VexExecutionRoot()
        val store =
            GatedStore()
        val operationSuccess =
            CountDownLatch(
                1,
            )
        val logger =
            RecordingLogger { event ->
                if (
                    event is
                        VexLogger.Event.Family.OperationSuccess
                ) {
                    operationSuccess.countDown()
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
                            "old-result",
                    ),
                ),
            )
            assertTrue(
                store.started.await(
                    5,
                    TimeUnit.SECONDS,
                ),
            )

            assertTrue(
                family.activate(
                    ValueFamily.Control.Set(
                        value =
                            "newer-current",
                    ),
                ),
            )

            store.gate.complete(
                Unit,
            )

            assertTrue(
                operationSuccess.await(
                    5,
                    TimeUnit.SECONDS,
                ),
            )

            runBlocking {
                withTimeout(
                    5_000,
                ) {
                    root.executions.first { executions ->
                        executions.values.any { state ->
                            state.status ==
                                VexExecution.Status.COMPLETED
                        }
                    }
                }
            }

            assertEquals(
                ValueFamily.State.Ready(
                    value =
                        "newer-current",
                ),
                family.currentState,
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun saveRunsAfterImmediateAdmissionAndOwnsNoLaterStateDoor() {
        val root =
            VexExecutionRoot()
        val store =
            GatedStore()
        val operationSuccess =
            CountDownLatch(
                1,
            )
        val logger =
            RecordingLogger { event ->
                if (
                    event is
                        VexLogger.Event.Family.OperationSuccess
                ) {
                    operationSuccess.countDown()
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
                ValueFamily.State.Ready(
                    value =
                        "loaded",
                ),
                family.currentState,
            )
            assertTrue(
                store.started.await(
                    5,
                    TimeUnit.SECONDS,
                ),
            )

            store.gate.complete(
                Unit,
            )
            assertTrue(
                operationSuccess.await(
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
