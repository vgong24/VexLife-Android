package com.vextreme.vexlife.android.ui

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.runBlocking
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
            firstStore.save(
                ThemeMode.Light,
            )
        }

        assertEquals(
            ThemeMode.Light,
            firstStore.current(),
        )
        assertEquals(
            "LIGHT",
            context
                .getSharedPreferences(
                    "vexlife-theme-mode",
                    Context.MODE_PRIVATE,
                )
                .getString(
                    "explicit-selection",
                    null,
                ),
        )

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
                        recreatedStore.current(),
                    executionRoot =
                        root,
                    store =
                        recreatedStore,
                )

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

        val invalidSave =
            runCatching {
                runBlocking {
                    store.save(
                        ThemeMode.NotSet,
                    )
                }
            }.exceptionOrNull()

        assertTrue(
            invalidSave is
                IllegalArgumentException,
        )

        runBlocking {
            store.save(
                ThemeMode.Dark,
            )
        }
        assertEquals(
            ThemeMode.Dark,
            store.current(),
        )

        store.forget()
        assertEquals(
            ThemeMode.NotSet,
            store.current(),
        )
    }

    private fun clearStore() {
        SharedPreferencesThemeModeStore(
            context,
        ).forget()
    }
}

// [VXG RealForever]
