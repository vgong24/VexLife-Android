@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.execution.VexExecution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
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
    fun selectCarriesTheSameFamilyModeParameterIntoItsSaveEffect() {
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeMode.Light,
            )
        val mode =
            ThemeModeFamily.Parameter.Mode(
                mode =
                    ThemeMode.Dark,
            )

        val act =
            family.activate(
                ThemeModeFamily.Control.Select(
                    mode,
                ),
            )

        val effect =
            act?.effect as
                ThemeModeFamily.Effect.Save

        assertSame(
            mode,
            effect.mode,
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
                    ThemeModeFamily.Parameter.Mode(
                        mode =
                            ThemeMode.Dark,
                    ),
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
                ThemeModeFamily.Parameter.Mode(
                    mode =
                        ThemeMode.Dark,
                ),
            ),
            darkAct?.effect,
        )
        assertEquals(
            VexExecution.Approach.Latest,
            darkAct?.begin
                ?.approach,
        )

        val sameAct =
            family.activate(
                ThemeModeFamily.Control.Select(
                    ThemeModeFamily.Parameter.Mode(
                        mode =
                            ThemeMode.Dark,
                    ),
                ),
            )

        assertEquals(
            ThemeMode.Dark,
            family.currentState,
        )
        assertEquals(
            ThemeModeFamily.Effect.Save(
                ThemeModeFamily.Parameter.Mode(
                    mode =
                        ThemeMode.Dark,
                ),
            ),
            sameAct?.effect,
        )
    }

    @Test
    fun selectPayloadDoesNotBecomeFamilyExecutionRelationshipIdentity() {
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeMode.Light,
            )

        val dark =
            family.activate(
                ThemeModeFamily.Control.Select(
                    ThemeModeFamily.Parameter.Mode(
                        mode =
                            ThemeMode.Dark,
                    ),
                ),
            )
        val light =
            family.activate(
                ThemeModeFamily.Control.Select(
                    ThemeModeFamily.Parameter.Mode(
                        mode =
                            ThemeMode.Light,
                    ),
                ),
            )

        assertEquals(
            dark?.relationshipId,
            light?.relationshipId,
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
                    ThemeModeFamily.Parameter.Mode(
                        mode =
                            ThemeMode.Light,
                    ),
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
                    ThemeModeFamily.Parameter.Mode(
                        mode =
                            ThemeMode.NotSet,
                    ),
                )
            }.exceptionOrNull()

        assertTrue(
            invalidSelection is
                IllegalArgumentException,
        )
    }
}

// [VXG RealForever]
