package com.vextreme.vexlife.android.behavior

/**
 * Authoring receiver for begin(...).
 *
 * This file contains only the already-existing execution-result DSL. It is not involved in
 * Family.activate(...) -> Family.act(...) State/Control selection.
 */
class VexBeginScope<
    State : Any,
> internal constructor(
    private val mode:
        VexBeginMode,
) {
    private var operation:
        VexOperation<State>? =
        null

    fun save(
        state: State,
    ):
        VexSaveDestination<State> {
        require(
            operation ==
                null,
        ) {
            "begin currently admits one Operation"
        }

        return VexSaveDestination(
            state =
                state,
            complete = { completed ->
                require(
                    operation ==
                        null,
                ) {
                    "begin Operation was already completed"
                }

                operation =
                    completed
            },
        )
    }

    internal fun build():
        VexBegin<State> =
        VexBegin(
            mode =
                mode,
            operation =
                requireNotNull(
                    operation,
                ) {
                    "begin requires one Operation; Save(...).results { ... } is the current patient zero"
                },
        )
}

class VexSaveDestination<
    State : Any,
> internal constructor(
    private val state:
        State,
    private val complete:
        (
            VexSave<State>,
        ) -> Unit,
) {
    fun results(
        block:
            VexResultsScope<State>.() -> Unit,
    ) {
        complete(
            VexSave(
                state =
                    state,
                results =
                    VexResultsScope<State>()
                        .apply(
                            block,
                        )
                        .build(),
            ),
        )
    }
}

class VexResultsScope<
    State : Any,
> internal constructor() {
    private var success:
        VexResult<State>? =
        null

    private var failure:
        VexResult<State>? =
        null

    fun success(
        block:
            VexResultScope<State>.() -> Unit = {},
    ) {
        require(
            success ==
                null,
        ) {
            "Success result was declared more than once"
        }

        success =
            VexResultScope<State>()
                .apply(
                    block,
                )
                .build()
    }

    fun failure(
        block:
            VexResultScope<State>.() -> Unit = {},
    ) {
        require(
            failure ==
                null,
        ) {
            "Failure result was declared more than once"
        }

        failure =
            VexResultScope<State>()
                .apply(
                    block,
                )
                .build()
    }

    internal fun build():
        VexResults<State> =
        VexResults(
            success =
                requireNotNull(
                    success,
                ) {
                    "results requires explicit Success"
                },
            failure =
                requireNotNull(
                    failure,
                ) {
                    "results requires explicit Failure"
                },
        )
}

class VexResultScope<
    State : Any,
> internal constructor() {
    private var admittedState:
        State? =
        null

    fun admit(
        state: State,
    ) {
        require(
            admittedState ==
                null,
        ) {
            "one result may admit at most one later State"
        }

        admittedState =
            state
    }

    internal fun build():
        VexResult<State> =
        VexResult(
            admittedState =
                admittedState,
        )
}

// [VXG RealForever]
