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

    override fun act(
        currentState: ThemeMode,
        control: Control,
    ):
        VexFamily.Act<ThemeMode>? =
        when (control) {
            is Control.Select ->
                when (currentState) {
                    ThemeMode.NotSet ->
                        admit(
                            control.mode,
                        )

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
