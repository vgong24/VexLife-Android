package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.relay.VexState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexBehaviorV2Test {
    private class ThemeModeFamily(
        initialState: State,
    ) :
        VexBehaviorV2Family<
            ThemeModeFamily,
            ThemeModeFamily.State,
            ThemeModeFamily.Control
        >(
            initialState =
                initialState,
        ) {
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

        sealed interface DisplayState :
            VexBehaviorV2DisplayState<
                State
            > {
            data class Empty(
                override val state:
                    State,
            ) :
                DisplayState

            data class Active(
                override val state:
                    State,
            ) :
                DisplayState

            data class Inactive(
                override val state:
                    State,
            ) :
                DisplayState
        }

        override val acts =
            vexActs<
                ThemeModeFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "example.theme-mode.v2",
            ) {
                state<State.NotSet> {
                    on(
                        Control.Toggle,
                    ) {
                        stay()

                        begin(
                            mode =
                                VexBehaviorV2BeginMode.Sequenced,
                        ) {
                            save(
                                State.Dark,
                            ).results {
                                success {
                                    admit(
                                        State.Dark,
                                    )
                                }

                                failure {
                                }
                            }
                        }
                    }
                }

                state<State.Light> {
                    on(
                        Control.Toggle,
                    ) {
                        admit(
                            State.Dark,
                        )
                    }
                }

                state<State.Dark> {
                    on(
                        Control.Toggle,
                    ) {
                        admit(
                            State.Light,
                        )
                    }
                }
            }

        override val displayState:
            DisplayState
            get() =
                when (
                    val state =
                        current
                ) {
                    State.NotSet ->
                        DisplayState.Empty(
                            state,
                        )

                    State.Light ->
                        DisplayState.Inactive(
                            state,
                        )

                    State.Dark ->
                        DisplayState.Active(
                            state,
                        )
                }
    }

    private class MinimalFamily(
        initialState: State,
    ) :
        VexBehaviorV2Family<
            MinimalFamily,
            MinimalFamily.State,
            MinimalFamily.Control
        >(
            initialState =
                initialState,
        ) {
        sealed interface State :
            VexBehaviorFamily.State<
                MinimalFamily
            > {
            data object Idle :
                State
        }

        sealed interface Control :
            VexBehaviorFamily.Control<
                MinimalFamily
            > {
            data object Keep :
                Control
        }

        override val acts =
            vexActs<
                MinimalFamily,
                State,
                Control
            >(
                family =
                    this,
                behaviorRef =
                    "example.minimal.v2",
            ) {
                state<State.Idle> {
                    on(
                        Control.Keep,
                    ) {
                        stay()
                    }
                }
            }
    }

    @Test
    fun activateUsesFamilyCurrentStateAndAppliesTheAuthoredAct() {
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeModeFamily.State.Light,
            )

        assertEquals(
            ThemeModeFamily.DisplayState.Inactive(
                ThemeModeFamily.State.Light,
            ),
            family.displayState,
        )

        val act =
            family.activate(
                ThemeModeFamily.Control.Toggle,
            )

        assertSame(
            ThemeModeFamily.Control.Toggle,
            act?.control,
        )
        assertNull(
            act?.effect,
        )
        assertEquals(
            VexBehaviorV2StateDispositionKind.Admit,
            act?.immediateDisposition
                ?.kind,
        )
        assertSame(
            ThemeModeFamily.State.Dark,
            family.current,
        )
        assertEquals(
            ThemeModeFamily.DisplayState.Active(
                ThemeModeFamily.State.Dark,
            ),
            family.displayState,
        )
    }

    @Test
    fun sameControlUsesLiveCurrentStateWithoutCallerSupplyingState() {
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeModeFamily.State.Light,
            )

        family.activate(
            ThemeModeFamily.Control.Toggle,
        )
        assertSame(
            ThemeModeFamily.State.Dark,
            family.current,
        )

        family.activate(
            ThemeModeFamily.Control.Toggle,
        )
        assertSame(
            ThemeModeFamily.State.Light,
            family.current,
        )
    }

    @Test
    fun durabilityGatedActPreservesBeginResultsAndStaysNow() {
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeModeFamily.State.NotSet,
            )

        val act =
            family.activate(
                ThemeModeFamily.Control.Toggle,
            )

        assertSame(
            ThemeModeFamily.State.NotSet,
            family.current,
        )
        assertEquals(
            VexBehaviorV2BeginMode.Sequenced,
            act?.effect
                ?.mode,
        )
        assertSame(
            ThemeModeFamily.State.Dark,
            act?.effect
                ?.save
                ?.state,
        )
        assertEquals(
            VexBehaviorV2StateDispositionKind.Stay,
            act?.immediateDisposition
                ?.kind,
        )
        assertSame(
            ThemeModeFamily.State.Dark,
            act?.effect
                ?.save
                ?.results
                ?.success
                ?.admittedState,
        )
        assertNull(
            act?.effect
                ?.save
                ?.results
                ?.failure
                ?.admittedState,
        )
    }

    @Test
    fun displayStateIsAlwaysPresentAndDefaultsToEmptyCurrentState() {
        val family =
            MinimalFamily(
                initialState =
                    MinimalFamily.State.Idle,
            )

        assertEquals(
            VexBehaviorV2DisplayState.Empty(
                MinimalFamily.State.Idle,
            ),
            family.displayState,
        )
    }

    @Test
    fun themeModeStateCanAlsoBeRelayVexStateWithoutInventingAnotherStateNoun() {
        val copied:
            ThemeModeFamily.State =
            ThemeModeFamily.State.Dark
                .deepCopy()

        assertSame(
            ThemeModeFamily.State.Dark,
            copied,
        )

        val family =
            ThemeModeFamily(
                initialState =
                    ThemeModeFamily.State.Light,
            )

        assertTrue(
            family.acts
                .describe()
                .contains(
                    "Light --on(Toggle)",
                ),
        )
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun saveResultsStillRequireExplicitSuccessAndFailure() {
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeModeFamily.State.Light,
            )

        vexActs<
            ThemeModeFamily,
            ThemeModeFamily.State,
            ThemeModeFamily.Control
        >(
            family =
                family,
            behaviorRef =
                "example.theme-mode.v2.incomplete-results",
        ) {
            state<ThemeModeFamily.State.Light> {
                on(
                    ThemeModeFamily.Control.Toggle,
                ) {
                    admit(
                        ThemeModeFamily.State.Dark,
                    )

                    begin(
                        mode =
                            VexBehaviorV2BeginMode.Replaceable,
                    ) {
                        save(
                            ThemeModeFamily.State.Dark,
                        ).results {
                            success {
                            }
                        }
                    }
                }
            }
        }
    }

    @Test(
        expected =
            IllegalArgumentException::class,
    )
    fun everyAuthoredActRequiresAnImmediateStayOrAdmit() {
        val family =
            ThemeModeFamily(
                initialState =
                    ThemeModeFamily.State.Light,
            )

        vexActs<
            ThemeModeFamily,
            ThemeModeFamily.State,
            ThemeModeFamily.Control
        >(
            family =
                family,
            behaviorRef =
                "example.theme-mode.v2.missing-immediate",
        ) {
            state<ThemeModeFamily.State.Light> {
                on(
                    ThemeModeFamily.Control.Toggle,
                ) {
                }
            }
        }
    }
}

// [VXG RealForever]
