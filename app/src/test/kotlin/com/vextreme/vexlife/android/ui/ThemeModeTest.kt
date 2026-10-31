package com.vextreme.vexlife.android.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun themeModeVocabularyKeepsNotSetAndExplicitPeerValuesDistinct() {
        assertEquals(
            setOf(
                ThemeMode.NotSet,
                ThemeMode.Light,
                ThemeMode.Dark,
            ),
            ThemeMode.entries.toSet(),
        )

        assertNotEquals(
            ThemeMode.NotSet,
            ThemeMode.Light,
        )
        assertNotEquals(
            ThemeMode.NotSet,
            ThemeMode.Dark,
        )
    }

    @Test
    fun displayStateOwnsTheExpectedVexThemePresentation() {
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeMode.Light,
            )

        assertEquals(
            ThemeModeFamily.DisplayState.Light,
            family.displayState,
        )
        assertEquals(
            VexThemePresentation.Light,
            family.displayState.presentation,
        )

        family.activate(
            ThemeModeFamily.Control.Select(
                ThemeMode.Dark,
            ),
        )

        assertEquals(
            ThemeModeFamily.DisplayState.Dark,
            family.displayState,
        )
        assertEquals(
            VexThemePresentation.Dark,
            family.displayState.presentation,
        )
    }

    @Test
    fun selectAdmitsSemanticStateAndReturnsSaveEffectWithoutPerformingPersistence() {
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeMode.Light,
            )

        val darkAct =
            family.activate(
                ThemeModeFamily.Control.Select(
                    ThemeMode.Dark,
                ),
            )

        assertNotNull(
            darkAct,
        )
        assertEquals(
            ThemeMode.Dark,
            family.currentState,
        )
        assertEquals(
            ThemeModeFamily.Effect.Save(
                ThemeMode.Dark,
            ),
            darkAct?.effect,
        )

        val sameAct =
            family.activate(
                ThemeModeFamily.Control.Select(
                    ThemeMode.Dark,
                ),
            )

        assertEquals(
            ThemeMode.Dark,
            family.currentState,
        )
        assertEquals(
            ThemeModeFamily.Effect.Save(
                ThemeMode.Dark,
            ),
            sameAct?.effect,
        )
    }

    @Test
    fun explicitSelectionResolvesNotSetWithoutInventingADefault() {
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeMode.NotSet,
            )

        val act =
            family.activate(
                ThemeModeFamily.Control.Select(
                    ThemeMode.Light,
                ),
            )

        assertNotNull(
            act,
        )
        assertEquals(
            ThemeMode.Light,
            family.currentState,
        )

        val invalidSelection =
            runCatching {
                ThemeModeFamily.Control.Select(
                    ThemeMode.NotSet,
                )
            }.exceptionOrNull()

        assertTrue(
            invalidSelection is
                IllegalArgumentException,
        )
    }
}

// [VXG RealForever]
