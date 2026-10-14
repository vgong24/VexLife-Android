package com.vextreme.vexlife.android.behavior

/**
 * One finite Control vocabulary bound to one exact Behavior blueprint instance.
 *
 * Control is an enum deliberately: these are identity-only human affordances whose complete range
 * should be visible to the compiler/IDE. State/Act/Effect may remain sealed families because those
 * families can later carry data.
 *
 * The Control blueprint owns only:
 *
 * Control -> prioritized Acts it can represent
 *
 * The Behavior graph still owns:
 *
 * State -> available Acts -> Effects -> expected next State
 *
 * Binding both through the same [Act] type prevents cross-domain Controls from silently accepting a
 * different feature's Act family.
 */
class VexBehaviorControlBlueprint<
    State : Any,
    Act : Any,
    Effect : Any,
    Control,
> internal constructor(
    val behavior: VexBehaviorBlueprint<State, Act, Effect>,
    private val actPriorityByControl: Map<Control, List<Act>>,
) where Control : Enum<Control> {
    /**
     * Resolve what one finite Control means from current State.
     *
     * Null means none of that Control's candidate Acts are currently available. Presentation owns
     * whether null appears hidden, disabled, replaced, or otherwise rendered.
     */
    fun resolve(
        current: State,
        control: Control,
    ): VexBehaviorControlResolution<Control, Act>? {
        val available =
            behavior.availableActs(current)
                .toSet()

        val resolved =
            actPriorityByControl
                .getValue(control)
                .firstOrNull { candidate ->
                    candidate in available
                }
                ?: return null

        return VexBehaviorControlResolution(
            control = control,
            act = resolved,
        )
    }

    /**
     * Complete immutable semantic projection for one observed State.
     *
     * This is the common value boundary for UI, tests, previews, or later convergence. Effects stay
     * behind invocation/runtime ownership and are intentionally absent here.
     */
    fun projection(
        current: State,
    ): VexBehaviorProjection<State, Control, Act> =
        VexBehaviorProjection(
            state = current,
            controls =
                actPriorityByControl.keys
                    .associateWith { control ->
                        VexBehaviorControlProjection(
                            control = control,
                            act =
                                resolve(
                                    current = current,
                                    control = control,
                                )?.act,
                        )
                    },
        )
}

/** Current semantic meaning of one finite Control. */
data class VexBehaviorControlResolution<
    Control : Any,
    Act : Any,
>(
    val control: Control,
    val act: Act,
)

/**
 * Builder for one finite Control vocabulary.
 *
 * The builder rejects:
 * - undeclared enum members;
 * - duplicate Control declarations;
 * - empty Act ranges;
 * - duplicate Act priority entries;
 * - Acts that do not occur anywhere in the bound Behavior graph.
 */
class VexBehaviorControlBlueprintBuilder<
    Act : Any,
    Control,
> @PublishedApi internal constructor(
    private val expectedControls: Set<Control>,
) where Control : Enum<Control> {
    private val actPriorityByControl =
        linkedMapOf<Control, List<Act>>()

    /**
     * Bind one Control to the Acts it can represent.
     *
     * Argument order is explicit conflict priority when several candidate Acts are simultaneously
     * available from current State.
     */
    fun control(
        control: Control,
        vararg actPriority: Act,
    ) {
        require(control in expectedControls) {
            "control is not part of the admitted finite Control vocabulary: " + control
        }
        require(actPriority.isNotEmpty()) {
            "behavior control requires at least one Act: " + control
        }
        require(actPriority.distinct().size == actPriority.size) {
            "behavior control Act priority must not contain duplicates: " + control
        }
        require(
            actPriorityByControl.put(
                control,
                actPriority.toList(),
            ) == null,
        ) {
            "behavior control was declared more than once: " + control
        }
    }

    @PublishedApi
    internal fun <
        State : Any,
        Effect : Any,
    > build(
        behavior: VexBehaviorBlueprint<State, Act, Effect>,
    ): VexBehaviorControlBlueprint<State, Act, Effect, Control> {
        val missingControls =
            expectedControls - actPriorityByControl.keys

        require(missingControls.isEmpty()) {
            "behavior Control vocabulary is incomplete; missing: " +
                missingControls.joinToString()
        }

        val admittedActs =
            behavior.relationships
                .map {
                    it.act
                }
                .toSet()

        actPriorityByControl.forEach { (control, acts) ->
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
                actPriorityByControl.mapValues { (_, acts) ->
                    acts.toList()
                },
        )
    }
}
