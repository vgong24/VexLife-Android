package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.behavior.VexFamily

/**
 * Android realization of the ThemeMode semantic State domain.
 *
 * ThemeMode is ordinary Family State. It does not encode initialization/default policy,
 * presentation form, persistence, or platform-appearance reconciliation.
 *
 * [NotSet] preserves the explicit distinction that no effective ThemeMode has yet been resolved.
 * It is not null, not persistence failure, and not silently equivalent to [Light] or [Dark].
 */
internal enum class ThemeMode :
    VexFamily.State<ThemeModeFamily> {
    NotSet,
    Light,
    Dark,
}

/**
 * Smallest current ThemeMode Family earned by the Debug Menu patient zero.
 *
 * ThemeMode values are peer semantic choices. The Family accepts an explicit Select request rather
 * than making today's two-state Switch the semantic contract. A target may realize Light/Dark with
 * a toggle, segmented control, menu, or another presentation without changing this relationship.
 *
 * The selected mode is Control payload. Runtime persistence identity/order belong to
 * ThemeModeConnection, not to this semantic Family.
 */
internal class ThemeModeFamily(
    initialState:
        ThemeMode,
) :
    VexFamily<
        ThemeModeFamily,
        ThemeMode,
        ThemeModeFamily.Control,
        ThemeModeFamily.Effect
    >(
        initialState =
            initialState,
    ) {


    sealed interface Control :
        VexFamily.Control<ThemeModeFamily> {
        data class Select(
            val mode: ThemeMode,
        ) :
            Control {
            init {
                require(
                    mode !=
                        ThemeMode.NotSet,
                ) {
                    "ThemeMode.NotSet is unresolved state, not a selectable mode"
                }
            }
        }
    }

    sealed interface Effect :
        VexFamily.Effect<ThemeModeFamily> {
        /**
         * Remember the explicit semantic selection durably.
         *
         * This requests persistence; it does not claim persistence success or another ThemeMode State.
         */
        data class PersistSelection(
            val mode:
                ThemeMode,
        ) :
            Effect
    }

    sealed interface DisplayState :
        VexFamily.DisplayState<ThemeMode> {
        val presentation:
            VexThemePresentation

        data object Unresolved :
            DisplayState {
            override val presentation =
                VexThemePresentation.Unresolved
        }

        data object Light :
            DisplayState {
            override val presentation =
                VexThemePresentation.Light
        }

        data object Dark :
            DisplayState {
            override val presentation =
                VexThemePresentation.Dark
        }
    }

    internal fun displayStateFor(
        state: ThemeMode,
    ):
        DisplayState =
        when (state) {
            ThemeMode.NotSet ->
                DisplayState.Unresolved

            ThemeMode.Light ->
                DisplayState.Light

            ThemeMode.Dark ->
                DisplayState.Dark
        }

    override val displayState:
        DisplayState
        get() =
            displayStateFor(
                currentState,
            )

    override fun act(
        currentState:
            ThemeMode,
        control:
            Control,
    ):
        VexFamily.Act<
            ThemeMode,
            Effect
        >? =
        when (control) {
            is Control.Select -> {
                val effect =
                    Effect.PersistSelection(
                        mode =
                            control.mode,
                    )

                when (currentState) {
                    ThemeMode.NotSet ->
                        admit(
                            control.mode,
                            effect =
                                effect,
                        )

                    ThemeMode.Light,
                    ThemeMode.Dark ->
                        if (
                            currentState ==
                            control.mode
                        ) {
                            /*
                             * Selecting the already-current peer still means "remember this explicit
                             * choice durably"; same State does not erase the Effect.
                             */
                            stay(
                                effect =
                                    effect,
                            )
                        } else {
                            admit(
                                control.mode,
                                effect =
                                    effect,
                            )
                        }
                }
            }
        }

}
