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


/** Boolean presentation projection of a resolved ThemeMode. */
internal fun ThemeMode.requireResolvedDarkTheme(): Boolean =
    when (this) {
        ThemeMode.NotSet ->
            throw IllegalStateException(
                "ThemeMode must be resolved before VexLifeTheme projection",
            )
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

/**
 * Smallest current ThemeMode Family earned by the Debug Menu patient zero.
 *
 * Toggle meaning depends on CURRENT semantic State, so it belongs in the accepted direct Family
 * circuit rather than in a Boolean callback. Control kind alone is sufficient relationship identity:
 * ThemeMode has no repeated resource whose identity would earn a ResourceId.
 *
 * NotSet deliberately has no Toggle Act. A future platform/system-appearance relationship must first
 * resolve effective ThemeMode through its rightful owner instead of this Family inventing a default.
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
        data object Toggle :
            Control
    }

    override fun act(
        currentState: ThemeMode,
        control: Control,
    ):
        VexFamily.Act<ThemeMode>? =
        when (currentState) {
            ThemeMode.NotSet ->
                when (control) {
                    Control.Toggle ->
                        noAct()
                }

            ThemeMode.Light ->
                when (control) {
                    Control.Toggle ->
                        admit(
                            ThemeMode.Dark,
                        )
                }

            ThemeMode.Dark ->
                when (control) {
                    Control.Toggle ->
                        admit(
                            ThemeMode.Light,
                        )
                }
        }
}
