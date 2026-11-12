package com.vextreme.vexlife.android.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences

/**
 * Typed durable snapshot owned by one widget surface.
 *
 * This is not product/domain State and is not a SCAE State family. It is the smallest persisted
 * read model needed to reproduce one remote widget surface after process/session loss.
 *
 * Implementations should use structural value equality (normally a data class). The widget
 * foundation uses that equality for duplicate state-write suppression and active-session
 * reprojection. Separate foundation-owned update-request currentness coordinates distinguish whether
 * the current snapshot still owes a Glance update and which request occurrence may settle that debt.
 */
interface VexWidgetSurfaceState

/**
 * Feature-owned mapping between typed widget surface state and Glance Preferences storage.
 *
 * The foundation owns when state is read/written and reserves its own currentness keys. The feature
 * owns only the shape and persistence keys of its surface snapshot; codec writes must not treat the
 * shared Preferences object as feature-exclusive storage.
 */
interface VexWidgetPreferenceCodec<
    SurfaceState : VexWidgetSurfaceState,
> {
    fun read(
        preferences: Preferences,
    ): SurfaceState?

    fun write(
        preferences: MutablePreferences,
        state: SurfaceState,
    )
}

/**
 * Result of one bounded widget-state publication/transform fan-out.
 *
 * "update requested" means Glance accepted the update call. It deliberately does not claim that the
 * launcher rendered the result; lived/host proof owns that stronger statement.
 */
data class VexWidgetPublishResult internal constructor(
    val instanceCount: Int,
    val changedCount: Int,
    val updateRequestedCount: Int,
    val failedCount: Int,
) {
    init {
        require(
            instanceCount >= 0 &&
                changedCount >= 0 &&
                updateRequestedCount >= 0 &&
                failedCount >= 0,
        ) {
            "widget publish counts must not be negative"
        }
        require(
            changedCount <= instanceCount &&
                updateRequestedCount <= instanceCount &&
                failedCount <= instanceCount,
        ) {
            "widget publish counts must describe the same bounded instance set"
        }
    }

    /**
     * True when no instance failed its bounded persistence/update-request operation.
     *
     * updateRequestedCount may exceed changedCount on a retry: an unchanged durable SurfaceState can
     * still owe a Glance update request after an earlier request failed.
     */
    val allUpdatesRequested: Boolean
        get() =
            failedCount == 0
}

internal fun vexWidgetUpdateRequestRequired(
    stateChanged: Boolean,
    updatePending: Boolean,
): Boolean =
    stateChanged ||
        updatePending

internal fun vexWidgetNextUpdateRequestGeneration(
    currentGeneration: Long,
): Long =
    currentGeneration +
        1L

internal fun vexWidgetUpdateRequestCanSettle(
    currentGeneration: Long,
    updatePending: Boolean,
    expectedGeneration: Long,
): Boolean =
    updatePending &&
        currentGeneration ==
        expectedGeneration

internal fun <
    SurfaceState : VexWidgetSurfaceState,
> vexWidgetSurfaceStateChanged(
    current: SurfaceState?,
    next: SurfaceState,
): Boolean =
    current !=
        next

// [VXG RealForever]
