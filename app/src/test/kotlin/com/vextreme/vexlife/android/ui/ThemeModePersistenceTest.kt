package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModePersistenceTest {
    @Test
    fun saveMethodOwnsTheFamilyModeParameterAndConcreteStoreCall() {
        val store =
            RecordingThemeModeStore(
                ThemeMode.NotSet,
            )
        val parameter =
            ThemeModeFamily.Parameter.Mode(
                mode =
                    ThemeMode.Dark,
            )
        val method =
            ThemeModeFamily.Act.Method.Save(
                store =
                    store,
                parameter =
                    parameter,
            )

        val result =
            runBlocking {
                method()
            }

        assertEquals(
            ThemeModeStore.Result.Success(
                observedState =
                    ThemeMode.Dark,
            ),
            result,
        )
        assertEquals(
            listOf(
                ThemeMode.Dark,
            ),
            store.savedStates,
        )
        assertTrue(
            method.parameter ===
                parameter,
        )
    }

    @Test
    fun explicitSelectionPersistsEvenWhenTheSelectedModeIsAlreadyCurrent() {
        val root =
            VexExecutionRoot()
        val store =
            RecordingThemeModeStore(
                ThemeMode.NotSet,
            )
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeMode.Dark,
            )
        val logger =
            VexLogger()
        observeThemeModeStore(
            family =
                family,
            store =
                store,
            executionRoot =
                root,
            logger =
                logger,
        )

        try {
            assertTrue(
                activateThemeMode(
                    family =
                        family,
                    control =
                        ThemeModeFamily.Control.Select(
                    ThemeModeFamily.Parameter.Mode(
                        mode =
                            ThemeMode.Dark,
                    ),
                ),
                    store =
                        store,
                    executionRoot =
                        root,
                    logger =
                        logger,
                ),
            )

            runBlocking {
                withTimeout(
                    5_000,
                ) {
                    store.states.first { state ->
                        state ==
                            ThemeMode.Dark
                    }
                }
            }

            assertEquals(
                listOf(
                    ThemeMode.Dark,
                ),
                store.savedStates,
            )
            assertEquals(
                ThemeMode.Dark,
                family.currentState,
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun durableSelectionObservationRestoresCurrentThroughTheFamily() {
        val root =
            VexExecutionRoot()
        val store =
            RecordingThemeModeStore(
                ThemeMode.Light,
            )
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeMode.Dark,
            )

        try {
            observeThemeModeStore(
                family =
                    family,
                store =
                    store,
                executionRoot =
                    root,
                logger =
                    VexLogger(),
            )

            runBlocking {
                withTimeout(
                    5_000,
                ) {
                    family.states.first { state ->
                        state ==
                            ThemeMode.Light
                    }
                }
            }

            assertEquals(
                ThemeMode.Light,
                family.currentState,
            )
            assertTrue(
                store.savedStates.isEmpty(),
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun externalDurableObservationDoesNotFeedBackIntoAnotherSave() {
        val root =
            VexExecutionRoot()
        val store =
            RecordingThemeModeStore(
                ThemeMode.NotSet,
            )
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeMode.Dark,
            )

        try {
            observeThemeModeStore(
                family =
                    family,
                store =
                    store,
                executionRoot =
                    root,
                logger =
                    VexLogger(),
            )

            store.emit(
                ThemeMode.Light,
            )

            runBlocking {
                withTimeout(
                    5_000,
                ) {
                    family.states.first { state ->
                        state ==
                            ThemeMode.Light
                    }
                }
            }

            assertEquals(
                ThemeMode.Light,
                family.currentState,
            )
            assertTrue(
                store.savedStates.isEmpty(),
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun immediateStateAdmissionPrecedesSuccessfulSaveWithoutDuplicateResultAdmission() {
        val root =
            VexExecutionRoot()
        val store =
            ResultOnlyThemeModeStore()
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeMode.Light,
            )
        val logger =
            VexLogger()
        observeThemeModeStore(
            family =
                family,
            store =
                store,
            executionRoot =
                root,
            logger =
                logger,
        )

        try {
            assertTrue(
                activateThemeMode(
                    family =
                        family,
                    control =
                        ThemeModeFamily.Control.Select(
                            ThemeModeFamily.Parameter.Mode(
                                mode =
                                    ThemeMode.Dark,
                            ),
                        ),
                    store =
                        store,
                    executionRoot =
                        root,
                    logger =
                        logger,
                ),
            )

            assertEquals(
                ThemeMode.Dark,
                family.currentState,
            )

            runBlocking {
                withTimeout(
                    5_000,
                ) {
                    while (
                        logger.history.value
                            .none { record ->
                                record.action ==
                                    "EffectSucceeded"
                            }
                    ) {
                        yield()
                    }
                }
            }

            val actions =
                logger.history.value
                    .map {
                        it.action
                    }

            val stateAdmitted =
                actions.indexOf(
                    "StateAdmitted",
                )
            val effectRequested =
                actions.indexOf(
                    "EffectRequested",
                )
            val effectSucceeded =
                actions.indexOf(
                    "EffectSucceeded",
                )

            assertTrue(
                stateAdmitted >=
                    0,
            )
            assertTrue(
                stateAdmitted <
                    effectRequested,
            )
            assertTrue(
                effectRequested <
                    effectSucceeded,
            )
            assertEquals(
                1,
                actions.count {
                    it ==
                        "StateAdmitted"
                },
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun delayedBootstrapStoreSnapshotCannotOverwriteNewerAcceptedSelection() {
        val root =
            VexExecutionRoot()
        val store =
            DelayedBootstrapThemeModeStore(
                initialState =
                    ThemeMode.Dark,
            )
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeMode.Dark,
            )
        val logger =
            VexLogger()

        observeThemeModeStore(
            family =
                family,
            store =
                store,
            executionRoot =
                root,
            logger =
                logger,
        )

        try {
            assertTrue(
                activateThemeMode(
                    family =
                        family,
                    control =
                        ThemeModeFamily.Control.Select(
                            ThemeModeFamily.Parameter.Mode(
                                mode =
                                    ThemeMode.Light,
                            ),
                        ),
                    store =
                        store,
                    executionRoot =
                        root,
                    logger =
                        logger,
                ),
            )
            assertEquals(
                ThemeMode.Light,
                family.currentState,
            )

            store.releaseBootstrapObservation()

            runBlocking {
                withTimeout(
                    5_000,
                ) {
                    store.awaitBootstrapObservationDelivered()
                }
            }

            /*
             * The Store snapshot was Dark when observation was formed, but the newer accepted
             * Select(Light) happened before that bootstrap snapshot reached the collector.
             */
            assertEquals(
                ThemeMode.Light,
                family.currentState,
            )

            store.releaseSave()
        } finally {
            root.forceStopAll()
        }
    }

    private class DelayedBootstrapThemeModeStore(
        private val initialState:
            ThemeMode,
    ) :
        ThemeModeStore {
        private val bootstrapGate =
            CompletableDeferred<Unit>()
        private val bootstrapDelivered =
            CompletableDeferred<Unit>()
        private val saveGate =
            CompletableDeferred<Unit>()

        override val states:
            Flow<ThemeMode> =
            flow {
                bootstrapGate.await()
                emit(
                    initialState,
                )
                bootstrapDelivered.complete(
                    Unit,
                )
                awaitCancellation()
            }

        override suspend fun get():
            ThemeMode =
            initialState

        override suspend fun save(
            state:
                ThemeMode,
        ):
            ThemeModeStore.Result {
            saveGate.await()
            return ThemeModeStore.Result.Success(
                observedState =
                    state,
            )
        }

        override suspend fun forget():
            ThemeModeStore.Result =
            ThemeModeStore.Result.Success(
                observedState =
                    ThemeMode.NotSet,
            )

        fun releaseBootstrapObservation() {
            bootstrapGate.complete(
                Unit,
            )
        }

        suspend fun awaitBootstrapObservationDelivered() {
            bootstrapDelivered.await()
        }

        fun releaseSave() {
            saveGate.complete(
                Unit,
            )
        }
    }

    private class ResultOnlyThemeModeStore :
        ThemeModeStore {
        private val mutableState =
            MutableStateFlow(
                ThemeMode.NotSet,
            )

        override val states:
            Flow<ThemeMode> =
            mutableState

        override suspend fun get():
            ThemeMode =
            mutableState.value

        override suspend fun save(
            state:
                ThemeMode,
        ):
            ThemeModeStore.Result =
            ThemeModeStore.Result.Success(
                observedState =
                    state,
            )

        override suspend fun forget():
            ThemeModeStore.Result =
            ThemeModeStore.Result.Success(
                observedState =
                    ThemeMode.NotSet,
            )
    }

    @Test
    fun selectPayloadDoesNotCreateAnotherExecutionRelationship() {
        val root =
            VexExecutionRoot()
        val store =
            LatestThemeModeStore()
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeMode.Light,
            )
        val logger =
            VexLogger()
        observeThemeModeStore(
            family =
                family,
            store =
                store,
            executionRoot =
                root,
            logger =
                logger,
        )

        try {
            assertTrue(
                activateThemeMode(
                    family =
                        family,
                    control =
                        ThemeModeFamily.Control.Select(
                    ThemeModeFamily.Parameter.Mode(
                        mode =
                            ThemeMode.Dark,
                    ),
                ),
                    store =
                        store,
                    executionRoot =
                        root,
                    logger =
                        logger,
                ),
            )

            runBlocking {
                withTimeout(
                    5_000,
                ) {
                    store.firstSaveStarted.await()
                }
            }

            assertTrue(
                activateThemeMode(
                    family =
                        family,
                    control =
                        ThemeModeFamily.Control.Select(
                    ThemeModeFamily.Parameter.Mode(
                        mode =
                            ThemeMode.Light,
                    ),
                ),
                    store =
                        store,
                    executionRoot =
                        root,
                    logger =
                        logger,
                ),
            )

            runBlocking {
                withTimeout(
                    5_000,
                ) {
                    store.firstSaveCancelled.await()
                    store.latestSaveCompleted.await()
                }
            }

            assertEquals(
                listOf(
                    ThemeMode.Light,
                ),
                store.completedSaves,
            )
        } finally {
            root.forceStopAll()
        }
    }

    private class LatestThemeModeStore :
        ThemeModeStore {
        private val mutableState =
            MutableStateFlow(
                ThemeMode.Light,
            )

        val firstSaveStarted =
            CompletableDeferred<Unit>()
        val firstSaveCancelled =
            CompletableDeferred<Unit>()
        val latestSaveCompleted =
            CompletableDeferred<Unit>()
        val completedSaves =
            mutableListOf<ThemeMode>()

        override val states:
            Flow<ThemeMode> =
            mutableState

        override suspend fun get():
            ThemeMode =
            mutableState.value

        override suspend fun save(
            state:
                ThemeMode,
        ):
            ThemeModeStore.Result {
            if (
                state ==
                ThemeMode.Dark
            ) {
                firstSaveStarted.complete(
                    Unit,
                )
                try {
                    awaitCancellation()
                } finally {
                    firstSaveCancelled.complete(
                        Unit,
                    )
                }
            }

            completedSaves +=
                state
            mutableState.value =
                state
            latestSaveCompleted.complete(
                Unit,
            )
            return ThemeModeStore.Result.Success(
                observedState =
                    state,
            )
        }

        override suspend fun forget():
            ThemeModeStore.Result =
            ThemeModeStore.Result.Success(
                observedState =
                    ThemeMode.NotSet,
            )
    }

    private class RecordingThemeModeStore(
        initialState:
            ThemeMode,
    ) :
        ThemeModeStore {
        private val mutableState =
            MutableStateFlow(
                initialState,
            )

        val savedStates =
            mutableListOf<ThemeMode>()

        override val states:
            Flow<ThemeMode> =
            mutableState

        override suspend fun get():
            ThemeMode =
            mutableState.value

        override suspend fun save(
            state:
                ThemeMode,
        ):
            ThemeModeStore.Result {
            savedStates +=
                state
            mutableState.value =
                state
            return ThemeModeStore.Result.Success(
                observedState =
                    state,
            )
        }

        override suspend fun forget():
            ThemeModeStore.Result {
            mutableState.value =
                ThemeMode.NotSet
            return ThemeModeStore.Result.Success(
                observedState =
                    ThemeMode.NotSet,
            )
        }

        fun emit(
            state:
                ThemeMode,
        ) {
            mutableState.value =
                state
        }
    }
}

// [VXG RealForever]
