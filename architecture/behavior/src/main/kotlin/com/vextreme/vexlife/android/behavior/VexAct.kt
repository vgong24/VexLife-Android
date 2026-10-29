package com.vextreme.vexlife.android.behavior

enum class VexBeginMode {
    Replaceable,
    Sequenced,
}

/**
 * One authored State + Control outcome.
 *
 * This is the direct return value of Family.act(currentState, control). It carries only the semantic
 * State admission and optional execution consequence that Family.activate(...) needs to apply.
 */
class VexAct<
    State : Any,
> internal constructor(
    internal val admittedState:
        State?,
    internal val begin:
        VexBegin<State>?,
)

/**
 * Optional execution consequence authored by an Act.
 *
 * Begin currently owns one earned operation shape: Save. Save owns its explicit success/failure
 * consequences. These are nested relationships, not standalone Vex roots or a generic Operation
 * taxonomy waiting for hypothetical children.
 */
class VexBegin<
    State : Any,
> internal constructor(
    val mode:
        VexBeginMode,
    internal val save:
        Save<State>,
) {
    internal data class Result<
        State,
    >(
        val admittedState:
            State?,
    )

    internal data class Results<
        State,
    >(
        val success:
            Result<State>,
        val failure:
            Result<State>,
    )

    internal data class Save<
        State : Any,
    >(
        val state:
            State,
        val results:
            Results<State>,
    )

    class Scope<
        State : Any,
    > internal constructor(
        private val mode:
            VexBeginMode,
    ) {
        private var save:
            Save<State>? =
            null

        fun save(
            state: State,
        ):
            SaveDestination<State> {
            require(
                save ==
                    null,
            ) {
                "begin currently admits one Save"
            }

            return SaveDestination(
                state =
                    state,
                complete = { completed ->
                    require(
                        save ==
                            null,
                    ) {
                        "begin Save was already completed"
                    }

                    save =
                        completed
                },
            )
        }

        internal fun build():
            VexBegin<State> =
            VexBegin(
                mode =
                    mode,
                save =
                    requireNotNull(
                        save,
                    ) {
                        "begin requires one Save"
                    },
            )

        class SaveDestination<
            State : Any,
        > internal constructor(
            private val state:
                State,
            private val complete:
                (
                    Save<State>,
                ) -> Unit,
        ) {
            fun results(
                block:
                    ResultsScope<State>.() -> Unit,
            ) {
                complete(
                    Save(
                        state =
                            state,
                        results =
                            ResultsScope<State>()
                                .apply(
                                    block,
                                )
                                .build(),
                    ),
                )
            }
        }

        class ResultsScope<
            State : Any,
        > internal constructor() {
            private var success:
                Result<State>? =
                null

            private var failure:
                Result<State>? =
                null

            fun success(
                block:
                    ResultScope<State>.() -> Unit = {},
            ) {
                require(
                    success ==
                        null,
                ) {
                    "Success result was declared more than once"
                }

                success =
                    ResultScope<State>()
                        .apply(
                            block,
                        )
                        .build()
            }

            fun failure(
                block:
                    ResultScope<State>.() -> Unit = {},
            ) {
                require(
                    failure ==
                        null,
                ) {
                    "Failure result was declared more than once"
                }

                failure =
                    ResultScope<State>()
                        .apply(
                            block,
                        )
                        .build()
            }

            internal fun build():
                Results<State> =
                Results(
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

        class ResultScope<
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
                Result<State> =
                Result(
                    admittedState =
                        admittedState,
                )
        }
    }
}

// [VXG RealForever]
