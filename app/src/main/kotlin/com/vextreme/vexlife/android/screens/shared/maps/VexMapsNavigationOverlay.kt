package com.vextreme.vexlife.android.screens.shared.maps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.vexElement

/**
 * Presentation of one prepared/active VexMaps navigation session.
 *
 * Navigation semantic state remains owned by VexNavigationSession/Convergence. This file answers only
 * what that current state should show and which already-registered human actions are available.
 */
@Composable
internal fun VexMapsNavigationCard(
    state: VexNavigationSessionState,
    locationReady: Boolean,
    canRecenter: Boolean,
    onRecenter: () -> Unit,
    onEditRoute: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (
        state.phase ==
        VexNavigationSessionPhase.IDLE
    ) {
        return
    }

    val destination =
        state
            .routeResult
            ?.route
            ?.destinationLabel
            .orEmpty()

    Card(
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp),
        ) {
            when (state.phase) {
                VexNavigationSessionPhase.READY -> {
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_ready,
                                destination,
                                state
                                    .routeResult
                                    ?.route
                                    ?.totalDistanceMeters
                                    ?: 0,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,
                    )

                    state
                        .routeResult
                        ?.estimatedDurationSeconds
                        ?.let { durationSeconds ->
                            val estimate =
                                projectVexMapsDurationEstimate(
                                    durationSeconds,
                                )

                            Text(
                                text =
                                    if (estimate.hours > 0) {
                                        stringResource(
                                            R.string
                                                .vexmaps_navigation_eta_hours,
                                            estimate.hours,
                                            estimate.minutes,
                                        )
                                    } else {
                                        stringResource(
                                            R.string
                                                .vexmaps_navigation_eta_minutes,
                                            estimate.minutes,
                                        )
                                    },
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,
                            )
                        }

                    state
                        .routeResult
                        ?.travelMode
                        ?.let { mode ->
                            Text(
                                text =
                                    stringResource(
                                        R.string
                                            .vexmaps_navigation_route_mode,
                                        stringResource(
                                            mode.labelRes,
                                        ),
                                    ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,
                            )
                        }

                    OutlinedButton(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen.EditRoute,
                            ),
                        onClick = onEditRoute,
                    ) {
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_navigation_edit_route,
                            ),
                        )
                    }

                    if (locationReady) {
                        Button(
                            modifier =
                                Modifier.vexElement(
                                    VexMapsScreen.StartNavigation,
                                ),
                            onClick = onStart,
                        ) {
                            Text(
                                stringResource(
                                    R.string
                                        .vexmaps_navigation_start,
                                ),
                            )
                        }
                    } else {
                        Text(
                            text =
                                stringResource(
                                    R.string
                                        .vexmaps_navigation_location_needed,
                                ),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                        )
                    }
                }

                VexNavigationSessionPhase.ACTIVE -> {
                    val progress =
                        state.convergence?.progress
                    val next =
                        progress?.nextManeuver

                    Text(
                        text =
                            next
                                ?.maneuver
                                ?.instructionProjection
                                ?: stringResource(
                                    R.string
                                        .vexmaps_navigation_continue,
                                ),
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                    )

                    next?.let {
                        Text(
                            text =
                                "${it.distanceToManeuverMeters} m",
                            style =
                                MaterialTheme
                                    .typography
                                    .headlineSmall,
                        )
                    }

                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_remaining,
                                progress
                                    ?.distanceRemainingMeters
                                    ?: 0,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                    )

                    NavigationStopButton(
                        onStop = onStop,
                    )
                }

                VexNavigationSessionPhase.OFF_ROUTE -> {
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_off_route,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_off_route_body,
                            ),
                    )
                    NavigationStopButton(
                        onStop = onStop,
                    )
                }

                VexNavigationSessionPhase.LOCATION_HELD -> {
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_waiting_location,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_waiting_location_body,
                            ),
                    )
                    NavigationStopButton(
                        onStop = onStop,
                    )
                }

                VexNavigationSessionPhase.ARRIVED -> {
                    Text(
                        text =
                            stringResource(
                                R.string
                                    .vexmaps_navigation_arrived,
                                destination,
                            ),
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,
                    )
                    Button(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen.DoneNavigation,
                            ),
                        onClick = onDone,
                    ) {
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_navigation_done,
                            ),
                        )
                    }
                }

                VexNavigationSessionPhase.IDLE ->
                    Unit
            }

            if (
                canRecenter &&
                (
                    state.phase ==
                        VexNavigationSessionPhase.ACTIVE ||
                    state.phase ==
                        VexNavigationSessionPhase.OFF_ROUTE ||
                    state.phase ==
                        VexNavigationSessionPhase.LOCATION_HELD
                )
            ) {
                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen.RecenterNavigation,
                        ),
                    onClick = onRecenter,
                ) {
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_navigation_recenter,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationStopButton(
    onStop: () -> Unit,
) {
    OutlinedButton(
        modifier =
            Modifier.vexElement(
                VexMapsScreen.StopNavigation,
            ),
        onClick = onStop,
    ) {
        Text(
            stringResource(
                R.string
                    .vexmaps_navigation_stop,
            ),
        )
    }
}

// [VXG RealForever]
