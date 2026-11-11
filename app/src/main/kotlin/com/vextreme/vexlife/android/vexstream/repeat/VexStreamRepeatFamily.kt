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

    data class DisplayState(
        val toggleAvailable:
            Boolean,
    ) :
        VexFamily.DisplayState<
            VexStreamRepeatFamily,
            VexStreamRepeatState
        > {
        internal fun isAvailable(
            control:
                VexStreamRepeatControl,
        ):
            Boolean =
            when (
                control
            ) {
                VexStreamRepeatControl.Toggle ->
                    toggleAvailable
            }
    }

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

    override fun displayStateFor(
        state:
            VexStreamRepeatState,
    ):
        DisplayState =
        when (
            state
        ) {
            is VexStreamRepeatState.Disabled ->
                DisplayState(
                    toggleAvailable =
                        state.toggleAvailable,
                )

            is VexStreamRepeatState.Enabled ->
                DisplayState(
                    toggleAvailable =
                        state.toggleAvailable,
                )
        }

    override fun projectionFor(
        state:
            VexStreamRepeatState,
    ):
        VexStreamRepeatProjection =
        VexFamily.Projection(
            state =
                state,
            displayState =
                displayStateFor(
                    state,
                ),
        )

    override val currentProjection:
        VexStreamRepeatProjection
        get() =
            projectionFor(
                currentState,
            )

    override val displayState:
        DisplayState
        get() =
            currentProjection
                .displayState

    object Act {
        class Method(
            private val connection:
                VexStreamPlayerPartner.Connection.Request,
            override val effect:
                VexStreamRepeatEffect,
        ) :
            VexFamily.Act.Method<
                VexStreamRepeatFamily,
                Unit,
                Unit
            > {
            override suspend fun invoke():
                VexMethod.Result<
                    Unit,
                    Unit
                > {
                val dispatched =
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

                return if (
                    dispatched
                ) {
                    VexMethod.Result.Success(
                        Unit,
                    )
                } else {
                    VexMethod.Result.Failure(
                        Unit,
                    )
                }
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
    val begin =
        act.realizationBegin
            ?: return null

    return act.launch(
        executionRoot =
            executionRoot,
        method =
            VexStreamRepeatFamily.Act.Method(
                connection =
                    connection,
                effect =
                    begin.effect,
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
    family.observeStore(
        store.currentRepeatSignal(),
    )

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

private fun VexStreamPlayerStore.currentRepeatSignal():
    VexStreamPlayerPartner.Signal.Repeat? =
    (
        current as?
            VexStreamPlayerStore.Snapshot.Available
    )
        ?.repeat

// [VXG RealForever][VEXSTREAM][REPEAT][DIRECT-FAMILY]
