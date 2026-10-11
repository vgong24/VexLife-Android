package com.vextreme.vexlife.android.runtime

import com.vextreme.vexlife.android.elements.ActionElement
import com.vextreme.vexlife.android.elements.ActionKey
import com.vextreme.vexlife.android.elements.ElementIntent

/**
 * One scoped owner for one semantic Invoke action.
 *
 * Feature code keeps these beside the Screen/domain that owns the effect. [AppActionRegistrations]
 * then assembles them into one immutable, queryable registration set.
 */
data class AppActionRegistration(
    val action: ActionKey,
    val ownerRef: String,
    val handle: (AppActionRequest) -> Unit,
)

/**
 * Immutable application-action registration set.
 *
 * This owns the collection/index/duplicate validation once so callers do not repeatedly group/filter
 * raw Lists. It is also inspectable by either ActionKey or ActionElement:
 *
 * registrations[AppActions.SaveUserProfile]
 * registrations[UserProfileScreen.Save]
 *
 * Element lookup follows the element's existing Invoke/Sequence intent; it does not create a second
 * element->action map.
 *
 * IMPORTANT:
 * REGISTRATION_EXISTS != ACTION_IS_CURRENTLY_AVAILABLE_OR_AUTHORIZED.
 * Registration answers wiring/ownership only. Future Relay/capability state must own whether an
 * action is currently usable.
 *
 * This is not an accumulating listener collection. Instances are immutable snapshots.
 */
class AppActionRegistrations private constructor(
    registrations: List<AppActionRegistration>,
) {
    val all: List<AppActionRegistration> = registrations.toList()

    private val byActionId: Map<String, AppActionRegistration>

    init {
        val duplicates = all
            .groupBy { it.action.actionId }
            .filterValues { it.size > 1 }

        check(duplicates.isEmpty()) {
            duplicates.entries.joinToString(
                prefix = "duplicate app action ownership: ",
                separator = "; ",
            ) { (actionId, owners) ->
                "$actionId -> " + owners.joinToString { it.ownerRef }
            }
        }

        byActionId = all.associateBy { it.action.actionId }
    }

    operator fun get(action: ActionKey): AppActionRegistration? =
        byActionId[action.actionId]

    /**
     * Returns registrations referenced by this element's Invoke/Sequence intent in declaration order.
     *
     * Navigate/Open/Close/Toggle/Swap actions are runtime-owned and therefore return no app-action
     * registrations.
     */
    operator fun get(element: ActionElement): List<AppActionRegistration> =
        actionKeysFor(element)
            .mapNotNull { action ->
                this[action]
            }

    fun require(action: ActionKey): AppActionRegistration =
        checkNotNull(this[action]) {
            "no app action owner registered for ${action.actionId}"
        }

    fun actionKeysFor(element: ActionElement): List<ActionKey> =
        invokedActionKeys(element.intent)
            .distinctBy { it.actionId }

    /**
     * Fails construction when registered Screen elements declare Invoke actions with no owner.
     *
     * This lets AppScreens discover feature registrations from the Screen list instead of keeping a
     * second hand-maintained feature list in VexLifeSession.
     */
    fun requireCoverage(
        elements: Iterable<ActionElement>,
    ): AppActionRegistrations {
        val missing = elements
            .flatMap(::actionKeysFor)
            .distinctBy { it.actionId }
            .filter { action -> this[action] == null }

        check(missing.isEmpty()) {
            "missing app action ownership for registered Screen actions: " +
                missing.joinToString { it.actionId }
        }

        return this
    }

    fun dispatch(request: AppActionRequest) {
        require(request.action).handle(request)
    }

    operator fun plus(other: AppActionRegistrations): AppActionRegistrations =
        from(all + other.all)

    companion object {
        val Empty: AppActionRegistrations =
            AppActionRegistrations(emptyList())

        fun of(
            vararg registrations: AppActionRegistration,
        ): AppActionRegistrations =
            AppActionRegistrations(registrations.toList())

        fun from(
            registrations: Iterable<AppActionRegistration>,
        ): AppActionRegistrations =
            AppActionRegistrations(registrations.toList())

        fun combine(
            vararg groups: AppActionRegistrations,
        ): AppActionRegistrations =
            from(groups.flatMap { it.all })
    }
}

private fun invokedActionKeys(intent: ElementIntent): List<ActionKey> =
    when (intent) {
        is ElementIntent.Invoke ->
            listOf(intent.action)

        is ElementIntent.Sequence ->
            intent.intents.flatMap(::invokedActionKeys)

        else ->
            emptyList()
    }

fun appAction(
    action: ActionKey,
    ownerRef: String,
    handle: (AppActionRequest) -> Unit,
): AppActionRegistration =
    AppActionRegistration(
        action = action,
        ownerRef = ownerRef,
        handle = handle,
    )
