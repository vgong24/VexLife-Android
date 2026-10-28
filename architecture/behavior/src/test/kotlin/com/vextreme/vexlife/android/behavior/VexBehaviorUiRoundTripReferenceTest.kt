package com.vextreme.vexlife.android.behavior

import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.relay.VexState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VexBehaviorUiRoundTripReferenceTest {
    private class ThemeModeFamily(
        initialState: State,
        executionRoot: VexExecutionRoot,
    ) :
        VexBehaviorV2Family<
            ThemeModeFamily,
            ThemeModeFamily.State,
            ThemeModeFamily.Control
        >(
            initialState =
                initialState,
            executionRoot =
                executionRoot,
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
                state<State.NotSet>()

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
        val root =
            VexExecutionRoot()

        try {
            val themeMode =
                ThemeModeFamily(
                    initialState =
                        ThemeModeFamily.State.Light,
                    executionRoot =
                        root,
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
        } finally {
            root.forceStopAll()
        }
    }

    @Test
    fun activationUsesFamilyCurrentStateInsteadOfStaleRenderedDisplayState() {
        val root =
            VexExecutionRoot()

        try {
            val themeMode =
                ThemeModeFamily(
                    initialState =
                        ThemeModeFamily.State.Light,
                    executionRoot =
                        root,
                )

            val ui =
                ReferenceComposableUi(
                    themeMode =
                        themeMode,
                )

            val stale =
                ui.render()

            assertEquals(
                ThemeModeFamily.DisplayState.Inactive(
                    ThemeModeFamily.State.Light,
                ),
                stale.displayState,
            )

            themeMode.activate(
                ThemeModeFamily.Control.Toggle,
            )
            ui.onActivate()

            assertSame(
                ThemeModeFamily.State.Light,
                themeMode.current,
            )
        } finally {
            root.forceStopAll()
        }
    }
}

// [VXG RealForever]
