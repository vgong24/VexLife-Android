package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
                                    beginQueued {
                                        save(
                                            State.Ready(
                                                value =
                                                    control.value,
                                            ),
                                        ).results {
                                            success {
                                                admit { current ->
                                                    when (
                                                        current
                                                    ) {
                                                        State.Empty ->
                                                            State.Ready(
                                                                value =
                                                                    control.value,
                                                            )

                                                        is State.Ready ->
                                                            current
                                                    }
                                                }
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

    private class ResourceIdentityFamily(
        initialState:
            State,
        executionRoot:
            VexExecutionRoot,
    ) :
        VexFamily<
            ResourceIdentityFamily,
            ResourceIdentityFamily.State,
            ResourceIdentityFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
            logger =
                VexLogger.Sink.None,
        ) {
        sealed interface State :
            VexFamily.State<ResourceIdentityFamily> {
            data object Idle :
                State
        }

        data class TrackId(
            val value:
                String,
        ) :
            VexFamily.ResourceId<ResourceIdentityFamily>

        data class AccountId(
            val value:
                String,
        ) :
            VexFamily.ResourceId<ResourceIdentityFamily>

        sealed interface Control :
            VexFamily.Control<ResourceIdentityFamily> {
            data class Play(
                override val resourceId:
                    TrackId,
                val position:
                    Int,
            ) :
                Control,
                VexFamily.Control.Resource<
                    ResourceIdentityFamily,
                    TrackId
                >

            data class Search(
                val query:
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
                State.Idle ->
                    when (
                        control
                    ) {
                        is Control.Play ->
                            stay()

                        is Control.Search ->
                            stay()
                    }
            }
    }

    private class EnumControlFamily(
        initialState:
            State,
        executionRoot:
            VexExecutionRoot,
    ) :
        VexFamily<
            EnumControlFamily,
            EnumControlFamily.State,
            EnumControlFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
            logger =
                VexLogger.Sink.None,
        ) {
        sealed interface State :
            VexFamily.State<EnumControlFamily> {
            data object Idle :
                State
        }

        enum class Control :
            VexFamily.Control<EnumControlFamily> {
            Play,
            Pause,
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
                State.Idle ->
                    when (
                        control
                    ) {
                        Control.Play,
                        Control.Pause,
                        ->
                            stay()
                    }
            }
    }

    /**
     * Compile-time fixture: Play exposes TrackId exactly, not an arbitrary ResourceId for the Family.
     */
    private fun requireTrackId(
        control:
            ResourceIdentityFamily.Control.Play,
    ):
        ResourceIdentityFamily.TrackId =
        control.resourceId

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
        ):
            VexFamily.Store.WriteResult {
            started.countDown()
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
    fun relationshipIdentityPreservesControlKindExactResourceTypeAndLiveFamilyScope() {
        val root =
            VexExecutionRoot()

        try {
            val firstValueFamily =
                ValueFamily(
                    initialState =
                        ValueFamily.State.Empty,
                    executionRoot =
                        root,
                )
            val secondValueFamily =
                ValueFamily(
                    initialState =
                        ValueFamily.State.Empty,
                    executionRoot =
                        root,
                )

            val defaultFirst =
                firstValueFamily.relationshipId(
                    ValueFamily.Control.Set(
                        value =
                            "first payload",
                    ),
                )
            val defaultSecond =
                firstValueFamily.relationshipId(
                    ValueFamily.Control.Set(
                        value =
                            "second payload",
                    ),
                )
            val otherLiveFamily =
                secondValueFamily.relationshipId(
                    ValueFamily.Control.Set(
                        value =
                            "first payload",
                    ),
                )

            assertEquals(
                defaultFirst,
                defaultSecond,
            )
            assertFalse(
                defaultFirst ==
                    otherLiveFamily,
            )
            assertEquals(
                "Set",
                defaultFirst.controlName,
            )
            assertEquals(
                null,
                defaultFirst.resourceId,
            )

            val resourceFamily =
                ResourceIdentityFamily(
                    initialState =
                        ResourceIdentityFamily.State.Idle,
                    executionRoot =
                        root,
                )
            val trackA =
                ResourceIdentityFamily.TrackId(
                    value =
                        "same",
                )
            val trackACopy =
                ResourceIdentityFamily.TrackId(
                    value =
                        "same",
                )
            val trackB =
                ResourceIdentityFamily.TrackId(
                    value =
                        "other",
                )

            val narrowedFirst =
                resourceFamily.relationshipId(
                    ResourceIdentityFamily.Control.Play(
                        resourceId =
                            trackA,
                        position =
                            10,
                    ),
                )
            val narrowedSame =
                resourceFamily.relationshipId(
                    ResourceIdentityFamily.Control.Play(
                        resourceId =
                            trackACopy,
                        position =
                            99,
                    ),
                )
            val narrowedOther =
                resourceFamily.relationshipId(
                    ResourceIdentityFamily.Control.Play(
                        resourceId =
                            trackB,
                        position =
                            10,
                    ),
                )

            assertEquals(
                narrowedFirst,
                narrowedSame,
            )
            assertFalse(
                narrowedFirst ==
                    narrowedOther,
            )
            assertEquals(
                trackA,
                narrowedFirst.resourceId,
            )
            assertEquals(
                trackA,
                requireTrackId(
                    ResourceIdentityFamily.Control.Play(
                        resourceId =
                            trackA,
                        position =
                            0,
                    ),
                ),
            )

            val enumFamily =
                EnumControlFamily(
                    initialState =
                        EnumControlFamily.State.Idle,
                    executionRoot =
                        root,
                )
            val play =
                enumFamily.relationshipId(
                    EnumControlFamily.Control.Play,
                )
            val pause =
                enumFamily.relationshipId(
                    EnumControlFamily.Control.Pause,
                )

            assertFalse(
                play ==
                    pause,
            )
            assertEquals(
                "Play",
                play.controlName,
            )
            assertEquals(
                "Pause",
                pause.controlName,
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun availabilityMayInspectAnActWithBeginWithoutStartingItsExecution() {
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

            assertTrue(
                "activated Begin did not reach Store.save",
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
    fun throwingLoggerCannotChangeFamilyControlStateStoreOrSuccessSemantics() {
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
                    executor.submit {
                        ready.countDown()
                        go.await()
                        family.activate(
                            CounterFamily.Control.Increment,
                        )
                    }
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
    fun olderAsyncResultReevaluatesAgainstCurrentStateBeforeAdmission() {
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
