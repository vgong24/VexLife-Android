package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * V2 sibling proof of the Origin-Walk Family circuit.
 *
 * The POC originally copied V1's separate Family.Act vocabulary and therefore needed:
 *
 *     Control + State -> Act value
 *     State + Act value -> Effect
 *
 * ThemeMode made the duplication visible: the same State neighborhood was authored twice. The V2
 * removal test showed that no meaning was lost when that intermediate Act value disappeared.
 *
 * V2 therefore treats each authored on(Control) relationship as the Act:
 *
 *     CURRENT State + Control
 *     -> Act { Effect + immediate State consequence }
 *
 * This keeps Act as behavioral process/meaning while removing an extra type hierarchy and resolver
 * layer that fresh AI authors could otherwise recreate by habit.
 */
class VexBehaviorV2<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
> internal constructor(
    val family: Family,
    val behaviorRef: String,
    val states: Set<KClass<out State>>,
    val acts:
        List<
            VexBehaviorV2Act<
                Family,
                State,
                Control
            >
        >,
) {
    init {
        require(
            behaviorRef.isNotBlank(),
        ) {
            "behaviorRef must not be blank"
        }
        require(
            states.isNotEmpty(),
        ) {
            "behavior V2 requires at least one State"
        }

        acts.forEach { act ->
            require(
                act.from in
                    states,
            ) {
                "Act source is not a registered State: " +
                    act.from
            }
            require(
                act.immediateDisposition
                    .stateKind in
                    states,
            ) {
                "Act immediate State is not registered: " +
                    act.immediateDisposition
                        .stateKind
            }

            require(
                act.effect
                    .save
                    .state
                    .let {
                        it::class in states
                    },
            ) {
                "Save State is not registered in " +
                    behaviorRef
            }

            act.effect
                .save
                .results
                .let { results ->
                    listOf(
                        results.success,
                        results.failure,
                    ).forEach { result ->
                        result.admittedStateKind
                            ?.let { laterState ->
                                require(
                                    laterState in
                                        states,
                                ) {
                                    "later admitted State is not registered: " +
                                        laterState
                                }
                            }
                    }
                }
        }
    }

    /**
     * Resolve the one authored Act for this exact CURRENT State + Control input.
     *
     * The caller does not translate Control into another Family vocabulary first. Connection/UI
     * supplies the semantic Control, Behavior selects the matching Act, and the Act already carries
     * the Effect plus immediate State consequence.
     */
    fun resolve(
        current: State,
        control: Control,
    ): VexBehaviorV2Act<
        Family,
        State,
        Control
    >? {
        val available =
            acts.filter { act ->
                act.matches(
                    current =
                        current,
                    requestedControl =
                        control,
                )
            }

        require(
            available.size <=
                1,
        ) {
            "behavior " +
                behaviorRef +
                " has ambiguous Acts for current " +
                current::class.simpleName +
                " + Control " +
                control
        }

        return available.singleOrNull()
    }

    fun describe(): String =
        buildString {
            appendLine(
                behaviorRef +
                    " [V2]",
            )

            acts.forEach { act ->
                append(
                    act.from
                        .simpleName,
                )
                append(
                    " --on(",
                )
                append(
                    act.control,
                )
                append(
                    ") / begin[",
                )
                append(
                    act.effect
                        .mode,
                )
                append(
                    "] Save(",
                )
                append(
                    act.effect
                        .save
                        .state,
                )
                append(
                    ")--> ",
                )
                append(
                    act.immediateDisposition
                        .kind,
                )
                append(
                    " ",
                )
                append(
                    act.immediateDisposition
                        .stateKind
                        .simpleName,
                )

                val results =
                    act.effect
                        .save
                        .results

                append(
                    " ; Success=",
                )
                append(
                    results.success
                        .admittedStateKind
                        ?.simpleName
                        ?: "NO_STATE_CHANGE",
                )
                append(
                    " ; Failure=",
                )
                append(
                    results.failure
                        .admittedStateKind
                        ?.simpleName
                        ?: "NO_STATE_CHANGE",
                )

                act.conditionName
                    ?.let { name ->
                        append(
                            " when ",
                        )
                        append(
                            name,
                        )
                    }

                appendLine()
            }
        }.trimEnd()
}

fun <
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
> vexBehaviorV2(
    family: Family,
    behaviorRef: String,
    block:
        VexBehaviorV2Builder<
            Family,
            State,
            Control
        >.() -> Unit,
): VexBehaviorV2<
    Family,
    State,
    Control
> =
    VexBehaviorV2Builder<
        Family,
        State,
        Control
    >(
        behaviorRef =
            behaviorRef,
    )
        .apply(
            block,
        )
        .build(
            family,
        )

// [VXG RealForever]
