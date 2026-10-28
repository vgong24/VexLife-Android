package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.relay.VexState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Canonical fresh-entry reference for the current V2 Family/UI Origin Walk.
 *
 * UI receives one live Family, reads its DisplayState, and calls activate(Control). The Family
 * re-reads CURRENT semantic State, selects the authored Act, applies its immediate consequence, and
 * exposes the resulting DisplayState.
 *
 * Store / Connection / provider execution is deliberately not introduced merely to complete the UI
 * loop. Effect-bearing third-party truth is the next Origin pressure.
 */
class VexBehaviorUiRoundTripReferenceTest {
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
                    "reference.theme-mode",
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

    private data class ReferenceUiState(
        val displayState:
            ThemeModeFamily.DisplayState,
    )

    private class ReferenceComposableUi(
        private val themeMode:
            ThemeModeFamily,
    ) {
        fun render():
            ReferenceUiState =
            ReferenceUiState(
                displayState =
                    themeMode.displayState,
            )

        fun onActivate() {
            themeMode.activate(
                ThemeModeFamily.Control.Toggle,
            )
        }
    }

    @Test
    fun uiReadsDisplayStateAndActivatesTheSameLiveFamily() {
        val themeMode =
            ThemeModeFamily(
                initialState =
                    ThemeModeFamily.State.Light,
            )

        val ui =
            ReferenceComposableUi(
                themeMode =
                    themeMode,
            )

        assertEquals(
            ReferenceUiState(
                displayState =
                    ThemeModeFamily.DisplayState.Inactive(
                        ThemeModeFamily.State.Light,
                    ),
            ),
            ui.render(),
        )

        ui.onActivate()

        assertSame(
            ThemeModeFamily.State.Dark,
            themeMode.current,
        )

        assertEquals(
            ReferenceUiState(
                displayState =
                    ThemeModeFamily.DisplayState.Active(
                        ThemeModeFamily.State.Dark,
                    ),
            ),
            ui.render(),
        )
    }

    @Test
    fun activationUsesFamilyCurrentStateInsteadOfStaleRenderedDisplayState() {
        val themeMode =
            ThemeModeFamily(
                initialState =
                    ThemeModeFamily.State.Light,
            )

        val ui =
            ReferenceComposableUi(
                themeMode =
                    themeMode,
            )

        val staleRender =
            ui.render()

        assertEquals(
            ThemeModeFamily.DisplayState.Inactive(
                ThemeModeFamily.State.Light,
            ),
            staleRender.displayState,
        )

        themeMode.activate(
            ThemeModeFamily.Control.Toggle,
        )

        assertSame(
            ThemeModeFamily.State.Dark,
            themeMode.current,
        )

        ui.onActivate()

        assertSame(
            ThemeModeFamily.State.Light,
            themeMode.current,
        )

        assertEquals(
            ThemeModeFamily.DisplayState.Inactive(
                ThemeModeFamily.State.Light,
            ),
            ui.render()
                .displayState,
        )
    }
}

// [VXG RealForever]
