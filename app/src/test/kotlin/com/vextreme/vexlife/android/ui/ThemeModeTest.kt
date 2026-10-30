package com.vextreme.vexlife.android.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun selectUsesTheAcceptedFamilyCircuitWithoutBinaryInversion() {
            val family =
                ThemeModeFamily(
                    initialState =
                        ThemeMode.Light,
                )

            assertTrue(
                family.activate(
                    ThemeModeFamily.Control.Select(
                        ThemeMode.Dark,
                ) !=
                    null,
            ),
            )
            assertEquals(
                ThemeMode.Dark,
                family.currentState,
            )

            // Selecting the already-current peer value stays there; it never means "invert".
            assertTrue(
                family.activate(
                    ThemeModeFamily.Control.Select(
                        ThemeMode.Dark,
                ) !=
                    null,
            ),
            )
            assertEquals(
                ThemeMode.Dark,
                family.currentState,
            )

            assertTrue(
                family.activate(
                    ThemeModeFamily.Control.Select(
                        ThemeMode.Light,
                ) !=
                    null,
            ),
            )
            assertEquals(
                ThemeMode.Light,
                family.currentState,
            )
            assertEquals(
                family.currentState,
                family.states.value,
            )
    }

    @Test
    fun explicitSelectionResolvesNotSetWithoutInventingADefault() {
            val family =
                ThemeModeFamily(
                    initialState =
                        ThemeMode.NotSet,
                )

            assertTrue(
                family.activate(
                    ThemeModeFamily.Control.Select(
                        ThemeMode.Light,
                ) !=
                    null,
            ),
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
