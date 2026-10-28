package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.relay.VexState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexBehaviorV2Test {
    private object ThemeModeFamily :
        VexBehaviorFamily<
            ThemeModeFamily
        >,
        VexBehaviorFamily.Members<
            ThemeModeFamily,
            ThemeModeFamily.State,
            ThemeModeFamily.Control,
            ThemeModeFamily.Act,
            VexBehaviorV2Begin<
                ThemeModeFamily,
                ThemeModeFamily.State
            >
        > {
        sealed interface State :
            VexBehaviorFamily.State<
                ThemeModeFamily
            >,
            VexState<
                State
            > {
            data object NotSet :
                State

            data object Light :
                State

            data object Dark :
                State

            override fun deepCopy():
                State =
                this
        }

        enum class Control :
            VexBehaviorFamily.Control<
                ThemeModeFamily
            > {
            Toggle,
        }

        sealed interface Act :
            VexBehaviorFamily.Act<
                ThemeModeFamily
            > {
            data object CommitDark :
                Act

            data object SelectDark :
                Act

            data object SelectLight :
                Act
        }

        override val controls =
            Control.entries
                .toSet()
    }

    private fun themeModeBehavior() =
        vexBehaviorV2(
            family =
                ThemeModeFamily,
            behaviorRef =
                "example.theme-mode.v2",
        ) {
            state<ThemeModeFamily.State.NotSet> {
                on(
                    act =
                        ThemeModeFamily.Act.CommitDark,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode
                                    .Sequenced,
                        ) {
                            save(
                                ThemeModeFamily.State.Dark,
                            ).results {
                                success {
                                    admit<
                                        ThemeModeFamily.State.Dark
                                    >()
                                }

                                failure {
                                }
                            }
                        },
                ).stay()
            }

            state<ThemeModeFamily.State.Light> {
                on(
                    act =
                        ThemeModeFamily.Act.SelectDark,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode
                                    .Replaceable,
                        ) {
                            save(
                                ThemeModeFamily.State.Dark,
                            ).results {
                                success {
                                }

                                failure {
                                }
                            }
                        },
                ).admit<
                    ThemeModeFamily.State.Dark
                >()
            }

            state<ThemeModeFamily.State.Dark> {
                on(
                    act =
                        ThemeModeFamily.Act.SelectLight,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode
                                    .Replaceable,
                        ) {
                            save(
                                ThemeModeFamily.State.Light,
                            ).results {
                                success {
                                }

                                failure {
                                }
                            }
                        },
                ).admit<
                    ThemeModeFamily.State.Light
                >()
            }
        }

    @Test
    fun saveCarriesTheActualThemeModeStateFromTheOriginWalk() {
        val resolution =
            themeModeBehavior()
                .resolve(
                    current =
                        ThemeModeFamily.State.Light,
                    act =
                        ThemeModeFamily.Act.SelectDark,
                )

        assertSame(
            ThemeModeFamily.State.Dark,
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
            ThemeModeFamily.State.Dark::class,
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
                        ThemeModeFamily.State.NotSet,
                    act =
                        ThemeModeFamily.Act.CommitDark,
                )

        assertSame(
            ThemeModeFamily.State.Dark,
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
            ThemeModeFamily.State.Dark::class,
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
                .controls {
                    control(
                        ThemeModeFamily.Control.Toggle,
                        ThemeModeFamily.Act.SelectDark,
                        ThemeModeFamily.Act.SelectLight,
                        ThemeModeFamily.Act.CommitDark,
                    )
                }

        assertSame(
            ThemeModeFamily.Act.SelectDark,
            controls.resolve(
                current =
                    ThemeModeFamily.State.Light,
                control =
                    ThemeModeFamily.Control.Toggle,
            )?.act,
        )
    }

    @Test
    fun themeModeCanAlsoBeRelayVexStateWithoutRenamingTheSemanticNoun() {
        val copied:
            ThemeModeFamily.State =
            ThemeModeFamily.State.Dark
                .deepCopy()

        assertSame(
            ThemeModeFamily.State.Dark,
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
        vexBehaviorV2(
            family =
                ThemeModeFamily,
            behaviorRef =
                "example.theme-mode.v2.incomplete-results",
        ) {
            state<ThemeModeFamily.State.Light> {
                on(
                    act =
                        ThemeModeFamily.Act.SelectDark,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode
                                    .Replaceable,
                        ) {
                            save(
                                ThemeModeFamily.State.Dark,
                            ).results {
                                success {
                                }
                            }
                        },
                ).admit<
                    ThemeModeFamily.State.Dark
                >()
            }

            state<ThemeModeFamily.State.Dark>()
        }
    }
}

// [VXG RealForever]
