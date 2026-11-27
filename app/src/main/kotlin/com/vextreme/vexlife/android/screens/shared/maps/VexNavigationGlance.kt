package com.vextreme.vexlife.android.screens.shared.maps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

/**
 * What a compact surface outside VexMaps may show about the one current journey.
 *
 * Human question: "Where am I going and what's next?" — asked from Home, the app-shell Now strip,
 * or (later) Vex context. Navigation truth stays with [VexNavigationSession]; this is a read-only
 * projection of it.
 *
 * [plannedDurationSeconds] is the route's estimate at planning time. It is NOT a live time-remaining
 * figure: VexMaps does not own one yet, so surfaces must not present it as an arrival ETA.
 */
internal data class VexNavigationGlance(
    val phase:
        VexNavigationSessionPhase,
    val destinationLabel:
        String,
    val nextInstruction:
        String?,
    val distanceToNextManeuverMeters:
        Int?,
    val distanceRemainingMeters:
        Int?,
    val plannedDurationSeconds:
        Int?,
) {
    init {
        require(
            phase !=
                VexNavigationSessionPhase.IDLE,
        ) {
            "an idle navigation session has no glance"
        }
    }
}

/** Pure projection; null while no journey is prepared or active. */
internal fun VexNavigationSessionState.toVexNavigationGlance(): VexNavigationGlance? {
    if (
        phase ==
        VexNavigationSessionPhase.IDLE
    ) {
        return null
    }
    val route =
        routeResult
            ?: return null
    val progress =
        convergence
            ?.progress

    return VexNavigationGlance(
        phase =
            phase,
        destinationLabel =
            progress
                ?.destinationLabel
                ?: route.route.destinationLabel,
        nextInstruction =
            progress
                ?.nextManeuver
                ?.maneuver
                ?.instructionProjection,
        distanceToNextManeuverMeters =
            progress
                ?.nextManeuver
                ?.distanceToManeuverMeters,
        distanceRemainingMeters =
            progress
                ?.distanceRemainingMeters,
        plannedDurationSeconds =
            route.estimatedDurationSeconds,
    )
}

/**
 * Live glance of the one process-local journey owned by [VexMapsNavigationProcess].
 *
 * Reading it never starts, stops or prepares navigation.
 */
@Composable
internal fun rememberVexNavigationGlance(): VexNavigationGlance? {
    val state by
        VexMapsNavigationProcess
            .session
            .states
            .collectAsState()

    return remember(
        state,
    ) {
        state.toVexNavigationGlance()
    }
}

// [VXG RealForever][VEXMAPS][NAVIGATION][GLANCE]
