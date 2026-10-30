package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot

/**
 * Runtime/reality boundary for ThemeMode.
 *
 * ThemeModeFamily owns semantic State / Control / Act / Effect meaning.
 * This Connection owns Effect realization and the runtime dependencies needed for it.
 */
internal class ThemeModeConnection(
    private val store:
        ThemeModeStore? =
        null,
    executionRoot:
        VexExecutionRoot? =
        null,
    initialState:
        ThemeMode =
        store
            ?.current()
            ?: ThemeMode.Dark,
) {
    init {
        require(
            store ==
                null ||
                executionRoot !=
                null,
        ) {
            "ThemeMode persistence requires an executionRoot"
        }
    }

    val family =
        ThemeModeFamily(
            initialState =
                initialState,
        )

    private val persistenceExecution =
        if (
            store !=
            null
        ) {
            PersistenceExecution(
                executionRoot =
                    requireNotNull(
                        executionRoot,
                    ),
                store =
                    store,
            )
        } else {
            null
        }

    fun activate(
        control:
            ThemeModeFamily.Control,
    ):
        Boolean {
        val act =
            family.activate(
                control,
            )
                ?: return false

        act.effect
            ?.let(
                ::realize,
            )

        return true
    }

    private fun realize(
        effect:
            ThemeModeFamily.Effect,
    ) {
        when (effect) {
            is ThemeModeFamily.Effect.PersistSelection ->
                persistenceExecution
                    ?.persist(
                        effect.mode,
                    )
        }
    }

    /**
     * ThemeMode-specific execution mechanics.
     *
     * The CoordinationKey is private runtime identity. It is not Family/Control/Effect identity.
     */
    private class PersistenceExecution(
        executionRoot:
            VexExecutionRoot,
        private val store:
            ThemeModeStore,
    ) :
        VexExecution(
            executionRoot,
        ) {
        private data object Persistence :
            CoordinationKey

        fun persist(
            mode:
                ThemeMode,
        ):
            Handle =
            launchQueued(
                key =
                    Persistence,
                action =
                    store::save,
                argument =
                    mode,
            )
    }
}

// [VXG RealForever]
