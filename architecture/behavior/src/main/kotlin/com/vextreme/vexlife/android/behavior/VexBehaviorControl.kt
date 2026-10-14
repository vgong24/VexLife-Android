package com.vextreme.vexlife.android.behavior

/**
 * One human-facing control that can represent one of several semantic Acts.
 *
 * [actPriority] is intentionally explicit. If more than one candidate Act is currently available,
 * the first matching Act wins. This policy belongs to the Control, not to the Behavior graph and
 * not to Compose.
 *
 * Example:
 *
 * PlayPauseControl
 *     priority = [Play, Pause]
 *
 * From Playing, only Pause is available -> resolves Pause.
 * From FastForwarding, Play and Pause are both available -> resolves Play by explicit priority.
 */
class VexBehaviorControl<Act : Any> internal constructor(
    val controlRef: String,
    val actPriority: List<Act>,
) {
    init {
        require(controlRef.isNotBlank()) {
            "controlRef must not be blank"
        }
        require(actPriority.isNotEmpty()) {
            "behavior control requires at least one Act"
        }
        require(actPriority.distinct().size == actPriority.size) {
            "behavior control Act priority must not contain duplicates"
        }
    }

    /**
     * Resolve what this Control semantically means from the current Behavior State.
     *
     * Null means none of this Control's candidate Acts are currently available. Presentation may
     * hide, disable, replace, or otherwise render that unavailability; the Control does not decide
     * pixels.
     */
    fun <
        State : Any,
        Effect : Any,
    > resolve(
        blueprint: VexBehaviorBlueprint<State, Act, Effect>,
        current: State,
    ): VexBehaviorControlResolution<Act>? {
        val available =
            blueprint.availableActs(current)
                .toSet()

        val resolved =
            actPriority.firstOrNull { candidate ->
                candidate in available
            } ?: return null

        return VexBehaviorControlResolution(
            controlRef = controlRef,
            act = resolved,
        )
    }
}

/** Current semantic meaning of one Control. */
data class VexBehaviorControlResolution<Act : Any>(
    val controlRef: String,
    val act: Act,
)

/**
 * Construct one immutable Control whose argument order is also its explicit conflict priority.
 */
fun <Act : Any> vexBehaviorControl(
    controlRef: String,
    vararg actPriority: Act,
): VexBehaviorControl<Act> =
    VexBehaviorControl(
        controlRef = controlRef,
        actPriority = actPriority.toList(),
    )
