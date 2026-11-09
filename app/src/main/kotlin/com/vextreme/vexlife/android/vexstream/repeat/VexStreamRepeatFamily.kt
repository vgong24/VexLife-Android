package com.vextreme.vexlife.android.vexstream.repeat

import com.vextreme.vexlife.android.behavior.VexFamily
import com.vextreme.vexlife.android.behavior.VexMethod
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerPartner
import com.vextreme.vexlife.android.vexstream.player.VexStreamPlayerStore
import kotlinx.coroutines.Job

/** Semantic owner for VexStream Repeat meaning. */
internal class VexStreamRepeatFamily(
    initialState:
        VexStreamRepeatState =
        VexStreamRepeatState.Disabled(
            toggleAvailable =
                false,
        ),
) :
    VexFamily<
        VexStreamRepeatFamily,
        VexStreamRepeatState,
        VexStreamRepeatControl,
        VexStreamRepeatEffect
    >(
        initialState =
            initialState,
    ) {

    override fun act(
        currentState:
            VexStreamRepeatState,
        control:
            VexStreamRepeatControl,
    ):
        VexFamily.Act<
            VexStreamRepeatFamily,
            VexStreamRepeatState,
            VexStreamRepeatEffect
        >? =
        when (
            control
        ) {
            VexStreamRepeatControl.Toggle ->
                when (
                    currentState
                ) {
                    is VexStreamRepeatState.Disabled ->
                        VexStreamRepeatEffect.RequestEnableAll
                            .takeIf {
                                currentState.toggleAvailable
                            }

                    is VexStreamRepeatState.Enabled ->
                        VexStreamRepeatEffect.RequestDisable
                            .takeIf {
                                currentState.toggleAvailable
                            }
                }
                    ?.let { effect ->
                        stay(
                            beginLatest(
                                effect,
                            ),
                        )
                    }
        }

    internal fun observeStore(
        repeat:
            VexStreamPlayerPartner.Signal.Repeat?,
    ):
        VexStreamRepeatState =
        observe(
            repeat
                .toVexStreamRepeatState(),
        )

    fun projectionFor(
        state:
            VexStreamRepeatState,
    ):
        VexStreamRepeatProjection =
        VexStreamRepeatProjection(
            state =
                state,
            toggleAvailable =
                when (
                    state
                ) {
                    is VexStreamRepeatState.Disabled ->
                        state.toggleAvailable

                    is VexStreamRepeatState.Enabled ->
                        state.toggleAvailable
                },
        )

    val currentRepeatProjection:
        VexStreamRepeatProjection
        get() =
            projectionFor(
                currentState,
            )

    object Act {
        class Method(
            private val connection:
                VexStreamPlayerPartner.Connection.Request,
            private val effect:
                VexStreamRepeatEffect,
        ) :
            VexFamily.Act.Method<
                VexStreamRepeatFamily,
                Unit,
                Nothing
            > {
            override suspend fun invoke():
                VexMethod.Result<
                    Unit,
                    Nothing
                > {
                when (
                    effect
                ) {
                    VexStreamRepeatEffect.RequestEnableAll ->
                        connection.setRepeatEnabled(
                            true,
                        )

                    VexStreamRepeatEffect.RequestDisable ->
                        connection.setRepeatEnabled(
                            false,
                        )
                }

                return VexMethod.Result.Success(
                    Unit,
                )
            }
        }
    }
}

internal fun activateVexStreamRepeat(
    family:
        VexStreamRepeatFamily,
    control:
        VexStreamRepeatControl,
    connection:
        VexStreamPlayerPartner.Connection.Request,
    executionRoot:
        VexExecutionRoot,
):
    Job? {
    val act =
        family.activate(
            control,
        )
            ?: return null
    val effect =
        act.effect
            ?: return null
    val execution =
        act.launchWith(
            executionRoot,
        )
            ?: return null

    return execution.method(
        VexStreamRepeatFamily.Act.Method(
            connection =
                connection,
            effect =
                effect,
        ),
    )
}

internal fun observeVexStreamRepeatPlayerStore(
    family:
        VexStreamRepeatFamily,
    store:
        VexStreamPlayerStore,
    executionRoot:
        VexExecutionRoot,
) {
    VexExecution.observe(
        executionRoot =
            executionRoot,
        owner =
            family,
        source =
            store::repeatSignals,
    ) { repeat ->
        family.observeStore(
            repeat,
        )
    }
}

internal fun VexStreamPlayerPartner.Signal.Repeat?.toVexStreamRepeatState():
    VexStreamRepeatState =
    if (
        this?.enabled ==
        true
    ) {
        VexStreamRepeatState.Enabled(
            toggleAvailable =
                toggleAvailable,
        )
    } else {
        VexStreamRepeatState.Disabled(
            toggleAvailable =
                this?.toggleAvailable ==
                    true,
        )
    }

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
