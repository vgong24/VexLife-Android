package com.vextreme.vexlife.android.ui

import androidx.test.platform.app.InstrumentationRegistry
import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ThemeModeStoreInstrumentedTest {
    private val context
        get() =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext

    @Before
    fun clearBefore() =
        clearStore()

    @After
    fun clearAfter() =
        clearStore()

    @Test
    fun sharedPreferencesSelectionSurvivesFreshStoreAndFamilyConstruction() {
        val firstStore =
            SharedPreferencesThemeModeStore(
                context,
            )

        runBlocking {
            assertEquals(
                VexFamily.Store.WriteResult.Success,
                firstStore.save(
                    ThemeMode.Light,
                ),
            )
            assertEquals(
                ThemeMode.Light,
                firstStore.get(),
            )
        }

        val root =
            VexExecutionRoot()

        try {
            val recreatedStore =
                SharedPreferencesThemeModeStore(
                    context,
                )
            val recreatedFamily =
                ThemeModeFamily(
                    initialState =
                        ThemeMode.Dark,
                    executionRoot =
                        root,
                    store =
                        recreatedStore,
                )

            runBlocking {
                assertEquals(
                    ThemeMode.Light,
                    recreatedStore.get(),
                )
                withTimeout(
                    5_000,
                ) {
                    recreatedFamily.states.first { state ->
                        state ==
                            ThemeMode.Light
                    }
                }
            }

            assertEquals(
                ThemeMode.Light,
                recreatedFamily.currentState,
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun notSetIsRepresentedByForgetRatherThanPersistedAsAnExplicitSelection() {
        val store =
            SharedPreferencesThemeModeStore(
                context,
            )

        runBlocking {
            val invalidSave =
                store.save(
                    ThemeMode.NotSet,
                )

            assertTrue(
                invalidSave is
                    VexFamily.Store.WriteResult.Failure,
            )

            assertEquals(
                VexFamily.Store.WriteResult.Success,
                store.save(
                    ThemeMode.Dark,
                ),
            )
            assertEquals(
                ThemeMode.Dark,
                store.get(),
            )

            assertEquals(
                VexFamily.Store.WriteResult.Success,
                store.forget(),
            )
            assertEquals(
                ThemeMode.NotSet,
                store.get(),
            )
        }
    }

    private fun clearStore() {
        runBlocking {
            SharedPreferencesThemeModeStore(
                context,
            ).forget()
        }
    }
}

// [VXG RealForever]
