package com.vextreme.vexlife.android.behavior

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused implementation proof of the Direct Family semantic core.
 *
 * These tests prove behavior of the current implementation; they do not create architecture
 * authority.
 */
class VexFamilyHighConfidenceTest {
    private class ValueFamily(
        initialState:
            State,
    ) :
        VexFamily<
            ValueFamily,
            ValueFamily.State,
            ValueFamily.Control,
            ValueFamily.Effect
        >(
            initialState =
                initialState,
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

            data object Unavailable :
                Control
        }

        sealed interface Effect :
            VexFamily.Effect<ValueFamily> {
            data class Persist(
                val value:
                    String,
            ) :
                Effect
        }

        override fun act(
            currentState:
                State,
            control:
                Control,
        ):
            VexFamily.Act<
                State,
                Effect
            >? =
            when (currentState) {
                State.Empty ->
                    when (control) {
                        is Control.Set ->
                            admit(
                                State.Ready(
                                    value =
                                        control.value,
                                ),
                                effect =
                                    Effect.Persist(
                                        value =
                                            control.value,
                                    ),
                            )

                        Control.Unavailable ->
                            null
                    }

                is State.Ready ->
                    when (control) {
                        is Control.Set ->
                            if (
                                currentState.value ==
                                control.value
                            ) {
                                stay(
                                    effect =
                                        Effect.Persist(
                                            value =
                                                control.value,
                                        ),
                                )
                            } else {
                                admit(
                                    currentState.copy(
                                        value =
                                            control.value,
                                    ),
                                    effect =
                                        Effect.Persist(
                                            value =
                                                control.value,
                                        ),
                                )
                            }

                        Control.Unavailable ->
                            null
                    }
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
                    count =
                        0,
                ),
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

        sealed interface Effect :
            VexFamily.Effect<CounterFamily>

        override fun act(
            currentState:
                State,
            control:
                Control,
        ):
            VexFamily.Act<
                State,
                Effect
            > =
            when (control) {
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

    @Test
    fun activateAppliesImmediateStateAndReturnsEffect() {
        val family =
            ValueFamily(
                initialState =
                    ValueFamily.State.Empty,
            )

        val act =
            family.activate(
                ValueFamily.Control.Set(
                    value =
                        "one",
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
        assertEquals(
            ValueFamily.Effect.Persist(
                value =
                    "one",
            ),
            act?.effect,
        )
    }

    @Test
    fun stayMayReturnEffectWithoutChangingCurrentState() {
        val initial =
            ValueFamily.State.Ready(
                value =
                    "same",
            )
        val family =
            ValueFamily(
                initialState =
                    initial,
            )

        val act =
            family.activate(
                ValueFamily.Control.Set(
                    value =
                        "same",
                ),
            )

        assertEquals(
            initial,
            family.currentState,
        )
        assertEquals(
            ValueFamily.Effect.Persist(
                value =
                    "same",
            ),
            act?.effect,
        )
    }

    @Test
    fun unavailableControlReturnsNullAndDoesNotChangeState() {
        val family =
            ValueFamily(
                initialState =
                    ValueFamily.State.Empty,
            )

        assertNull(
            family.activate(
                ValueFamily.Control.Unavailable,
            ),
        )
        assertEquals(
            ValueFamily.State.Empty,
            family.currentState,
        )
    }

    @Test
    fun availabilityUsesTheSamePureRelationshipWithoutApplyingIt() {
        val family =
            ValueFamily(
                initialState =
                    ValueFamily.State.Empty,
            )
        val control =
            ValueFamily.Control.Set(
                value =
                    "candidate",
            )

        assertTrue(
            family.isAvailable(
                control,
            ),
        )
        assertEquals(
            ValueFamily.State.Empty,
            family.currentState,
        )

        val act =
            family.activate(
                control,
            )
        assertEquals(
            ValueFamily.State.Ready(
                value =
                    "candidate",
            ),
            family.currentState,
        )
        assertEquals(
            ValueFamily.Effect.Persist(
                value =
                    "candidate",
            ),
            act?.effect,
        )
    }

    @Test
    fun observedSemanticRealityUsesTheSameStateRelay() {
        val family =
            ValueFamily(
                initialState =
                    ValueFamily.State.Empty,
            )
        val observed =
            ValueFamily.State.Ready(
                value =
                    "provider-current",
            )

        family.observe(
            observed,
        )

        assertEquals(
            observed,
            family.currentState,
        )
        assertEquals(
            family.currentState,
            family.states.value,
        )
    }

    @Test
    fun displayStateDefaultsToCurrentStateOnly() {
        val family =
            ValueFamily(
                initialState =
                    ValueFamily.State.Empty,
            )

        assertEquals(
            VexFamily.DisplayState.StateOnly(
                ValueFamily.State.Empty,
            ),
            family.displayState,
        )
    }

    @Test
    fun concurrentControlsResolveAgainstOneSerializedCurrentState() {
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
                                CounterFamily.Control.Increment,
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
                    count =
                        count,
                ),
                family.currentState,
            )
        } finally {
            executor.shutdownNow()
        }
    }
}

// [VXG RealForever]
