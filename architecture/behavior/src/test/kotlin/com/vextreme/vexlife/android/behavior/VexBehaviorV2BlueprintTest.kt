package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.relay.VexState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexBehaviorV2BlueprintTest {
    private sealed interface ThemeModeFamily :
        VexBehaviorFamily<
            ThemeModeFamily
        >

    private sealed interface ThemeMode :
        VexBehaviorFamily.State<
            ThemeModeFamily
        >,
        VexState<
            ThemeMode
        > {
        data object NotSet :
            ThemeMode

        data object Light :
            ThemeMode

        data object Dark :
            ThemeMode

        override fun deepCopy():
            ThemeMode =
            this
    }

    private enum class ThemeModeControl :
        VexBehaviorFamily.Control<
            ThemeModeFamily
        > {
        Toggle,
    }

    private sealed interface ThemeModeAct :
        VexBehaviorFamily.Act<
            ThemeModeFamily
        > {
        data object CommitDark :
            ThemeModeAct

        data object SelectDark :
            ThemeModeAct

        data object SelectLight :
            ThemeModeAct
    }

    private fun themeModeBehavior() =
        vexBehaviorV2Blueprint<
            ThemeModeFamily,
            ThemeMode,
            ThemeModeAct
        >(
            behaviorRef =
                "example.theme-mode.v2",
        ) {
            state<ThemeMode.NotSet> {
                on(
                    act =
                        ThemeModeAct.CommitDark,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode
                                    .Sequenced,
                        ) {
                            save(
                                ThemeMode.Dark,
                            ).results {
                                success {
                                    admit<
                                        ThemeMode.Dark
                                    >()
                                }

                                failure {
                                }
                            }
                        },
                ).stay()
            }

            state<ThemeMode.Light> {
                on(
                    act =
                        ThemeModeAct.SelectDark,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode
                                    .Replaceable,
                        ) {
                            save(
                                ThemeMode.Dark,
                            ).results {
                                success {
                                }

                                failure {
                                }
                            }
                        },
                ).admit<
                    ThemeMode.Dark
                >()
            }

            state<ThemeMode.Dark> {
                on(
                    act =
                        ThemeModeAct.SelectLight,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode
                                    .Replaceable,
                        ) {
                            save(
                                ThemeMode.Light,
                            ).results {
                                success {
                                }

                                failure {
                                }
                            }
                        },
                ).admit<
                    ThemeMode.Light
                >()
            }
        }

    @Test
    fun saveCarriesTheActualThemeModeStateFromTheOriginWalk() {
        val resolution =
            themeModeBehavior()
                .resolve(
                    current =
                        ThemeMode.Light,
                    act =
                        ThemeModeAct.SelectDark,
                )

        assertSame(
            ThemeMode.Dark,
            resolution
                ?.effect
                ?.save
                ?.state,
        )
        assertEquals(
            VexBehaviorV2BeginMode.Replaceable,
            resolution
                ?.effect
                ?.mode,
        )
        assertEquals(
            VexBehaviorV2StateDispositionKind.Admit,
            resolution
                ?.immediateDisposition
                ?.kind,
        )
        assertEquals(
            ThemeMode.Dark::class,
            resolution
                ?.immediateDisposition
                ?.stateKind,
        )
    }

    @Test
    fun durabilityGatedSaveCanAdmitTheSameStateLater() {
        val resolution =
            themeModeBehavior()
                .resolve(
                    current =
                        ThemeMode.NotSet,
                    act =
                        ThemeModeAct.CommitDark,
                )

        assertSame(
            ThemeMode.Dark,
            resolution
                ?.effect
                ?.save
                ?.state,
        )
        assertEquals(
            VexBehaviorV2StateDispositionKind.Stay,
            resolution
                ?.immediateDisposition
                ?.kind,
        )
        assertEquals(
            ThemeMode.Dark::class,
            resolution
                ?.effect
                ?.save
                ?.results
                ?.success
                ?.admittedStateKind,
        )
        assertNull(
            resolution
                ?.effect
                ?.save
                ?.results
                ?.failure
                ?.admittedStateKind,
        )
    }

    @Test
    fun v2ReusesV1ControlMeaningInsteadOfCreatingAnotherControlSystem() {
        val controls =
            themeModeBehavior()
                .controls<
                    ThemeModeControl
                > {
                    control(
                        ThemeModeControl.Toggle,
                        ThemeModeAct.SelectDark,
                        ThemeModeAct.SelectLight,
                        ThemeModeAct.CommitDark,
                    )
                }

        assertSame(
            ThemeModeAct.SelectDark,
            controls.resolve(
                current =
                    ThemeMode.Light,
                control =
                    ThemeModeControl.Toggle,
            )?.act,
        )
    }

    @Test
    fun themeModeCanAlsoBeRelayVexStateWithoutRenamingTheSemanticNoun() {
        val copied:
            ThemeMode =
            ThemeMode.Dark
                .deepCopy()

        assertSame(
            ThemeMode.Dark,
            copied,
        )

        assertTrue(
            themeModeBehavior()
                .describe()
                .contains(
                    "Save(Dark)",
                ),
        )
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun resultsRequireExplicitSuccessAndFailure() {
        vexBehaviorV2Blueprint<
            ThemeModeFamily,
            ThemeMode,
            ThemeModeAct
        >(
            behaviorRef =
                "example.theme-mode.v2.incomplete-results",
        ) {
            state<ThemeMode.Light> {
                on(
                    act =
                        ThemeModeAct.SelectDark,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode
                                    .Replaceable,
                        ) {
                            save(
                                ThemeMode.Dark,
                            ).results {
                                success {
                                }
                            }
                        },
                ).admit<
                    ThemeMode.Dark
                >()
            }

            state<ThemeMode.Dark>()
        }
    }
}

// [VXG RealForever]
