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
 * foundation uses that equality directly for duplicate suppression and active-session reprojection.
 */
interface VexWidgetSurfaceState

/**
 * Feature-owned mapping between typed widget surface state and Glance Preferences storage.
 *
 * The foundation owns when state is read/written. The feature owns only the shape and persistence
 * keys of its surface snapshot.
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
data class VexWidgetPublishResult(
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
                updateRequestedCount <= changedCount &&
                failedCount <= instanceCount,
        ) {
            "widget publish counts must describe the same bounded instance set"
        }
    }

    val allUpdatesRequested: Boolean
        get() =
            failedCount == 0
}

internal fun <
    SurfaceState : VexWidgetSurfaceState,
> vexWidgetSurfaceStateChanged(
    current: SurfaceState?,
    next: SurfaceState,
): Boolean =
    current !=
        next

// [VXG RealForever]
