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
 * The selected mode is Control payload. The Act authors Save as Latest here because overlap is part
 * of the Select consequence relationship; VexExecution later realizes that authored consequence.
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
         * Save the explicit semantic selection durably.
         *
         * This requests Save; it does not claim Save success or another ThemeMode State.
         */
        data class Save(
            val mode:
                ThemeMode,
        ) :
            Effect
    }

    sealed interface DisplayState :
        VexFamily.DisplayState<
            ThemeModeFamily,
            ThemeMode
        > {
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

    override fun displayStateFor(
        state:
            ThemeMode,
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

    override fun projectionFor(
        state:
            ThemeMode,
    ):
        VexFamily.Projection<
            ThemeModeFamily,
            ThemeMode,
            DisplayState
        > =
        VexFamily.Projection(
            state =
                state,
            displayState =
                displayStateFor(
                    state,
                ),
        )

    override val currentProjection:
        VexFamily.Projection<
            ThemeModeFamily,
            ThemeMode,
            DisplayState
        >
        get() =
            projectionFor(
                currentState,
            )

    override val displayState:
        DisplayState
        get() =
            currentProjection
                .displayState

    /**
     * Reconcile already-observed durable ThemeMode truth into the one Family State holder.
     *
     * NotSet means no explicit durable selection and therefore does not replace the currently
     * effective ThemeMode. This method owns that ThemeMode-specific interpretation; observation
     * lifetime belongs to VexExecution.
     */
    fun observeStoredThemeMode(
        observed:
            ThemeMode,
    ):
        ThemeMode {
        if (
            observed ==
            ThemeMode.NotSet
        ) {
            return currentState
        }

        return observe(
            observed,
        )
    }

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
                    Effect.Save(
                        mode =
                            control.mode,
                    )

                when (currentState) {
                    ThemeMode.NotSet ->
                        admit(
                            control.mode,
                            begin =
                                beginLatest(
                                    effect,
                                ),
                        )

                    ThemeMode.Light,
                    ThemeMode.Dark ->
                        if (
                            currentState ==
                            control.mode
                        ) {
                            /*
                             * Selecting the already-current peer still means "save this explicit choice durably";
                             * same State does not erase the Effect.
                             */
                            stay(
                                beginLatest(
                                    effect,
                                ),
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
