@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Focused proof of the refined Family semantic and acceptance surface.
 *
 * This file proves Family-owned resolution, acceptance, and the acceptance-to-custody boundary.
 * VexExecution overlap mechanics, persistence, and logging remain proved by their rightful owners.
 */
class VexFamilyHighConfidenceTest {
    private class ValueFamily(
        initialState: State,
    ) :
        VexFamily<
            ValueFamily,
            ValueFamily.State,
            ValueFamily.Control,
            ValueFamily.Effect
        >(
            initialState = initialState,
        ) {
        sealed interface State :
            VexFamily.State<ValueFamily> {
            data object Empty :
                State

            data class Ready(
                val value: String,
            ) :
                State
        }

        sealed interface Control :
            VexFamily.Control<ValueFamily> {
            data class Set(
                val value: String,
            ) :
                Control

            data class Save(
                val value: String,
            ) :
                Control
        }

        sealed interface Effect :
            VexFamily.Effect<ValueFamily> {
            data class Save(
                val value: String,
            ) :
                Effect
        }

        class SaveMethod(
            override val effect:
                Effect.Save,
            private val shouldFail: Boolean,
        ) :
            VexFamily.Act.Method<
                ValueFamily,
                String,
                Throwable?
            > {
            override suspend fun invoke():
                VexMethod.Result<
                    String,
                    Throwable?
                > =
                if (shouldFail) {
                    VexMethod.Result.Failure(
                        output =
                            IllegalStateException(
                                "rejected",
                            ),
                    )
                } else {
                    VexMethod.Result.Success(
                        output =
                            effect.value,
                    )
                }
        }

        class ThrowingSaveMethod(
            override val effect:
                Effect.Save,
        ) :
            VexFamily.Act.Method<
                ValueFamily,
                String,
                Throwable?
            > {
            override suspend fun invoke():
                VexMethod.Result<
                    String,
                    Throwable?
                > =
                error(
                    "synthetic Method crash",
                )
        }

        data class DisplayState(
            val value: String?,
        ) :
            VexFamily.DisplayState<
                ValueFamily,
                State
            >

        override fun displayStateFor(
            state: State,
        ):
            DisplayState =
            DisplayState(
                value =
                    when (state) {
                        State.Empty ->
                            null

                        is State.Ready ->
                            state.value
                    },
            )

        override fun act(
            currentState: State,
            control: Control,
        ):
            VexFamily.Act<
                ValueFamily,
                State,
                Effect
            >? =
            when (control) {
                is Control.Set ->
                    admit(
                        State.Ready(
                            value = control.value,
                        ),
                    )

                is Control.Save ->
                    stay(
                        begin(
                            Effect.Save(
                                value = control.value,
                            ),
                        ),
                    )
            }
    }

    private class CounterFamily :
        VexFamily<
            CounterFamily,
            CounterFamily.State,
            CounterFamily.Control,
            CounterFamily.Effect
        >(
            initialState =
                State(
                    count = 0,
                ),
        ) {
        data class State(
            val count: Int,
        ) :
            VexFamily.State<CounterFamily>

        data object Control :
            VexFamily.Control<CounterFamily>

        sealed interface Effect :
            VexFamily.Effect<CounterFamily>

        override fun act(
            currentState: State,
            control: Control,
        ):
            VexFamily.Act<
                CounterFamily,
                State,
                Effect
            > =
            admit(
                currentState.copy(
                    count =
                        currentState.count +
                            1,
                ),
            )
    }

    private class MaintainedFamily(
        private val approach:
            VexExecution.Approach,
    ) :
        VexFamily<
            MaintainedFamily,
            MaintainedFamily.State,
            MaintainedFamily.Control,
            MaintainedFamily.Effect
        >(
            initialState =
                State,
        ) {
        data object State :
            VexFamily.State<MaintainedFamily>

        data class Control(
            val value:
                String,
        ) :
            VexFamily.Control<MaintainedFamily>

        data class Effect(
            val value:
                String,
        ) :
            VexFamily.Effect<MaintainedFamily>

        class SaveMethod(
            override val effect:
                Effect,
        ) :
            VexFamily.Act.Method<
                MaintainedFamily,
                String,
                Unit
            > {
            override suspend fun invoke():
                VexMethod.Result<
                    String,
                    Unit
                > =
                VexMethod.Result.Success(
                    output =
                        effect.value,
                )
        }

        override fun act(
            currentState:
                State,
            control:
                Control,
        ):
            VexFamily.Act<
                MaintainedFamily,
                State,
                Effect
            > {
            val effect =
                Effect(
                    value =
                        control.value,
                )

            return stay(
                begin =
                    when (
                        approach
                    ) {
                        VexExecution.Approach.Independent ->
                            begin(
                                effect,
                            )

                        VexExecution.Approach.Earliest ->
                            beginEarliest(
                                effect,
                            )

                        VexExecution.Approach.Latest ->
                            beginLatest(
                                effect,
                            )

                        VexExecution.Approach.Queued ->
                            beginQueued(
                                effect,
                            )
                    },
            )
        }
    }

    @Test
    fun activateResolvesCurrentStateControlAndReturnsTheAuthoredEffect() {
        val family =
            ValueFamily(
                initialState =
                    ValueFamily.State.Empty,
            )

        val admitted =
            family.activate(
                ValueFamily.Control.Set(
                    value = "one",
                ),
            )

        assertNotNull(
            admitted,
        )
        assertEquals(
            ValueFamily.State.Ready(
                value = "one",
            ),
            family.currentState,
        )
        assertEquals(
            family.currentState,
            family.states.value,
        )

        val accepted =
            family.activate(
                ValueFamily.Control.Save(
                    value = "one",
                ),
            )

        assertEquals(
            ValueFamily.Effect.Save(
                value = "one",
            ),
            accepted
                ?.authored
                ?.begin
                ?.effect,
        )
        assertEquals(
            ValueFamily.State.Ready(
                value = "one",
            ),
            family.currentState,
        )
    }

    @Test
    fun boundFamilyMethodUsesContractOwnedResultCarriers() =
        runBlocking {
            val success =
                ValueFamily.SaveMethod(
                    effect =
                        ValueFamily.Effect.Save(
                            value = "saved",
                        ),
                    shouldFail = false,
                )()

            val failure =
                ValueFamily.SaveMethod(
                    effect =
                        ValueFamily.Effect.Save(
                            value = "ignored",
                        ),
                    shouldFail = true,
                )()

            val successResult =
                success as
                    VexMethod.Result.Success<String>
            assertEquals(
                "saved",
                successResult.output,
            )

            val failureResult =
                failure as
                    VexMethod.Result.Failure<Throwable?>
            assertEquals(
                "rejected",
                failureResult.output
                    ?.message,
            )
        }

    @Test
    fun escapedMethodExceptionReturnsMethodErrorThroughTheSameResultChannel() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val family =
                ValueFamily(
                    initialState =
                        ValueFamily.State.Empty,
                )
            val act =
                checkNotNull(
                    family.activate(
                        ValueFamily.Control.Save(
                            value =
                                "boom",
                        ),
                    ),
                )
            var received:
                VexMethod.Result<
                    String,
                    Throwable?
                >? =
                null

            try {
                val begin =
                    checkNotNull(
                        act.realizationBegin,
                    )
                val effect =
                    begin.effect as
                        ValueFamily.Effect.Save
                val job =
                    checkNotNull(
                        act.launch(
                            executionRoot =
                                root,
                            method =
                                ValueFamily.ThrowingSaveMethod(
                                    effect =
                                        effect,
                                ),
                            onResult = { result ->
                                received =
                                    result
                            },
                        ),
                    )

                job.join()

                val error =
                    received as
                        VexMethod.Result.Error
                assertEquals(
                    "synthetic Method crash",
                    error.cause.message,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun acceptedActRejectsAMethodThatReconstructsItsEffect() {
        val root =
            VexExecutionRoot()
        val family =
            ValueFamily(
                initialState =
                    ValueFamily.State.Empty,
            )
        val act =
            checkNotNull(
                family.activate(
                    ValueFamily.Control.Save(
                        value =
                            "one",
                    ),
                ),
            )
        val copiedEffect =
            ValueFamily.Effect.Save(
                value =
                    "one",
            )

        try {
            val failure =
                runCatching {
                    act.launch(
                        executionRoot =
                            root,
                        method =
                            ValueFamily.SaveMethod(
                                effect =
                                    copiedEffect,
                                shouldFail =
                                    false,
                            ),
                    )
                }
                    .exceptionOrNull()

            assertTrue(
                failure is
                    IllegalStateException,
            )
        } finally {
            root.forceStopAll(
                "test complete",
            )
        }
    }

    @Test
    fun latestAcceptancePreservesAuthoredBeginWhileSupersedingOlderPendingRealization() {
        val family =
            MaintainedFamily(
                approach =
                    VexExecution.Approach.Latest,
            )
        val first =
            checkNotNull(
                family.activate(
                    MaintainedFamily.Control(
                        value =
                            "first",
                    ),
                ),
            )
        val firstAuthoredBegin =
            checkNotNull(
                first.authored.begin,
            )

        assertNotNull(
            first.realizationBegin,
        )

        val second =
            checkNotNull(
                family.activate(
                    MaintainedFamily.Control(
                        value =
                            "second",
                    ),
                ),
            )

        assertSame(
            firstAuthoredBegin,
            first.authored.begin,
        )
        assertEquals(
            null,
            first.realizationBegin,
        )
        assertNotNull(
            second.realizationBegin,
        )
        assertEquals(
            first.relationshipId,
            second.relationshipId,
        )
    }

    @Test
    fun earliestAcceptancePreservesAuthoredBeginWhenEarlierRealizationAlreadyCoversIt() {
        val family =
            MaintainedFamily(
                approach =
                    VexExecution.Approach.Earliest,
            )
        val first =
            checkNotNull(
                family.activate(
                    MaintainedFamily.Control(
                        value =
                            "first",
                    ),
                ),
            )
        val second =
            checkNotNull(
                family.activate(
                    MaintainedFamily.Control(
                        value =
                            "second",
                    ),
                ),
            )

        assertNotNull(
            first.realizationBegin,
        )
        assertNotNull(
            second.authored.begin,
        )
        assertEquals(
            null,
            second.realizationBegin,
        )
        assertEquals(
            first.relationshipId,
            second.relationshipId,
        )
    }

    @Test
    fun queuedAcceptanceRejectsLaterRealizationBeforeEarlierPendingAcceptance() {
        val root =
            VexExecutionRoot()
        val family =
            MaintainedFamily(
                approach =
                    VexExecution.Approach.Queued,
            )
        val first =
            checkNotNull(
                family.activate(
                    MaintainedFamily.Control(
                        value =
                            "first",
                    ),
                ),
            )
        val second =
            checkNotNull(
                family.activate(
                    MaintainedFamily.Control(
                        value =
                            "second",
                    ),
                ),
            )
        val secondBegin =
            checkNotNull(
                second.realizationBegin,
            )

        try {
            assertNotNull(
                first.realizationBegin,
            )
            assertEquals(
                first.relationshipId,
                second.relationshipId,
            )

            val failure =
                runCatching {
                    second.launch(
                        executionRoot =
                            root,
                        method =
                            MaintainedFamily.SaveMethod(
                                effect =
                                    secondBegin.effect,
                            ),
                    )
                }
                    .exceptionOrNull()

            assertTrue(
                failure is
                    IllegalStateException,
            )
        } finally {
            root.forceStopAll(
                "test complete",
            )
        }
    }

    @Test
    fun oneAcceptedActCannotEnterExecutionCustodyTwice() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val family =
                ValueFamily(
                    initialState =
                        ValueFamily.State.Empty,
                )
            val accepted =
                checkNotNull(
                    family.activate(
                        ValueFamily.Control.Save(
                            value =
                                "one",
                        ),
                    ),
                )
            val begin =
                checkNotNull(
                    accepted.realizationBegin,
                )
            val effect =
                begin.effect as
                    ValueFamily.Effect.Save
            val method =
                ValueFamily.SaveMethod(
                    effect =
                        effect,
                    shouldFail =
                        false,
                )

            try {
                checkNotNull(
                    accepted.launch(
                        executionRoot =
                            root,
                        method =
                            method,
                    ),
                )
                    .join()

                val failure =
                    runCatching {
                        accepted.launch(
                            executionRoot =
                                root,
                            method =
                                method,
                        )
                    }
                        .exceptionOrNull()

                assertTrue(
                    failure is
                        IllegalStateException,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun availabilityInspectsTheSameRelationshipWithoutMutatingStateOrRealizingEffect() {
        val family =
            ValueFamily(
                initialState =
                    ValueFamily.State.Empty,
            )

        assertTrue(
            family.isAvailable(
                ValueFamily.Control.Save(
                    value = "pending",
                ),
            ),
        )
        assertEquals(
            ValueFamily.State.Empty,
            family.currentState,
        )
    }

    @Test
    fun observedOutsideTruthEntersTheSameFamilyStateRelay() {
        val family =
            ValueFamily(
                initialState =
                    ValueFamily.State.Empty,
            )

        family.observe(
            ValueFamily.State.Ready(
                value = "provider-current",
            ),
        )

        assertEquals(
            ValueFamily.State.Ready(
                value = "provider-current",
            ),
            family.currentState,
        )
        assertEquals(
            family.currentState,
            family.states.value,
        )
    }

    @Test
    fun displayProjectionIsDerivedFromTheExactSemanticStateSnapshot() {
        val family =
            ValueFamily(
                initialState =
                    ValueFamily.State.Ready(
                        value = "visible",
                    ),
            )

        assertEquals(
            ValueFamily.State.Ready(
                value = "visible",
            ),
            family.currentProjection.state,
        )
        assertEquals(
            ValueFamily.DisplayState(
                value = "visible",
            ),
            family.currentProjection.displayState,
        )
    }

    @Test
    fun concurrentActivationsResolveAgainstOneSerializedCurrentState() {
        val family =
            CounterFamily()
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
                                CounterFamily.Control,
                            ) !=
                                null
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
                    count = count,
                ),
                family.currentState,
            )
        } finally {
            executor.shutdownNow()
        }
    }
}

// [VXG RealForever]
