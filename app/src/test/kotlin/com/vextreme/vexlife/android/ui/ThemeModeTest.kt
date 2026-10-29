package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun resolvedThemeModeProjectsToTheExistingComposeBooleanWithoutOwningNewState() {
        assertFalse(
            ThemeMode.Light.requireResolvedDarkTheme(),
        )
        assertTrue(
            ThemeMode.Dark.requireResolvedDarkTheme(),
        )

        val unresolved =
            runCatching {
                ThemeMode.NotSet.requireResolvedDarkTheme()
            }.exceptionOrNull()

        assertTrue(
            unresolved is IllegalStateException,
        )
    }

    @Test
    fun toggleUsesTheAcceptedFamilyCircuitAgainstCurrentThemeModeState() {
        val root =
            VexExecutionRoot()

        try {
            val family =
                ThemeModeFamily(
                    initialState =
                        ThemeMode.Light,
                    executionRoot =
                        root,
                )

            assertTrue(
                family.isAvailable(
                    ThemeModeFamily.Control.Toggle,
                ),
            )
            assertTrue(
                family.activate(
                    ThemeModeFamily.Control.Toggle,
                ),
            )
            assertEquals(
                ThemeMode.Dark,
                family.currentState,
            )
            assertEquals(
                family.currentState,
                family.states.value,
            )

            assertTrue(
                family.activate(
                    ThemeModeFamily.Control.Toggle,
                ),
            )
            assertEquals(
                ThemeMode.Light,
                family.currentState,
            )
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun notSetDoesNotInventAThemeResolutionPolicy() {
        val root =
            VexExecutionRoot()

        try {
            val family =
                ThemeModeFamily(
                    initialState =
                        ThemeMode.NotSet,
                    executionRoot =
                        root,
                )

            assertFalse(
                family.isAvailable(
                    ThemeModeFamily.Control.Toggle,
                ),
            )
            assertFalse(
                family.activate(
                    ThemeModeFamily.Control.Toggle,
                ),
            )
            assertEquals(
                ThemeMode.NotSet,
                family.currentState,
            )
        } finally {
            root.forceStopAll()
        }
    }
}
