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

        sealed interface Control :
            VexBehaviorFamily.Control<
                ThemeModeFamily
            > {
            data object Toggle :
                Control
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

        val Behavior =
            vexBehaviorV2(
                family =
                    this,
                behaviorRef =
                    "example.theme-mode.v2",
                controlToAct = {
                        current: State,
                        control: Control,
                    ->
                    when (control) {
                        Control.Toggle ->
                            when (current) {
                                State.NotSet ->
                                    Act.CommitDark

                                State.Light ->
                                    Act.SelectDark

                                State.Dark ->
                                    Act.SelectLight
                            }
                    }
                },
            ) {
                state<State.NotSet> {
                    on(
                        act =
                            Act.CommitDark,
                        effect =
                            begin(
                                mode =
                                    VexBehaviorV2BeginMode
                                        .Sequenced,
                            ) {
                                save(
                                    State.Dark,
                                ).results {
                                    success {
                                        admit<
                                            State.Dark
                                        >()
                                    }

                                    failure {
                                    }
                                }
                            },
                    ).stay()
                }

                state<State.Light> {
                    on(
                        act =
                            Act.SelectDark,
                        effect =
                            begin(
                                mode =
                                    VexBehaviorV2BeginMode
                                        .Replaceable,
                            ) {
                                save(
                                    State.Dark,
                                ).results {
                                    success {
                                    }

                                    failure {
                                    }
                                }
                            },
                    ).admit<
                        State.Dark
                    >()
                }

                state<State.Dark> {
                    on(
                        act =
                            Act.SelectLight,
                        effect =
                            begin(
                                mode =
                                    VexBehaviorV2BeginMode
                                        .Replaceable,
                            ) {
                                save(
                                    State.Light,
                                ).results {
                                    success {
                                    }

                                    failure {
                                    }
                                }
                            },
                    ).admit<
                        State.Light
                    >()
                }
            }
    }

    @Test
    fun familyBehaviorResolvesControlThroughActToEffectInOneCall() {
        val resolution =
            ThemeModeFamily.Behavior
                .resolve(
                    current =
                        ThemeModeFamily.State.Light,
                    control =
                        ThemeModeFamily.Control.Toggle,
                )

        assertSame(
            ThemeModeFamily.Act.SelectDark,
            resolution
                ?.act,
        )
        assertSame(
            ThemeModeFamily.State.Dark,
            resolution
                ?.effect
                ?.save
                ?.state,
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
    fun saveCarriesTheActualThemeModeStateFromTheOriginWalk() {
        val resolution =
            ThemeModeFamily.Behavior
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
            ThemeModeFamily.Behavior
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
            ThemeModeFamily.Behavior
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
            controlToAct = {
                    current: ThemeModeFamily.State,
                    control: ThemeModeFamily.Control,
                ->
                when (control) {
                    ThemeModeFamily.Control.Toggle ->
                        when (current) {
                            ThemeModeFamily.State.NotSet ->
                                ThemeModeFamily.Act.CommitDark

                            ThemeModeFamily.State.Light ->
                                ThemeModeFamily.Act.SelectDark

                            ThemeModeFamily.State.Dark ->
                                ThemeModeFamily.Act.SelectLight
                        }
                }
            },
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
