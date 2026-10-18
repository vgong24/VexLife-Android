package com.vextreme.vexlife.android.behavior

/**
 * Semantic meaning of one Control from current State.
 *
 * Availability is modeled as meaning first, not as a Boolean. A Boolean is only a convenience read
 * of this semantic result.
 */
sealed interface VexBehaviorControlMeaning<
    out Act : Any,
> {
    fun <
        Result,
    > fold(
        onAvailable:
            (Act) -> Result,
        onUnavailable:
            () -> Result,
    ): Result

    data class Available<
        Act : Any,
    >(
        val act: Act,
    ) :
        VexBehaviorControlMeaning<
            Act
        > {
        override fun <
            Result,
        > fold(
            onAvailable:
                (Act) -> Result,
            onUnavailable:
                () -> Result,
        ): Result =
            onAvailable(
                act,
            )
    }

    data object Unavailable :
        VexBehaviorControlMeaning<
            Nothing
        > {
        override fun <
            Result,
        > fold(
            onAvailable:
                (Nothing) -> Result,
            onUnavailable:
                () -> Result,
        ): Result =
            onUnavailable()
    }
}

/**
 * Current semantic projection of one Control from one exact Behavior family.
 */
data class VexBehaviorControlProjection<
    Family : VexBehaviorFamily<Family>,
    Control : VexBehaviorFamily.Control<Family>,
    Act : VexBehaviorFamily.Act<Family>,
>(
    val control: Control,
    val meaning:
        VexBehaviorControlMeaning<
            Act
        >,
) {
    /**
     * Convenience for callers that need only the current Act.
     *
     * Prefer [meaning] when the distinction between Available and Unavailable matters explicitly.
     */
    val act: Act?
        get() =
            meaning.fold(
                onAvailable = {
                    it
                },
                onUnavailable = {
                    null
                },
            )

    val isAvailable: Boolean
        get() =
            meaning.fold(
                onAvailable = {
                    true
                },
                onUnavailable = {
                    false
                },
            )

    /**
     * Compatibility convenience. Prefer [isAvailable] for Boolean-only reads or [meaning] for
     * semantic reads.
     */
    @Deprecated(
        message =
            "Control availability is semantic meaning; prefer meaning or isAvailable",
        replaceWith =
            ReplaceWith(
                "isAvailable",
            ),
    )
    val available: Boolean
        get() =
            isAvailable
}

/**
 * Immutable UI/convergence-facing result of one exact Behavior family.
 *
 * Family is a compile-time coordinate only. It prevents a State, Control and Act from unrelated
 * families from being assembled into one apparently valid Projection.
 */
data class VexBehaviorProjection<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Control : VexBehaviorFamily.Control<Family>,
    Act : VexBehaviorFamily.Act<Family>,
>(
    val state: State,
    val controls:
        Map<
            Control,
            VexBehaviorControlProjection<
                Family,
                Control,
                Act
            >
        >,
) {
    operator fun get(
        control: Control,
    ): VexBehaviorControlProjection<
        Family,
        Control,
        Act
    > =
        controls.getValue(
            control,
        )
}
