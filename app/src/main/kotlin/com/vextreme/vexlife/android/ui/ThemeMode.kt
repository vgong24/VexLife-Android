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
 * Control kind alone is sufficient relationship identity: ThemeMode has no repeated resource whose
 * identity would earn a ResourceId. The selected mode is Control payload, not a ResourceId.
 */
internal class ThemeModeFamily(
    initialState: ThemeMode,
    executionRoot: VexExecutionRoot,
    logger: VexLogger.Sink = VexLogger.Sink.None,
    override val store: Store? = null,
) :
    VexFamily<
        ThemeModeFamily,
        ThemeMode,
        ThemeModeFamily.Control
    >(
        initialState = initialState,
        executionRoot = executionRoot,
        logger = logger,
    ) {
    /**
     * Android ThemeMode durable StateStore.
     *
     * Durable selection truth remains distinct from CURRENT semantic ThemeMode. The Family observes
     * this dependency through the shared StateRelay boundary; UI/session code never observes Store
     * directly.
     */
    interface Store :
        VexFamily.StateStore<ThemeMode>

    init {
        store
            ?.let { stateStore ->
                observeStore(
                    stateStore =
                        stateStore,
                ) { storedState ->
                    Control.Stored(
                        mode =
                            storedState,
                    )
                }
            }
    }

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

        /**
         * Durable ThemeMode truth observed from Store.
         *
         * Store observation is input, not State authority: it must re-enter the same
         * act(CURRENT State, Control) relationship as every other semantic cause.
         */
        data class Stored(
            val mode: ThemeMode,
        ) :
            Control
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
        when (currentState) {
            ThemeMode.NotSet ->
                when (control) {
                    is Control.Select ->
                        admit(
                            control.mode,
                            save =
                                store
                                    ?.let {
                                        saveLatest(
                                            control.mode,
                                        )
                                    },
                        )

                    is Control.Stored ->
                        when (control.mode) {
                            ThemeMode.NotSet ->
                                stay()

                            ThemeMode.Light,
                            ThemeMode.Dark ->
                                admit(
                                    control.mode,
                                )
                        }
                }

            ThemeMode.Light,
            ThemeMode.Dark ->
                when (control) {
                    is Control.Select -> {
                        val save =
                            store
                                ?.let {
                                    saveLatest(
                                        control.mode,
                                    )
                                }

                        if (
                            currentState ==
                            control.mode
                        ) {
                            /*
                             * Explicitly selecting the already-current peer still means "remember
                             * this choice durably"; same semantic State is not a reason to skip Save.
                             */
                            stay(
                                save =
                                    save,
                            )
                        } else {
                            admit(
                                control.mode,
                                save =
                                    save,
                            )
                        }
                    }

                    is Control.Stored ->
                        when (control.mode) {
                            ThemeMode.NotSet ->
                                stay()

                            ThemeMode.Light,
                            ThemeMode.Dark ->
                                if (
                                    currentState ==
                                    control.mode
                                ) {
                                    stay()
                                } else {
                                    admit(
                                        control.mode,
                                    )
                                }
                        }
                }
        }

}
