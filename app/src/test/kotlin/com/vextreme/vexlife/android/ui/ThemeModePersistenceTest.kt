package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModePersistenceTest {
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
        val runtime =
            ThemeModeRuntime(
                family = family,
                executionRoot = root,
                store = store,
            )

        try {
            assertTrue(
                runtime.activate(
                    ThemeModeFamily.Control.Select(
                        ThemeMode.Dark,
                    ),
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
    fun durableSelectionObservationRestoresCurrentThroughTheRuntime() {
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
            ThemeModeRuntime(
                family = family,
                executionRoot = root,
                store = store,
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
            ThemeModeRuntime(
                family = family,
                executionRoot = root,
                store = store,
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
        val runtime =
            ThemeModeRuntime(
                family =
                    family,
                executionRoot =
                    root,
                store =
                    store,
            )

        try {
            assertTrue(
                runtime.activate(
                    ThemeModeFamily.Control.Select(
                        ThemeMode.Dark,
                    ),
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
                runtime.activate(
                    ThemeModeFamily.Control.Select(
                        ThemeMode.Light,
                    ),
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
