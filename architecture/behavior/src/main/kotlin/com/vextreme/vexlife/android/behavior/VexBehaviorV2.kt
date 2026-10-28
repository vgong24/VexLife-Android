package com.vextreme.vexlife.android.behavior

import kotlin.reflect.KClass

/**
 * Required UI-facing role for a live V2 Family.
 *
 * DisplayState is presentation-facing output, not another semantic truth owner. Every live V2 Family
 * exposes [VexBehaviorV2Family.displayState]. The foundation supplies [Empty] by default so the
 * ownership surface is always visible even when a Family has not earned richer display categories.
 *
 * A Family that needs richer categories should declare its own nested DisplayState vocabulary and
 * override [VexBehaviorV2Family.displayState], while preserving the underlying current semantic State.
 */
interface VexBehaviorV2DisplayState<
    out State,
> {
    val state: State

    data class Empty<
        State,
    >(
        override val state: State,
    ) :
        VexBehaviorV2DisplayState<
            State
        >
}

/**
 * V2 authored Act graph.
 *
 * The public Family entrance is [VexBehaviorV2Family.activate]. This object remains the inspectable
 * graph/matching machinery underneath that entrance; callers should not need to provide CURRENT State
 * themselves.
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

            act.immediateDisposition
                .admittedState
                ?.let { admitted ->
                    require(
                        admitted::class in
                            states,
                    ) {
                        "Act immediate State is not registered: " +
                            admitted::class
                    }
                }

            act.effect
                ?.let { effect ->
                    require(
                        effect
                            .save
                            .state
                            .let {
                                it::class in states
                            },
                    ) {
                        "Save State is not registered in " +
                            behaviorRef
                    }

                    effect
                        .save
                        .results
                        .let { results ->
                            listOf(
                                results.success,
                                results.failure,
                            ).forEach { result ->
                                result.admittedState
                                    ?.let { laterState ->
                                        require(
                                            laterState::class in
                                                states,
                                        ) {
                                            "later admitted State is not registered: " +
                                                laterState::class
                                        }
                                    }
                            }
                        }
                }
        }
    }

    /**
     * Internal matching primitive used by the live Family entrance.
     *
     * A UI/runtime caller should activate the Family with a Control instead of supplying CURRENT State
     * to this matcher directly.
     */
    internal fun resolve(
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
                    ")",
                )

                val effect =
                    act.effect

                if (
                    effect ==
                    null
                ) {
                    append(
                        " / NO_EFFECT",
                    )
                } else {
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
                }

                append(
                    "--> ",
                )
                append(
                    act.immediateDisposition
                        .kind,
                )
                act.immediateDisposition
                    .admittedState
                    ?.let { admitted ->
                        append(
                            " ",
                        )
                        append(
                            admitted,
                        )
                    }

                effect
                    ?.save
                    ?.results
                    ?.let { results ->
                        append(
                            " ; Success=",
                        )
                        append(
                            results.success
                                .admittedState
                                ?.toString()
                                ?: "NO_STATE_CHANGE",
                        )
                        append(
                            " ; Failure=",
                        )
                        append(
                            results.failure
                                .admittedState
                                ?.toString()
                                ?: "NO_STATE_CHANGE",
                        )
                    }

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

/**
 * Author one required V2 Act graph.
 *
 * "acts" is the Family-facing noun. VexBehaviorV2 remains the internal graph type while this draft
 * proves the clearer Family-rooted authoring surface.
 */
fun <
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
> vexActs(
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

/**
 * Compatibility spelling retained while the V2 draft migrates to the required Family acts surface.
 */
@Deprecated(
    message =
        "V2 Family authors should expose required acts with vexActs(...).",
)
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
    vexActs(
        family =
            family,
        behaviorRef =
            behaviorRef,
        block =
            block,
    )

/**
 * Live V2 Family foundation.
 *
 * The Family owns one CURRENT semantic State, one required Act graph, one always-visible DisplayState
 * field, and one common [activate] entrance. UI callers provide only Control; CURRENT State is read
 * here so rendered/stale UI State cannot become activation authority.
 *
 * [activate] applies the Act's immediate stay/admit consequence and returns the selected Act. It does
 * not invent generic Effect execution. An Effect-bearing provider/Store/third-party boundary is the
 * next Origin pressure and can consume the already-authored Act without creating another semantic
 * story.
 */
abstract class VexBehaviorV2Family<
    Family : VexBehaviorV2Family<
        Family,
        State,
        Control
    >,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
>(
    initialState: State,
) :
    VexBehaviorFamily<
        Family
    > {
    var current: State =
        initialState
        private set

    abstract val acts:
        VexBehaviorV2<
            Family,
            State,
            Control
        >

    open val displayState:
        VexBehaviorV2DisplayState<
            State
        >
        get() =
            VexBehaviorV2DisplayState.Empty(
                current,
            )

    fun activate(
        control: Control,
    ):
        VexBehaviorV2Act<
            Family,
            State,
            Control
        >? {
        check(
            acts.family ===
                this,
        ) {
            "acts must belong to this live Family instance"
        }

        val act =
            acts.resolve(
                current =
                    current,
                control =
                    control,
            )
                ?: return null

        when (
            act.immediateDisposition
                .kind
        ) {
            VexBehaviorV2StateDispositionKind.Stay ->
                Unit

            VexBehaviorV2StateDispositionKind.Admit ->
                current =
                    requireNotNull(
                        act.immediateDisposition
                            .admittedState,
                    )
        }

        return act
    }
}

// [VXG RealForever]
