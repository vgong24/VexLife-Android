package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * V2 construction scope that mirrors the current Origin-Walk ThemeMode circuit.
 */
class VexBehaviorV2Builder<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
> internal constructor(
    private val behaviorRef: String,
) {
    @PublishedApi
    internal val stateTypes =
        linkedSetOf<
            KClass<out State>
        >()

    @PublishedApi
    internal val authoredRelationships =
        mutableListOf<
            VexBehaviorV2Relationship<
                Family,
                State,
                Act
            >
        >()

    private val pendingRelationships =
        linkedMapOf<
            Int,
            String
        >()

    private var nextRelationshipId =
        0

    inline fun <
        reified Current : State,
    > state(
        block:
            VexBehaviorV2StateScope<
                Family,
                State,
                Act,
                Current
            >.() -> Unit = {},
    ) {
        stateTypes +=
            Current::class

        VexBehaviorV2StateScope<
            Family,
            State,
            Act,
            Current
        >(
            from =
                Current::class,
            beginRelationship =
                ::beginRelationship,
            completeRelationship =
                ::completeRelationship,
        ).apply(
            block,
        )
    }

    @PublishedApi
    internal fun beginRelationship(
        from: KClass<out State>,
        act: Act,
        effect:
            VexBehaviorV2Begin<
                Family,
                State
            >,
        conditionName: String?,
    ): Int {
        val id =
            nextRelationshipId++

        pendingRelationships[id] =
            buildString {
                append(
                    from.simpleName,
                )
                append(
                    " + ",
                )
                append(
                    act,
                )
                append(
                    " / begin[",
                )
                append(
                    effect.mode,
                )
                append(
                    "] Save(",
                )
                append(
                    effect
                        .save
                        .state,
                )
                append(
                    ")",
                )

                conditionName
                    ?.let { name ->
                        append(
                            " when ",
                        )
                        append(
                            name,
                        )
                    }
            }

        return id
    }

    @PublishedApi
    internal fun completeRelationship(
        id: Int,
        relationship:
            VexBehaviorV2Relationship<
                Family,
                State,
                Act
            >,
    ) {
        require(
            pendingRelationships.remove(
                id,
            ) !=
                null,
        ) {
            "behavior V2 relationship was already completed"
        }

        authoredRelationships +=
            relationship
    }

    internal fun build():
        VexBehaviorV2<
            Family,
            State,
            Act
        > {
        require(
            pendingRelationships
                .isEmpty(),
        ) {
            "behavior " +
                behaviorRef +
                " V2 has relationship(s) without a terminal .stay() or .admit<State>(): " +
                pendingRelationships
                    .values
                    .joinToString()
        }

        return VexBehaviorV2(
            behaviorRef =
                behaviorRef,
            states =
                stateTypes.toSet(),
            relationships =
                authoredRelationships
                    .toList(),
        )
    }
}

class VexBehaviorV2StateScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Current : State,
> @PublishedApi internal constructor(
    @PublishedApi
    internal val from:
        KClass<Current>,
    @PublishedApi
    internal val beginRelationship:
        (
            KClass<out State>,
            Act,
            VexBehaviorV2Begin<
                Family,
                State
            >,
            String?,
        ) -> Int,
    @PublishedApi
    internal val completeRelationship:
        (
            Int,
            VexBehaviorV2Relationship<
                Family,
                State,
                Act
            >,
        ) -> Unit,
) {
    fun on(
        act: Act,
        effect:
            VexBehaviorV2Begin<
                Family,
                State
            >,
        conditionName: String? = null,
        whenCurrent: (Current) -> Boolean = {
            true
        },
    ): VexBehaviorV2Destination<
        Family,
        State,
        Act,
        Current
    > {
        val relationshipId =
            beginRelationship(
                from,
                act,
                effect,
                conditionName,
            )

        return VexBehaviorV2Destination(
            relationshipId =
                relationshipId,
            from =
                from,
            act =
                act,
            effect =
                effect,
            conditionName =
                conditionName,
            whenCurrent =
                whenCurrent,
            completeRelationship =
                completeRelationship,
        )
    }

    fun begin(
        mode: VexBehaviorV2BeginMode,
        block:
            VexBehaviorV2BeginScope<
                Family,
                State
            >.() -> Unit,
    ): VexBehaviorV2Begin<
        Family,
        State
    > =
        VexBehaviorV2BeginScope<
            Family,
            State
        >(
            mode =
                mode,
        )
            .apply(
                block,
            )
            .build()
}

class VexBehaviorV2BeginScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor(
    private val mode: VexBehaviorV2BeginMode,
) {
    private var save:
        VexBehaviorV2Save<
            Family,
            State
        >? =
        null

    /**
     * Kotlin target form of Origin Save(Family, State).
     *
     * Family is already fixed by this scope, so callers provide only the State value.
     */
    fun save(
        state: State,
    ): VexBehaviorV2SaveDestination<
        Family,
        State
    > {
        require(
            save ==
                null,
        ) {
            "begin V2 currently admits one Save operation"
        }

        return VexBehaviorV2SaveDestination(
            state =
                state,
            complete = { completed ->
                require(
                    save ==
                        null,
                ) {
                    "Save was already completed"
                }

                save =
                    completed
            },
        )
    }

    internal fun build():
        VexBehaviorV2Begin<
            Family,
            State
        > =
        VexBehaviorV2Begin(
            mode =
                mode,
            save =
                requireNotNull(
                    save,
                ) {
                    "begin V2 requires Save(...).results { ... }"
                },
        )
}

class VexBehaviorV2SaveDestination<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor(
    private val state: State,
    private val complete:
        (
            VexBehaviorV2Save<
                Family,
                State
            >,
        ) -> Unit,
) {
    fun results(
        block:
            VexBehaviorV2ResultsScope<
                Family,
                State
            >.() -> Unit,
    ) {
        complete(
            VexBehaviorV2Save(
                state =
                    state,
                results =
                    VexBehaviorV2ResultsScope<
                        Family,
                        State
                    >()
                        .apply(
                            block,
                        )
                        .build(),
            ),
        )
    }
}

class VexBehaviorV2ResultsScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor() {
    private var success:
        VexBehaviorV2Result<
            Family,
            State
        >? =
        null

    private var failure:
        VexBehaviorV2Result<
            Family,
            State
        >? =
        null

    fun success(
        block:
            VexBehaviorV2ResultScope<
                Family,
                State
            >.() -> Unit = {},
    ) {
        require(
            success ==
                null,
        ) {
            "Success result was declared more than once"
        }

        success =
            VexBehaviorV2ResultScope<
                Family,
                State
            >()
                .apply(
                    block,
                )
                .build()
    }

    fun failure(
        block:
            VexBehaviorV2ResultScope<
                Family,
                State
            >.() -> Unit = {},
    ) {
        require(
            failure ==
                null,
        ) {
            "Failure result was declared more than once"
        }

        failure =
            VexBehaviorV2ResultScope<
                Family,
                State
            >()
                .apply(
                    block,
                )
                .build()
    }

    internal fun build():
        VexBehaviorV2Results<
            Family,
            State
        > =
        VexBehaviorV2Results(
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

class VexBehaviorV2ResultScope<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
> internal constructor() {
    @PublishedApi
    internal var admittedStateKind:
        KClass<out State>? =
        null

    inline fun <
        reified Next : State,
    > admit() {
        require(
            admittedStateKind ==
                null,
        ) {
            "one result may admit at most one later State"
        }

        admittedStateKind =
            Next::class
    }

    internal fun build():
        VexBehaviorV2Result<
            Family,
            State
        > =
        VexBehaviorV2Result(
            admittedStateKind =
                admittedStateKind,
        )
}

class VexBehaviorV2Destination<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Current : State,
> @PublishedApi internal constructor(
    @PublishedApi
    internal val relationshipId:
        Int,
    @PublishedApi
    internal val from:
        KClass<Current>,
    @PublishedApi
    internal val act:
        Act,
    @PublishedApi
    internal val effect:
        VexBehaviorV2Begin<
            Family,
            State
        >,
    @PublishedApi
    internal val conditionName:
        String?,
    @PublishedApi
    internal val whenCurrent:
        (Current) -> Boolean,
    @PublishedApi
    internal val completeRelationship:
        (
            Int,
            VexBehaviorV2Relationship<
                Family,
                State,
                Act
            >,
        ) -> Unit,
) {
    inline fun <
        reified Next : State,
    > admit() {
        complete(
            VexBehaviorV2StateDisposition(
                kind =
                    VexBehaviorV2StateDispositionKind
                        .Admit,
                stateKind =
                    Next::class,
            ),
        )
    }

    fun stay() {
        complete(
            VexBehaviorV2StateDisposition(
                kind =
                    VexBehaviorV2StateDispositionKind
                        .Stay,
                stateKind =
                    from,
            ),
        )
    }

    @PublishedApi
    internal fun complete(
        disposition:
            VexBehaviorV2StateDisposition<
                Family,
                State
            >,
    ) {
        completeRelationship(
            relationshipId,
            VexBehaviorV2Relationship(
                from =
                    from,
                act =
                    act,
                effect =
                    effect,
                immediateDisposition =
                    disposition,
                conditionName =
                    conditionName,
                whenCurrent = { current ->
                    @Suppress(
                        "UNCHECKED_CAST",
                    )
                    whenCurrent(
                        current as Current,
                    )
                },
            ),
        )
    }
}

// [VXG RealForever]
