package com.vextreme.vexlife.android.screens.shared.maps.distribution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockPage
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockPageRef
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsScreen
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionLegAttempt
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationLayer
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState

internal val VEXMAPS_DISTRIBUTION_CONTEXT_DOCK_PAGE_REF =
    VexContextDockPageRef(
        "vexmaps.distribution",
    )

/**
 * Distribution's feature-owned projection into the host-owned Context Dock.
 *
 * The Dock owns paging/expansion only. The Distribution Experience owns the lived session membrane;
 * lower Mode/Run families and durable Draft custody remain independently owned behind it.
 */
@Composable
internal fun vexMapsDistributionContextDockPage(
    experience:
        VexMapsDistributionExperience,
    canPrepare: Boolean,
    onToggleMode: () -> Unit,
    onAddStop: () -> Unit,
    onRemoveStop: (String) -> Unit,
    onToggleReturnToStart: () -> Unit,
    onPrepare: () -> Unit,
    onStart: () -> Unit,
    onCompleteStop: () -> Unit,
    onRetryLeg: () -> Unit,
    onReset: () -> Unit,
): VexContextDockPage {
    val session by
        experience.states
            .collectAsState()
    val modeProjection by
        experience.modeProjections
            .collectAsState()
    val runProjection by
        experience.runProjections
            .collectAsState()
    val navigation by
        experience.navigationOutputs
            .collectAsState(
                initial =
                    experience.currentNavigation,
            )

    val draft =
        session.draft
    val plan =
        session.plan
    val distributionActive =
        modeProjection.state is
            VexMapsDistributionModeState.Distribution
    val draftEditable =
        !session.draftOperationInFlight &&
            (
                runProjection.state is
                    VexMapsDistributionRunState.Empty ||
                    runProjection.state is
                    VexMapsDistributionRunState.Draft
            )
    val canStart =
        runProjection[
            VexMapsDistributionRunControl.Start
        ].isAvailable &&
            navigation.layer ==
            VexMapsDistributionNavigationLayer
                .ReadyToStart

    return VexContextDockPage(
        pageRef =
            VEXMAPS_DISTRIBUTION_CONTEXT_DOCK_PAGE_REF,
    ) { expanded ->
        Column(
            modifier =
                Modifier.fillMaxWidth(),
            verticalArrangement =
                Arrangement.spacedBy(
                    6.dp,
                ),
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(
                        R.string
                            .vexmaps_distribution_title,
                    ),
                )

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen
                                .ToggleDistributionMode,
                        ),
                    onClick =
                        onToggleMode,
                    enabled =
                        !distributionActive ||
                            draft.stops
                                .isEmpty(),
                ) {
                    Text(
                        stringResource(
                            if (distributionActive) {
                                R.string
                                    .vexmaps_distribution_mode_regular
                            } else {
                                R.string
                                    .vexmaps_distribution_mode_enter
                            },
                        ),
                    )
                }
            }

            if (!distributionActive) {
                Text(
                    stringResource(
                        R.string
                            .vexmaps_distribution_regular_body,
                    ),
                )
                return@Column
            }

            Text(
                stringResource(
                    R.string
                        .vexmaps_distribution_stop_count,
                    draft.stops.size,
                ),
            )

            if (expanded) {
                draft.stops
                    .sortedBy {
                        it.inputOrder
                    }
                    .forEachIndexed {
                        index,
                        stop,
                        ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                        ) {
                            Text(
                                stringResource(
                                    R.string
                                        .vexmaps_distribution_stop_line,
                                    index + 1,
                                    stop.name,
                                ),
                            )
                            if (draftEditable) {
                                OutlinedButton(
                                    onClick = {
                                        onRemoveStop(
                                            stop.stopRef,
                                        )
                                    },
                                ) {
                                    Text(
                                        stringResource(
                                            R.string
                                                .vexmaps_distribution_remove_stop,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                plan
                    ?.let { currentPlan ->
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_distribution_plan_eta,
                                (
                                    currentPlan
                                        .estimatedDurationSeconds +
                                        59L
                                ) / 60L,
                            ),
                        )
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_distribution_optimized_order,
                            ),
                        )
                        currentPlan
                            .orderedStops
                            .forEachIndexed {
                                index,
                                stop,
                                ->
                                Text(
                                    stringResource(
                                        R.string
                                            .vexmaps_distribution_stop_line,
                                        index + 1,
                                        stop.name,
                                    ),
                                )
                            }
                    }
            }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp,
                    ),
            ) {
                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen
                                .AddDistributionStop,
                        ),
                    onClick =
                        onAddStop,
                    enabled =
                        draftEditable,
                ) {
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_add_stop,
                        ),
                    )
                }

                OutlinedButton(
                    modifier =
                        Modifier.vexElement(
                            VexMapsScreen
                                .ToggleDistributionReturnToStart,
                        ),
                    onClick =
                        onToggleReturnToStart,
                    enabled =
                        draftEditable,
                ) {
                    Text(
                        stringResource(
                            if (
                                draft.returnToStart
                            ) {
                                R.string
                                    .vexmaps_distribution_return_on
                            } else {
                                R.string
                                    .vexmaps_distribution_return_off
                            },
                        ),
                    )
                }
            }

            when (
                val state =
                    runProjection.state
            ) {
                VexMapsDistributionRunState.Empty ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_state_empty,
                        ),
                    )

                is VexMapsDistributionRunState.Draft ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_state_draft,
                        ),
                    )

                is VexMapsDistributionRunState.Preparing ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_state_preparing,
                        ),
                    )

                is VexMapsDistributionRunState.Ready ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_state_ready,
                        ),
                    )

                is VexMapsDistributionRunState.Running -> {
                    val index =
                        state.orderedStopRefs
                            .indexOf(
                                state.currentStopRef,
                            ) +
                            1
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_state_running,
                            index,
                            state.orderedStopRefs
                                .size,
                        ),
                    )
                }

                is VexMapsDistributionRunState.AtStop -> {
                    val index =
                        state.orderedStopRefs
                            .indexOf(
                                state.currentStopRef,
                            ) +
                            1
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_state_at_stop,
                            index,
                            state.orderedStopRefs
                                .size,
                        ),
                    )
                }

                is VexMapsDistributionRunState.Returning ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_state_returning,
                        ),
                    )

                is VexMapsDistributionRunState.Completed ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_state_complete,
                        ),
                    )
            }

            session
                .draftFailure
                ?.let { reason ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_draft_failed,
                            reason,
                        ),
                    )
                }

            session
                .preparationFailure
                ?.let { reason ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_preparation_failed,
                            reason,
                        ),
                    )
                }

            (
                session.legAttempt as?
                    VexMapsDistributionLegAttempt.Failed
            )
                ?.let { failed ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_leg_failed,
                            failed.reason,
                        ),
                    )
                    Button(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen
                                    .RetryDistributionLeg,
                            ),
                        onClick =
                            onRetryLeg,
                    ) {
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_distribution_retry,
                            ),
                        )
                    }
                }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp,
                    ),
            ) {
                if (
                    runProjection[
                        VexMapsDistributionRunControl.Prepare
                    ].isAvailable
                ) {
                    Button(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen
                                    .PrepareDistributionRun,
                            ),
                        onClick =
                            onPrepare,
                        enabled =
                            canPrepare,
                    ) {
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_distribution_prepare,
                            ),
                        )
                    }
                }

                if (
                    runProjection[
                        VexMapsDistributionRunControl.Start
                    ].isAvailable
                ) {
                    Button(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen
                                    .StartDistributionRun,
                            ),
                        onClick =
                            onStart,
                        enabled =
                            canStart,
                    ) {
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_distribution_start,
                            ),
                        )
                    }
                }

                if (
                    runProjection[
                        VexMapsDistributionRunControl.CompleteStop
                    ].isAvailable
                ) {
                    Button(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen
                                    .CompleteDistributionStop,
                            ),
                        onClick =
                            onCompleteStop,
                    ) {
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_distribution_delivered,
                            ),
                        )
                    }
                }

                if (
                    runProjection[
                        VexMapsDistributionRunControl.Reset
                    ].isAvailable
                ) {
                    OutlinedButton(
                        modifier =
                            Modifier.vexElement(
                                VexMapsScreen
                                    .ResetDistributionRun,
                            ),
                        onClick =
                            onReset,
                    ) {
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_distribution_clear,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// [VXG RealForever]
