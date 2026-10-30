package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.behavior.VexFamily
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

        try {
            val family =
                ThemeModeFamily(
                    initialState =
                        ThemeMode.Dark,
                    executionRoot =
                        root,
                    store =
                        store,
                )

            assertTrue(
                family.activate(
                    ThemeModeFamily.Control.Select(
                        ThemeMode.Dark,
                    ),
                ),
            )

            runBlocking {
                withTimeout(
                    5_000,
                ) {
                    store.observableState.first { state ->
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
    fun durableSelectionObservationRestoresCurrentThroughTheFamilyStateRelay() {
        val root =
            VexExecutionRoot()
        val store =
            RecordingThemeModeStore(
                ThemeMode.Light,
            )

        try {
            val family =
                ThemeModeFamily(
                    initialState =
                        ThemeMode.Dark,
                    executionRoot =
                        root,
                    store =
                        store,
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
            assertEquals(
                family.currentState,
                family.states.value,
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

        try {
            val family =
                ThemeModeFamily(
                    initialState =
                        ThemeMode.Dark,
                    executionRoot =
                        root,
                    store =
                        store,
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
        ThemeModeFamily.Store {
        private val mutableState =
            MutableStateFlow(
                initialState,
            )

        val savedStates =
            mutableListOf<ThemeMode>()

        override val observableState:
            Flow<ThemeMode> =
            mutableState

        suspend fun get():
            ThemeMode =
            mutableState.value

        override suspend fun save(
            state:
                ThemeMode,
        ):
            VexFamily.Store.WriteResult {
            savedStates +=
                state
            mutableState.value =
                state
            return VexFamily.Store.WriteResult.Success
        }

        suspend fun forget():
            VexFamily.Store.WriteResult {
            mutableState.value =
                ThemeMode.NotSet
            return VexFamily.Store.WriteResult.Success
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
