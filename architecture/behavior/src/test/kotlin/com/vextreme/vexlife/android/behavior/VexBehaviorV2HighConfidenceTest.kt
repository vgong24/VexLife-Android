package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused proof of the high-confidence V2 Family surface.
 *
 * This suite intentionally does not model Compound Family / stateFactory / post-commit Act pressure.
 */
class VexBehaviorV2HighConfidenceTest {
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
        VexBehaviorV2Family<
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
            VexBehaviorFamily.State<
                ValueFamily
            > {
            data object Empty :
                State

            data class Ready(
                val value:
                    String,
            ) :
                State
        }

        sealed interface Control :
            VexBehaviorFamily.Control<
                ValueFamily
            > {
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
            VexFamilyStateStore<
                ValueFamily,
                State
            >

        data class DisplayState(
            val value:
                String?,
        ) :
            VexBehaviorV2DisplayState<
                State
            >

        override val acts =
            vexActs<
                ValueFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "test.family.high-confidence.value",
            ) {
                state<State.Empty> {
                    on<Control.Set> { _, control ->
                        admit(
                            State.Ready(
                                value =
                                    control.value,
                            ),
                        )
                    }

                    on<Control.Commit> { _, control ->
                        stay()

                        begin(
                            mode =
                                VexBehaviorV2BeginMode.Sequenced,
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
                        }
                    }
                }

                state<State.Ready> {
                    on<Control.Set>(
                        whenCurrent = { current, control ->
                            current.value !=
                                control.value
                        },
                    ) { _, control ->
                        admit(
                            State.Ready(
                                value =
                                    control.value,
                            ),
                        )
                    }
                }
            }

        override val displayState:
            DisplayState
            get() =
                DisplayState(
                    value =
                        when (
                            val state =
                                current
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
        VexBehaviorV2Family<
            MinimalFamily,
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
        sealed interface State :
            VexBehaviorFamily.State<
                MinimalFamily
            > {
            data object Idle :
                State
        }

        sealed interface Control :
            VexBehaviorFamily.Control<
                MinimalFamily
            > {
            data object Keep :
                Control
        }

        override val acts =
            vexActs<
                MinimalFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "test.family.high-confidence.minimal",
            ) {
                state<State.Idle> {
                    on(
                        Control.Keep,
                    ) {
                        stay()
                    }
                }
            }
    }

    private class GatedStore :
        ValueFamily.Store {
        private val mutableState =
            MutableStateFlow<
                ValueFamily.State
            >(
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
    fun activateUsesCurrentStateAndPayloadControl() {
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

            val control =
                ValueFamily.Control.Set(
                    value =
                        "one",
                )

            assertTrue(
                family.isAvailable(
                    control,
                ),
            )

            val act =
                family.activate(
                    control,
                )

            assertEquals(
                control,
                act?.control,
            )
            assertNull(
                act?.effect,
            )
            assertEquals(
                ValueFamily.State.Ready(
                    value =
                        "one",
                ),
                family.current,
            )
            assertEquals(
                ValueFamily.DisplayState(
                    value =
                        "one",
                ),
                family.displayState,
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun availabilityAndActivationUseTheSameCurrentStateControlLaw() {
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

            val semanticNoOp =
                ValueFamily.Control.Set(
                    value =
                        "one",
                )
            val change =
                ValueFamily.Control.Set(
                    value =
                        "two",
                )

            assertFalse(
                family.isAvailable(
                    semanticNoOp,
                ),
            )
            assertNull(
                family.activate(
                    semanticNoOp,
                ),
            )
            assertTrue(
                family.isAvailable(
                    change,
                ),
            )

            family.activate(
                change,
            )

            assertEquals(
                ValueFamily.State.Ready(
                    value =
                        "two",
                ),
                family.current,
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
                VexBehaviorV2DisplayState.Empty(
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

            val act =
                family.activate(
                    ValueFamily.Control.Commit(
                        value =
                            "loaded",
                    ),
                )

            assertEquals(
                ValueFamily.State.Empty,
                family.current,
            )
            assertTrue(
                act?.effect
                    ?.operation is
                    VexBehaviorV2Save<
                        *,
                        *
                    >,
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
                family.current,
            )
        } finally {
            root.forceStopAll()
        }
    }
}

// [VXG RealForever]
