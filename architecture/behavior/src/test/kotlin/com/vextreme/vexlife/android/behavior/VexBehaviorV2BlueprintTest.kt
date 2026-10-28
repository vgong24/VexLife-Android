package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.relay.VexState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexBehaviorV2BlueprintTest {
    private sealed interface ThemeFamily :
        VexBehaviorFamily<
            ThemeFamily
        >

    private sealed interface ThemeState :
        VexBehaviorFamily.State<
            ThemeFamily
        >,
        VexState<
            ThemeState
        > {
        data object NotSet :
            ThemeState

        data object Light :
            ThemeState

        data object Dark :
            ThemeState

        override fun deepCopy():
            ThemeState =
            this
    }

    private enum class ThemeControl :
        VexBehaviorFamily.Control<
            ThemeFamily
        > {
        Toggle,
    }

    private sealed interface ThemeAct :
        VexBehaviorFamily.Act<
            ThemeFamily
        > {
        data object CommitDark :
            ThemeAct

        data object SelectDark :
            ThemeAct

        data object SelectLight :
            ThemeAct
    }

    private sealed interface ThemeEffect :
        VexBehaviorFamily.Effect<
            ThemeFamily
        > {
        data object SaveDark :
            ThemeEffect

        data object SaveLight :
            ThemeEffect
    }

    private fun themeBlueprint() =
        vexBehaviorV2Blueprint<
            ThemeFamily,
            ThemeState,
            ThemeAct,
            ThemeEffect
        >(
            behaviorRef =
                "example.theme.v2",
        ) {
            state<ThemeState.NotSet> {
                on(
                    act =
                        ThemeAct.CommitDark,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode
                                    .Sequenced,
                        ) {
                            operation(
                                ThemeEffect.SaveDark,
                            ).results {
                                success {
                                    admit<
                                        ThemeState.Dark
                                    >()
                                }

                                failure {
                                }
                            }
                        },
                ).stay()
            }

            state<ThemeState.Light> {
                on(
                    act =
                        ThemeAct.SelectDark,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode
                                    .Replaceable,
                        ) {
                            operation(
                                ThemeEffect.SaveDark,
                            ).results {
                                success {
                                }

                                failure {
                                }
                            }
                        },
                ).admit<
                    ThemeState.Dark
                >()
            }

            state<ThemeState.Dark> {
                on(
                    act =
                        ThemeAct.SelectLight,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode
                                    .Replaceable,
                        ) {
                            operation(
                                ThemeEffect.SaveLight,
                            ).results {
                                success {
                                }

                                failure {
                                }
                            }
                        },
                ).admit<
                    ThemeState.Light
                >()
            }
        }

    @Test
    fun v2KeepsImmediateAndLaterAdmissionAsSeparateDecisionPoints() {
        val resolution =
            themeBlueprint()
                .resolve(
                    current =
                        ThemeState.NotSet,
                    act =
                        ThemeAct.CommitDark,
                )

        assertSame(
            ThemeEffect.SaveDark,
            resolution?.effect,
        )
        assertEquals(
            VexBehaviorV2StateDispositionKind.Stay,
            resolution
                ?.immediateDisposition
                ?.kind,
        )
        assertEquals(
            ThemeState.NotSet::class,
            resolution
                ?.immediateDisposition
                ?.stateKind,
        )
        assertEquals(
            VexBehaviorV2BeginMode.Sequenced,
            resolution
                ?.begin
                ?.mode,
        )
        assertEquals(
            ThemeState.Dark::class,
            resolution
                ?.begin
                ?.operation
                ?.results
                ?.success
                ?.admittedStateKind,
        )
        assertNull(
            resolution
                ?.begin
                ?.operation
                ?.results
                ?.failure
                ?.admittedStateKind,
        )
    }

    @Test
    fun sameEffectIdentityDoesNotOwnAdmissionTiming() {
        val durabilityGated =
            themeBlueprint()
                .resolve(
                    current =
                        ThemeState.NotSet,
                    act =
                        ThemeAct.CommitDark,
                )

        val immediate =
            themeBlueprint()
                .resolve(
                    current =
                        ThemeState.Light,
                    act =
                        ThemeAct.SelectDark,
                )

        assertSame(
            ThemeEffect.SaveDark,
            durabilityGated?.effect,
        )
        assertSame(
            ThemeEffect.SaveDark,
            immediate?.effect,
        )

        assertEquals(
            VexBehaviorV2StateDispositionKind.Stay,
            durabilityGated
                ?.immediateDisposition
                ?.kind,
        )
        assertEquals(
            ThemeState.Dark::class,
            durabilityGated
                ?.begin
                ?.operation
                ?.results
                ?.success
                ?.admittedStateKind,
        )

        assertEquals(
            VexBehaviorV2StateDispositionKind.Admit,
            immediate
                ?.immediateDisposition
                ?.kind,
        )
        assertEquals(
            ThemeState.Dark::class,
            immediate
                ?.immediateDisposition
                ?.stateKind,
        )
        assertNull(
            immediate
                ?.begin
                ?.operation
                ?.results
                ?.success
                ?.admittedStateKind,
        )
    }

    @Test
    fun v2ReusesV1ControlMeaningInsteadOfCreatingAnotherControlSystem() {
        val controls =
            themeBlueprint()
                .controls<ThemeControl> {
                    control(
                        ThemeControl.Toggle,
                        ThemeAct.SelectDark,
                        ThemeAct.SelectLight,
                        ThemeAct.CommitDark,
                    )
                }

        assertSame(
            ThemeAct.SelectDark,
            controls.resolve(
                current =
                    ThemeState.Light,
                control =
                    ThemeControl.Toggle,
            )?.act,
        )
        assertSame(
            ThemeAct.SelectLight,
            controls.resolve(
                current =
                    ThemeState.Dark,
                control =
                    ThemeControl.Toggle,
            )?.act,
        )
        assertSame(
            ThemeAct.CommitDark,
            controls.resolve(
                current =
                    ThemeState.NotSet,
                control =
                    ThemeControl.Toggle,
            )?.act,
        )
    }

    @Test
    fun v2FamilyStateCanAlsoBeRelayVexStateWithoutChangingTheDsl() {
        val copied:
            ThemeState =
            ThemeState.Dark
                .deepCopy()

        assertSame(
            ThemeState.Dark,
            copied,
        )

        val description =
            themeBlueprint()
                .describe()

        assertTrue(
            description.contains(
                "begin[Replaceable]",
            ),
        )
        assertTrue(
            description.contains(
                "Success=Dark",
            ),
        )
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun resultsRequireExplicitSuccessAndFailure() {
        vexBehaviorV2Blueprint<
            ThemeFamily,
            ThemeState,
            ThemeAct,
            ThemeEffect
        >(
            behaviorRef =
                "example.theme.v2.incomplete-results",
        ) {
            state<ThemeState.Light> {
                on(
                    act =
                        ThemeAct.SelectDark,
                    effect =
                        begin(
                            mode =
                                VexBehaviorV2BeginMode
                                    .Replaceable,
                        ) {
                            operation(
                                ThemeEffect.SaveDark,
                            ).results {
                                success {
                                }
                            }
                        },
                ).admit<
                    ThemeState.Dark
                >()
            }

            state<ThemeState.Dark>()
        }
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun v2StillRejectsRelationshipWithoutImmediateStayOrAdmit() {
        vexBehaviorV2Blueprint<
            ThemeFamily,
            ThemeState,
            ThemeAct,
            ThemeEffect
        >(
            behaviorRef =
                "example.theme.v2.incomplete-immediate",
        ) {
            state<ThemeState.Light> {
                on(
                    act =
                        ThemeAct.SelectDark,
                    effect =
                        ThemeEffect.SaveDark,
                )
            }
        }
    }
}

// [VXG RealForever]
