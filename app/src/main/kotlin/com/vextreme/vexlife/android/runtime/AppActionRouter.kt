package com.vextreme.vexlife.android.runtime

import com.vextreme.vexlife.android.elements.ActionKey

/**
 * One scoped owner for one semantic app action.
 *
 * Feature code creates registrations beside the Screen that declares/triggers the action. The app
 * composition only assembles registrations; it does not re-describe every feature effect in one
 * giant when statement.
 */
data class AppActionRegistration(
    val action: ActionKey,
    val ownerRef: String,
    val handle: (AppActionRequest) -> Unit,
)

/**
 * Single delivery point for application actions with duplicate-owner protection.
 *
 * There are no broadcast listeners here. Each ActionKey may have exactly one registered owner.
 * Duplicate registration is a construction error, which prevents two Screens from accidentally
 * responding to the same Save/Reset/etc action.
 */
class AppActionRouter(
    registrations: List<AppActionRegistration>,
) {
    private val byActionId: Map<String, AppActionRegistration>

    init {
        val duplicates = registrations
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

        byActionId = registrations.associateBy { it.action.actionId }
    }

    /**
     * Dispatches to exactly one owner.
     *
     * An unregistered ActionKey is also a contract error: the Screen declared a semantic action but
     * composition forgot to provide its owner.
     */
    fun dispatch(request: AppActionRequest) {
        val registration = checkNotNull(byActionId[request.action.actionId]) {
            "no app action owner registered for ${request.action.actionId}"
        }
        registration.handle(request)
    }
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
