package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
            val connection =
                ThemeModeConnection(
                    store =
                        store,
                    executionRoot =
                        root,
                    initialState =
                        ThemeMode.Dark,
                )

            assertTrue(
                connection.activate(
                    ThemeModeFamily.Control.Select(
                        ThemeMode.Dark,
                    ),
                ),
            )
            assertTrue(
                store.saved.await(
                    5,
                    TimeUnit.SECONDS,
                ),
            )

            assertEquals(
                listOf(
                    ThemeMode.Dark,
                ),
                store.savedStates,
            )
            assertEquals(
                ThemeMode.Dark,
                store.current(),
            )
            assertEquals(
                ThemeMode.Dark,
                connection.family.currentState,
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun durableSelectionSeedsFreshConnectionFamily() {
        val root =
            VexExecutionRoot()
        val store =
            RecordingThemeModeStore(
                ThemeMode.Light,
            )

        try {
            val connection =
                ThemeModeConnection(
                    store =
                        store,
                    executionRoot =
                        root,
                )

            assertEquals(
                ThemeMode.Light,
                connection.family.currentState,
            )
            assertEquals(
                connection.family.currentState,
                connection.family.states.value,
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
        @Volatile
        private var currentState =
            initialState

        val savedStates =
            mutableListOf<ThemeMode>()

        val saved =
            CountDownLatch(
                1,
            )

        override fun current():
            ThemeMode =
            currentState

        override suspend fun save(
            state:
                ThemeMode,
        ) {
            savedStates +=
                state
            currentState =
                state
            saved.countDown()
        }

        override fun forget() {
            currentState =
                ThemeMode.NotSet
        }
    }
}

// [VXG RealForever]
