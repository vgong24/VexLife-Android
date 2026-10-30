@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.ui

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot

/**
 * Runtime/reality boundary for ThemeMode.
 *
 * ThemeModeFamily owns semantic State / Control / Act / Effect meaning.
 * This Connection owns only Effect realization and the runtime dependencies needed for it.
 */
internal class ThemeModeConnection(
    private val store:
        ThemeModeStore? =
        null,
    private val executionRoot:
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

    /*
     * One private persistence queue for this live ThemeMode Connection.
     *
     * PersistSelection effects carry whole durable ThemeMode truth, so accepted persistence requests
     * must be realized in semantic acceptance order.
     */
    private val persistenceRelationshipId:
        VexExecution.RelationshipId =
        object :
            VexExecution.RelationshipId {}

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
            is ThemeModeFamily.Effect.PersistSelection -> {
                val targetStore =
                    store
                        ?: return

                VexExecution.beginQueued(
                    executionRoot =
                        requireNotNull(
                            executionRoot,
                        ),
                    owner =
                        this::class,
                    relationshipId =
                        persistenceRelationshipId,
                    operation =
                        targetStore::save,
                    argument =
                        effect.mode,
                )
            }
        }
    }
}

// [VXG RealForever]
