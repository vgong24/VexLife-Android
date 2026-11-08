package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Focused proof of the refined pure Family semantic surface.
 *
 * Runtime execution, persistence and logging are intentionally proved by their rightful owners.
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
            private val value: String,
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
                            value,
                    )
                }
        }

        class ThrowingSaveMethod :
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
                        effect =
                            Effect.Save(
                                value = control.value,
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

        val effect =
            family.activate(
                ValueFamily.Control.Save(
                    value = "one",
                ),
            )

        assertEquals(
            ValueFamily.Effect.Save(
                value = "one",
            ),
            effect?.effect,
        )
        assertEquals(
            VexExecution.Approach.Independent,
            effect?.approach,
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
                    value = "saved",
                    shouldFail = false,
                )()

            val failure =
                ValueFamily.SaveMethod(
                    value = "ignored",
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
                val job =
                    checkNotNull(
                        act.launchWith(
                            root,
                        ),
                    ) {
                        "Save Effect requires an executable Act consequence"
                    }
                        .method(
                            method =
                                ValueFamily.ThrowingSaveMethod(),
                            onResult = { result ->
                                received =
                                    result
                            },
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
