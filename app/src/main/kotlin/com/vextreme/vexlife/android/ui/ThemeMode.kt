package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.logging.VexLogger

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
 * The selected mode is Control payload. Direct Family does not currently expose feature-authored
 * execution identity; ThemeMode persistence is ordered privately per live Family.
 */
internal class ThemeModeFamily(
    initialState: ThemeMode,
    executionRoot: VexExecutionRoot,
    logger: VexLogger.Sink = VexLogger.Sink.None,
    private val store: Store? = null,
) :
    VexFamily<
        ThemeModeFamily,
        ThemeMode,
        ThemeModeFamily.Control
    >(
        initialState = initialState,
        executionRoot = executionRoot,
        logger = logger,
        store = store,
    ) {
    /**
     * Android ThemeMode durable Store.
     *
     * A fresh process reads the durable selection before constructing this Family. During the live
     * Family lifetime, explicit Select Controls may author Save consequences; Store does not become
     * another live State owner or another semantic input stream.
     */
    interface Store :
        VexFamily.Store<ThemeMode>



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
        currentState: ThemeMode,
        control: Control,
    ):
        VexFamily.Act<ThemeMode>? =
        when (control) {
            is Control.Select -> {
                val saveState =
                    if (
                        store !=
                        null
                    ) {
                        control.mode
                    } else {
                        null
                    }

                when (currentState) {
                    ThemeMode.NotSet ->
                        admit(
                            control.mode,
                            saveState =
                                saveState,
                        )

                    ThemeMode.Light,
                    ThemeMode.Dark ->
                        if (
                            currentState ==
                            control.mode
                        ) {
                            /*
                             * Explicitly selecting the already-current peer still means "remember
                             * this choice durably"; same semantic State is not a reason to skip Save.
                             */
                            stay(
                                saveState =
                                    saveState,
                            )
                        } else {
                            admit(
                                control.mode,
                                saveState =
                                    saveState,
                            )
                        }
                }
            }
        }

}
