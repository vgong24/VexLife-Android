package com.vextreme.vexlife.android.behavior

/**
 * Complete finite Control vocabulary bound to one exact Behavior family and blueprint.
 */
class VexBehaviorControlBlueprint<
    Family : VexBehaviorFamily<Family>,
    State : VexBehaviorFamily.State<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Effect : VexBehaviorFamily.Effect<Family>,
    Control,
> internal constructor(
    val behavior:
        VexBehaviorBlueprint<
            Family,
            State,
            Act,
            Effect
        >,
    private val actPriorityByControl:
        Map<
            Control,
            List<Act>
        >,
)
    where Control : Enum<Control>,
          Control : VexBehaviorFamily.Control<Family> {
    fun resolve(
        current: State,
        control: Control,
    ): VexBehaviorControlResolution<
        Family,
        Control,
        Act
    >? {
        val available =
            behavior.availableActs(
                current,
            ).toSet()

        val resolved =
            actPriorityByControl
                .getValue(
                    control,
                )
                .firstOrNull { candidate ->
                    candidate in available
                }
                ?: return null

        return VexBehaviorControlResolution(
            control = control,
            act = resolved,
        )
    }

    fun projection(
        current: State,
    ): VexBehaviorProjection<
        Family,
        State,
        Control,
        Act
    > =
        VexBehaviorProjection(
            state = current,
            controls =
                actPriorityByControl.keys
                    .associateWith { control ->
                        val resolved =
                            resolve(
                                current = current,
                                control = control,
                            )

                        VexBehaviorControlProjection(
                            control = control,
                            meaning =
                                if (
                                    resolved != null
                                ) {
                                    VexBehaviorControlMeaning.Available(
                                        resolved.act,
                                    )
                                } else {
                                    VexBehaviorControlMeaning.Unavailable
                                },
                        )
                    },
        )
}

data class VexBehaviorControlResolution<
    Family : VexBehaviorFamily<Family>,
    Control : VexBehaviorFamily.Control<Family>,
    Act : VexBehaviorFamily.Act<Family>,
>(
    val control: Control,
    val act: Act,
)

class VexBehaviorControlBlueprintBuilder<
    Family : VexBehaviorFamily<Family>,
    Act : VexBehaviorFamily.Act<Family>,
    Control,
> @PublishedApi internal constructor(
    private val expectedControls:
        Set<Control>,
)
    where Control : Enum<Control>,
          Control : VexBehaviorFamily.Control<Family> {
    private val actPriorityByControl =
        linkedMapOf<
            Control,
            List<Act>
        >()

    fun control(
        control: Control,
        vararg actPriority: Act,
    ) {
        require(
            control in expectedControls,
        ) {
            "control is not part of the admitted finite Control vocabulary: " +
                control
        }
        require(
            actPriority.isNotEmpty(),
        ) {
            "behavior control requires at least one Act: " +
                control
        }
        require(
            actPriority.distinct().size ==
                actPriority.size,
        ) {
            "behavior control Act priority must not contain duplicates: " +
                control
        }
        require(
            actPriorityByControl.put(
                control,
                actPriority.toList(),
            ) == null,
        ) {
            "behavior control was declared more than once: " +
                control
        }
    }

    @PublishedApi
    internal fun <
        State : VexBehaviorFamily.State<Family>,
        Effect : VexBehaviorFamily.Effect<Family>,
    > build(
        behavior:
            VexBehaviorBlueprint<
                Family,
                State,
                Act,
                Effect
            >,
    ): VexBehaviorControlBlueprint<
        Family,
        State,
        Act,
        Effect,
        Control
    > {
        val missingControls =
            expectedControls -
                actPriorityByControl.keys

        require(
            missingControls.isEmpty(),
        ) {
            "behavior Control vocabulary is incomplete; missing: " +
                missingControls.joinToString()
        }

        val admittedActs =
            behavior.relationships
                .map {
                    it.act
                }
                .toSet()

        actPriorityByControl.forEach {
            (
                control,
                acts,
            ) ->
            require(
                acts.all {
                    it in admittedActs
                },
            ) {
                "control " +
                    control +
                    " references an Act not admitted by behavior " +
                    behavior.behaviorRef
            }
        }

        return VexBehaviorControlBlueprint(
            behavior = behavior,
            actPriorityByControl =
                actPriorityByControl
                    .mapValues {
                        (
                            _,
                            acts,
                        ) ->
                        acts.toList()
                    },
        )
    }
}
