package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.behavior.VexStore
import com.vextreme.vexlife.android.execution.VexExecutionRoot
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
        val binding =
            ThemeModeStoreBinding(
                family = family,
                executionRoot = root,
                store = store,
                approach =
                    VexExecution.Approach.Latest,
                admitObservation = { state ->
                    state != ThemeMode.NotSet
                },
            )

        try {
            assertTrue(
                binding.activate(
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
    fun durableSelectionObservationRestoresCurrentThroughTheBinding() {
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
            ThemeModeStoreBinding(
                family = family,
                executionRoot = root,
                store = store,
                approach =
                    VexExecution.Approach.Latest,
                admitObservation = { state ->
                    state != ThemeMode.NotSet
                },
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
            ThemeModeStoreBinding(
                family = family,
                executionRoot = root,
                store = store,
                approach =
                    VexExecution.Approach.Latest,
                admitObservation = { state ->
                    state != ThemeMode.NotSet
                },
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
            VexStore.Result<ThemeMode> {
            savedStates +=
                state
            mutableState.value =
                state
            return VexStore.Result.Success(
                observedState =
                    state,
            )
        }

        override suspend fun forget():
            VexStore.Result<ThemeMode> {
            mutableState.value =
                ThemeMode.NotSet
            return VexStore.Result.Success(
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
