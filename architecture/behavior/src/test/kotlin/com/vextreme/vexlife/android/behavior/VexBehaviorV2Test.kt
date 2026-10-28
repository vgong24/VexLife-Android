package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.relay.VexState
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexBehaviorV2Test {
    private class ThemeModeFamily(
        initialState: State,
        executionRoot: VexExecutionRoot,
        override val store: Store? = null,
        logger: VexFamilyLogger = VexFamilyLogger.None,
    ) :
        VexBehaviorV2Family<
            ThemeModeFamily,
            ThemeModeFamily.State,
            ThemeModeFamily.Control
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
                ThemeModeFamily
            >,
            VexState<
                State
            > {
            data object NotSet :
                State

            data object Light :
                State

            data object Dark :
                State

            override fun deepCopy():
                State =
                this
        }

        sealed interface Control :
            VexBehaviorFamily.Control<
                ThemeModeFamily
            > {
            data object Toggle :
                Control
        }

        interface Store :
            VexFamilyStore<
                ThemeModeFamily,
                State
            >

        sealed interface DisplayState :
            VexBehaviorV2DisplayState<
                State
            > {
            data class Empty(
                override val state:
                    State,
            ) :
                DisplayState

            data class Active(
                override val state:
                    State,
            ) :
                DisplayState

            data class Inactive(
                override val state:
                    State,
            ) :
                DisplayState
        }

        override val acts =
            vexActs<
                ThemeModeFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "example.theme-mode.v2",
            ) {
                state<State.NotSet> {
                    on(
                        Control.Toggle,
                    ) {
                        stay()

                        begin(
                            mode =
                                VexBehaviorV2BeginMode.Sequenced,
                        ) {
                            save(
                                State.Dark,
                            ).results {
                                success {
                                    admit(
                                        State.Dark,
                                    )
                                }

                                failure {
                                }
                            }
                        }
                    }
                }

                state<State.Light> {
                    on(
                        Control.Toggle,
                    ) {
                        admit(
                            State.Dark,
                        )
                    }
                }

                state<State.Dark> {
                    on(
                        Control.Toggle,
                    ) {
                        admit(
                            State.Light,
                        )
                    }
                }
            }

        override val displayState:
            DisplayState
            get() =
                when (
                    val state =
                        current
                ) {
                    State.NotSet ->
                        DisplayState.Empty(
                            state,
                        )

                    State.Light ->
                        DisplayState.Inactive(
                            state,
                        )

                    State.Dark ->
                        DisplayState.Active(
                            state,
                        )
                }
    }

    private class MinimalFamily(
        initialState: State,
        executionRoot: VexExecutionRoot,
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
                    "example.minimal.v2",
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
        ThemeModeFamily.Store {
        private val mutableState =
            MutableStateFlow<ThemeModeFamily.State>(
                ThemeModeFamily.State.NotSet,
            )

        override val observableState =
            mutableState.asStateFlow()

        val gate =
            CompletableDeferred<Unit>()

        override suspend fun get():
            ThemeModeFamily.State =
            mutableState.value

        override suspend fun save(
            state:
                ThemeModeFamily.State,
        ):
            VexOperationResult {
            gate.await()
            mutableState.value =
                state
            return VexOperationResult.Success
        }

        override suspend fun forget():
            VexOperationResult {
            mutableState.value =
                ThemeModeFamily.State.NotSet
            return VexOperationResult.Success
        }
    }

    private class RecordingLogger(
        private val onRecord:
            (
                VexFamilyLogEvent,
            ) -> Unit = {},
    ) :
        VexFamilyLogger {
        val events =
            Collections.synchronizedList(
                mutableListOf<
                    VexFamilyLogEvent
                >(),
            )

        override fun record(
            event: VexFamilyLogEvent,
        ) {
            events +=
                event
            onRecord(
                event,
            )
        }
    }

    @Test
    fun activateUsesFamilyCurrentStateAndAppliesTheAuthoredAct() {
        val root =
            VexExecutionRoot()

        try {
            val family =
                ThemeModeFamily(
                    initialState =
                        ThemeModeFamily.State.Light,
                    executionRoot =
                        root,
                )

            val act =
                family.activate(
                    ThemeModeFamily.Control.Toggle,
                )

            assertSame(
                ThemeModeFamily.Control.Toggle,
                act?.control,
            )
            assertNull(
                act?.effect,
            )
            assertSame(
                ThemeModeFamily.State.Dark,
                family.current,
            )
            assertEquals(
                ThemeModeFamily.DisplayState.Active(
                    ThemeModeFamily.State.Dark,
                ),
                family.displayState,
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun beginExecutesSaveAndSuccessCanAdmitLaterState() {
        val root =
            VexExecutionRoot()
        val store =
            GatedStore()
        val success =
            CountDownLatch(
                1,
            )
        val logger =
            RecordingLogger { event ->
                if (
                    event is
                        VexFamilyLogEvent.OperationSuccess
                ) {
                    success.countDown()
                }
            }

        try {
            val family =
                ThemeModeFamily(
                    initialState =
                        ThemeModeFamily.State.NotSet,
                    executionRoot =
                        root,
                    store =
                        store,
                    logger =
                        logger,
                )

            val act =
                family.activate(
                    ThemeModeFamily.Control.Toggle,
                )

            assertSame(
                ThemeModeFamily.State.NotSet,
                family.current,
            )
            assertTrue(
                act?.effect
                    ?.operation is
                    VexBehaviorV2Save,
            )

            store.gate.complete(
                Unit,
            )

            assertTrue(
                "Save Success was not witnessed",
                success.await(
                    5,
                    TimeUnit.SECONDS,
                ),
            )

            assertSame(
                ThemeModeFamily.State.Dark,
                runBlocking {
                    store.get()
                },
            )
            assertSame(
                ThemeModeFamily.State.Dark,
                family.current,
            )
            assertTrue(
                logger.events.any { event ->
                    event is
                        VexFamilyLogEvent.OperationBegan
                },
            )
            assertTrue(
                logger.events.any { event ->
                    event is
                        VexFamilyLogEvent.OperationSuccess
                },
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun displayStateIsAlwaysPresentAndDefaultsToEmptyCurrentState() {
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
    fun themeModeStateCanAlsoBeRelayVexStateWithoutInventingAnotherStateNoun() {
        val root =
            VexExecutionRoot()

        try {
            val copied:
                ThemeModeFamily.State =
                ThemeModeFamily.State.Dark
                    .deepCopy()

            assertSame(
                ThemeModeFamily.State.Dark,
                copied,
            )

            val family =
                ThemeModeFamily(
                    initialState =
                        ThemeModeFamily.State.Light,
                    executionRoot =
                        root,
                )

            assertTrue(
                family.acts
                    .describe()
                    .contains(
                        "Light --on(Toggle)",
                    ),
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun saveResultsStillRequireExplicitSuccessAndFailure() {
        val root =
            VexExecutionRoot()

        try {
            val family =
                ThemeModeFamily(
                    initialState =
                        ThemeModeFamily.State.Light,
                    executionRoot =
                        root,
                )

            vexActs<
                ThemeModeFamily,
                ThemeModeFamily.State,
                ThemeModeFamily.Control
            >(
                family =
                    family,
                behaviorRef =
                    "example.theme-mode.v2.incomplete-results",
            ) {
                state<ThemeModeFamily.State.Light> {
                    on(
                        ThemeModeFamily.Control.Toggle,
                    ) {
                        admit(
                            ThemeModeFamily.State.Dark,
                        )

                        begin(
                            mode =
                                VexBehaviorV2BeginMode.Replaceable,
                        ) {
                            save(
                                ThemeModeFamily.State.Dark,
                            ).results {
                                success {
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            root.forceStopAll()
        }
    }
}

// [VXG RealForever]
