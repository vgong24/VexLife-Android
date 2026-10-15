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
import com.vextreme.vexlife.android.data.maps.distribution.VexMapsDistributionDraft
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockPage
import com.vextreme.vexlife.android.elements.contextdock.VexContextDockPageRef
import com.vextreme.vexlife.android.elements.vexElement
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsScreen
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.planning.VexMapsDistributionPlan
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState

internal val VEXMAPS_DISTRIBUTION_CONTEXT_DOCK_PAGE_REF =
    VexContextDockPageRef(
        "vexmaps.distribution",
    )

/**
 * Distribution's feature-owned projection into the host-owned Context Dock.
 *
 * The Dock owns paging/expansion only. The Distribution Experience owns behavior and the durable
 * draft repository owns stop payload truth.
 */
@Composable
internal fun vexMapsDistributionContextDockPage(
    experience:
        VexMapsDistributionExperience,
    draft:
        VexMapsDistributionDraft,
    plan:
        VexMapsDistributionPlan?,
    canPrepare: Boolean,
    canStart: Boolean,
    onToggleMode: () -> Unit,
    onAddStop: () -> Unit,
    onToggleReturnToStart: () -> Unit,
    onPrepare: () -> Unit,
    onStart: () -> Unit,
    onCompleteStop: () -> Unit,
    onReset: () -> Unit,
): VexContextDockPage {
    val modeProjection by
        experience.modeProjections
            .collectAsState()
    val runProjection by
        experience.runProjections
            .collectAsState()

    val distributionActive =
        modeProjection.state is
            VexMapsDistributionModeState.Distribution

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
                        Text(
                            stringResource(
                                R.string
                                    .vexmaps_distribution_stop_line,
                                index + 1,
                                stop.name,
                            ),
                        )
                    }

                plan
                    ?.takeIf {
                        it.isCurrentFor(
                            draft,
                        )
                    }
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
                        runProjection.state is
                            VexMapsDistributionRunState.Empty ||
                            runProjection.state is
                            VexMapsDistributionRunState.Draft,
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
                        runProjection.state is
                            VexMapsDistributionRunState.Empty ||
                            runProjection.state is
                            VexMapsDistributionRunState.Draft,
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

                is VexMapsDistributionRunState.Ready ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_state_ready,
                        ),
                    )

                is VexMapsDistributionRunState.Running ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_state_running,
                            state.currentStopIndex + 1,
                            state.stopCount,
                        ),
                    )

                is VexMapsDistributionRunState.AtStop ->
                    Text(
                        stringResource(
                            R.string
                                .vexmaps_distribution_state_at_stop,
                            state.currentStopIndex + 1,
                            state.stopCount,
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
