package com.vextreme.vexlife.android.screens.shared.partners

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.cgm.CgmFamily
import com.vextreme.vexlife.android.cgm.CgmStore
import com.vextreme.vexlife.android.data.AppRepositories
import com.vextreme.vexlife.android.elements.ElementActionHandler
import com.vextreme.vexlife.android.elements.ElementIcon
import com.vextreme.vexlife.android.elements.TextRole
import com.vextreme.vexlife.android.elements.VexButton
import com.vextreme.vexlife.android.elements.VexCard
import com.vextreme.vexlife.android.elements.VexColumn
import com.vextreme.vexlife.android.elements.VexModifier
import com.vextreme.vexlife.android.elements.VexScrollableColumn
import com.vextreme.vexlife.android.elements.VexText
import com.vextreme.vexlife.android.elements.VexToggleRow
import com.vextreme.vexlife.android.elements.localEffect
import com.vextreme.vexlife.android.insulin.InsulinFamily
import com.vextreme.vexlife.android.insulin.InsulinPartnerLab
import com.vextreme.vexlife.android.partnerlab.PartnersCircuitLab
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint

/**
 * Consolidated human-facing doorway into the Partner circuit.
 *
 * This is intentionally a lab/debug Surface rather than product medical UI. It lets a human drive
 * the synthetic Partners and see the two directions of the architecture on one Screen:
 *
 * inbound:
 *     Partner -> Signal -> Observation -> Receiver -> Store -> Family -> Projection
 *
 * feedback:
 *     CGM Family State -> synthetic lab rule -> Insulin Family Control -> Effect -> Method
 *     -> Pump Partner acknowledgement -> later Pump Signal -> Store -> Insulin Family
 */
object PartnersCircuitLabScreen : AppScreenBlueprint(
    screenKey = "partners-circuit-lab",
    titleRes = R.string.partners_circuit_lab_title,
) {
    val Body =
        scrollableColumnRef(
            "body",
        )

    val Intro =
        textRef(
            id =
                "body.intro",
            textRes =
                R.string.partners_circuit_lab_intro,
            role =
                TextRole.Supporting,
        )

    val Automation =
        toggleRef(
            id =
                "body.automation.enabled",
            labelRes =
                R.string.partners_circuit_lab_automation_toggle,
            intent =
                localEffect(),
        )

    val ReceiveCgm =
        buttonRef(
            id =
                "body.cgm.actions.receive",
            labelRes =
                R.string.partners_circuit_lab_receive_cgm,
            icon =
                ElementIcon.Add,
            intent =
                localEffect(),
        )

    val ReceivePen =
        buttonRef(
            id =
                "body.pen.actions.receive",
            labelRes =
                R.string.partners_circuit_lab_receive_pen,
            icon =
                ElementIcon.Add,
            intent =
                localEffect(),
        )

    val RequestPump =
        buttonRef(
            id =
                "body.pump.actions.request",
            labelRes =
                R.string.partners_circuit_lab_request_pump,
            icon =
                ElementIcon.Play,
            intent =
                localEffect(),
        )

    val ReceivePumpOutcome =
        buttonRef(
            id =
                "body.pump.actions.receive",
            labelRes =
                R.string.partners_circuit_lab_receive_pump,
            icon =
                ElementIcon.Download,
            intent =
                localEffect(),
        )

    override val content =
        layout {
            scrollableColumn(
                Body,
            ) {
                text(
                    Intro,
                )
                toggle(
                    Automation,
                )
                button(
                    ReceiveCgm,
                )
                button(
                    ReceivePen,
                )
                button(
                    RequestPump,
                )
                button(
                    ReceivePumpOutcome,
                )
            }
        }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
    ) {
        PartnersCircuitLabScreenContent(
            runtime =
                runtime,
        )
    }
}

@Composable
internal fun PartnersCircuitLabScreenContent(
    runtime: VexRuntime,
    durableCgmStore:
        Boolean =
        true,
) {
    val context =
        LocalContext
            .current
            .applicationContext
    val lab =
        remember(
            context,
            durableCgmStore,
        ) {
            if (
                durableCgmStore
            ) {
                PartnersCircuitLab.production(
                    context,
                )
            } else {
                PartnersCircuitLab()
            }
        }

    DisposableEffect(
        lab,
    ) {
        onDispose {
            lab.close()
        }
    }

    val rule by
        lab.rule
            .collectAsState()
    val automationStatus by
        lab.automationStatus
            .collectAsState()
    val cgmState by
        lab.cgm
            .family
            .states
            .collectAsState()
    val cgmStore by
        lab.cgm
            .store
            .snapshots
            .collectAsState(
                initial =
                    CgmStore.Snapshot(),
            )
    val insulinState by
        lab.insulin
            .family
            .states
            .collectAsState()
    val insulinStore by
        lab.insulin
            .store
            .snapshots
            .collectAsState()
    val pumpStatus by
        lab.insulin
            .pumpRequestStatus
            .collectAsState()

    val handler =
        remember(
            runtime,
            lab,
        ) {
            ElementActionHandler { element ->
                runtime.activate(
                    element,
                )

                when (
                    element
                ) {
                    PartnersCircuitLabScreen.Automation ->
                        lab.toggleSyntheticRule()

                    PartnersCircuitLabScreen.ReceiveCgm ->
                        lab.receiveSyntheticCgmPacket()

                    PartnersCircuitLabScreen.ReceivePen ->
                        lab.receiveSyntheticPenPacket()

                    PartnersCircuitLabScreen.RequestPump ->
                        lab.requestSyntheticPumpDelivery()

                    PartnersCircuitLabScreen.ReceivePumpOutcome ->
                        lab.receiveSyntheticPumpOutcome()

                    else ->
                        Unit
                }
            }
        }

    VexScrollableColumn(
        element =
            PartnersCircuitLabScreen.Body,
    ) {
        VexText(
            element =
                PartnersCircuitLabScreen.Intro,
        )

        VexToggleRow(
            element =
                PartnersCircuitLabScreen.Automation,
            checked =
                rule.enabled,
            handler =
                handler,
        )

        VexCard(
            id =
                "rule",
        ) {
            VexColumn(
                id =
                    "content",
                modifier =
                    VexModifier.padding(
                        16.dp,
                    ),
            ) {
                VexText(
                    id =
                        "title",
                    text =
                        "Synthetic feedback rule",
                    role =
                        TextRole.Title,
                )
                VexText(
                    id =
                        "fixture",
                    text =
                        "Fixture only: glucose ≥ " +
                            rule.thresholdMgDl +
                            " mg/dL → request " +
                            rule.requestUnitsHundredths +
                            " hundredths U from Pump Partner.",
                    role =
                        TextRole.Supporting,
                )
                VexText(
                    id =
                        "status",
                    text =
                        automationStatus
                            .toDisplayText(),
                )
            }
        }

        VexCard(
            id =
                "cgm",
        ) {
            VexColumn(
                id =
                    "content",
                modifier =
                    VexModifier.padding(
                        16.dp,
                    ),
            ) {
                VexText(
                    id =
                        "title",
                    text =
                        "CGM Partner → Store → Family",
                    role =
                        TextRole.Title,
                )
                VexButton(
                    element =
                        PartnersCircuitLabScreen.ReceiveCgm,
                    handler =
                        handler,
                )
                VexText(
                    id =
                        "store",
                    text =
                        "CGM Store observations: " +
                            cgmStore
                                .observations
                                .size,
                )
                VexText(
                    id =
                        "family",
                    text =
                        when (
                            cgmState
                        ) {
                            CgmFamily.State.Waiting ->
                                "CgmFamily: waiting"

                            is CgmFamily.State.LatestAdmitted ->
                                "CgmFamily: " +
                                    cgmState
                                        .glucose
                                        .mgDl +
                                    " mg/dL · sequence " +
                                    cgmState
                                        .glucose
                                        .sequence
                        },
                )
            }
        }

        VexCard(
            id =
                "insulin",
        ) {
            VexColumn(
                id =
                    "content",
                modifier =
                    VexModifier.padding(
                        16.dp,
                    ),
            ) {
                VexText(
                    id =
                        "title",
                    text =
                        "Pen + Pump Partners → shared Store → Family",
                    role =
                        TextRole.Title,
                )
                VexButton(
                    element =
                        PartnersCircuitLabScreen.ReceivePen,
                    handler =
                        handler,
                )
                VexButton(
                    element =
                        PartnersCircuitLabScreen.RequestPump,
                    handler =
                        handler,
                )
                VexButton(
                    element =
                        PartnersCircuitLabScreen.ReceivePumpOutcome,
                    handler =
                        handler,
                    enabled =
                        pumpStatus is
                            InsulinPartnerLab.PumpRequestStatus.Acknowledged,
                )
                VexText(
                    id =
                        "store",
                    text =
                        "Insulin Store observations: " +
                            insulinStore
                                .observationCount,
                )
                VexText(
                    id =
                        "family",
                    text =
                        when (
                            insulinState
                        ) {
                            InsulinFamily.State.Empty ->
                                "InsulinFamily: empty"

                            is InsulinFamily.State.Admitted ->
                                "InsulinFamily: pen=" +
                                    (
                                        insulinState
                                            .latestPenDose
                                            ?.unitsHundredths
                                            ?: "-"
                                    ) +
                                    " · pump=" +
                                    (
                                        insulinState
                                            .latestPumpDelivery
                                            ?.unitsHundredths
                                            ?: "-"
                                    )
                        },
                )
                VexText(
                    id =
                        "pump",
                    text =
                        pumpStatus
                            .toDisplayText(),
                    role =
                        TextRole.Supporting,
                )
            }
        }

        VexCard(
            id =
                "execution",
        ) {
            VexColumn(
                id =
                    "content",
                modifier =
                    VexModifier.padding(
                        16.dp,
                    ),
            ) {
                VexText(
                    id =
                        "title",
                    text =
                        "Listener custody",
                    role =
                        TextRole.Title,
                )
                VexText(
                    id =
                        "body",
                    text =
                        "Store/Family listeners use VexExecution.observe(exact Flow source, live owner). " +
                            "They do not mint ResourceId merely to keep a listener running.",
                    role =
                        TextRole.Supporting,
                )
            }
        }
    }
}

private fun PartnersCircuitLab.AutomationStatus.toDisplayText():
    String =
    when (
        this
    ) {
        PartnersCircuitLab.AutomationStatus.Disabled ->
            "Automation: disabled"

        is PartnersCircuitLab.AutomationStatus.Armed ->
            "Automation: armed · latest=" +
                (
                    latestMgDl
                        ?.toString()
                        ?: "none"
                ) +
                " · threshold=" +
                thresholdMgDl

        is PartnersCircuitLab.AutomationStatus.Requested ->
            "Automation: requested from CGM " +
                sourceRef +
                " sequence " +
                sequence +
                " at " +
                mgDl +
                " mg/dL"

        is PartnersCircuitLab.AutomationStatus.PartnerAcknowledged ->
            "Automation: Pump Partner acknowledged " +
                requestRef +
                "; durable delivery still unchanged"

        is PartnersCircuitLab.AutomationStatus.PumpOutcomeObserved ->
            "Automation: Pump outcome observed " +
                deliveryRef
    }

private fun InsulinPartnerLab.PumpRequestStatus.toDisplayText():
    String =
    when (
        this
    ) {
        InsulinPartnerLab.PumpRequestStatus.Idle ->
            "Pump: idle"

        is InsulinPartnerLab.PumpRequestStatus.Acknowledged ->
            "Pump: acknowledged " +
                requestRef +
                " · waiting for Partner Signal"

        is InsulinPartnerLab.PumpRequestStatus.Rejected ->
            "Pump: rejected " +
                reason

        is InsulinPartnerLab.PumpRequestStatus.Errored ->
            "Pump: error " +
                message

        is InsulinPartnerLab.PumpRequestStatus.DeliveryObserved ->
            "Pump: delivery observed " +
                deliveryRef
    }

@VexScreenPreviews
@Composable
private fun PartnersCircuitLabScreenPreview() {
    VexPreviewHost(
        id =
            "partners-circuit-lab",
        startScreen =
            PartnersCircuitLabScreen,
    ) { context ->
        PartnersCircuitLabScreenContent(
            runtime =
                context.runtime,
            durableCgmStore =
                false,
        )
    }
}

// [VXG RealForever][VEX-PARTNER][FEEDBACK-CIRCUIT-SCREEN]
