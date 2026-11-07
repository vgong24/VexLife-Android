package com.vextreme.vexlife.android.screens.shared.insulin

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.vextreme.vexlife.android.R
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
import com.vextreme.vexlife.android.elements.localEffect
import com.vextreme.vexlife.android.insulin.InsulinFamily
import com.vextreme.vexlife.android.insulin.InsulinPartnerLab
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint

/**
 * Human-visible pressure fixture for two Partners sharing one Store/Family.
 *
 * Pen button:
 *     synthetic bytes -> Pen Partner -> Receiver -> Store -> Family
 *
 * Pump request button:
 *     Surface -> Family Control -> Effect -> Act.Method -> Pump Partner acknowledgement
 *
 * Pump outcome button:
 *     later Pump Signal -> Receiver -> Store -> Family
 *
 * This is architecture simulation only. It is not a treatment, dosing, or real-device interface.
 */
object InsulinPartnerLabScreen : AppScreenBlueprint(
    screenKey = "insulin-partner-lab",
    titleRes = R.string.insulin_partner_lab_title,
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
                R.string.insulin_partner_lab_intro,
            role =
                TextRole.Supporting,
        )

    val ReceivePenPacket =
        buttonRef(
            id =
                "body.pen.actions.receive",
            labelRes =
                R.string.insulin_partner_lab_receive_pen,
            icon =
                ElementIcon.Add,
            intent =
                localEffect(),
        )

    val RequestPumpDelivery =
        buttonRef(
            id =
                "body.pump.actions.request",
            labelRes =
                R.string.insulin_partner_lab_request_pump,
            icon =
                ElementIcon.Play,
            intent =
                localEffect(),
        )

    val ReceivePumpDelivery =
        buttonRef(
            id =
                "body.pump.actions.receive",
            labelRes =
                R.string.insulin_partner_lab_receive_pump,
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
                button(
                    ReceivePenPacket,
                )
                button(
                    RequestPumpDelivery,
                )
                button(
                    ReceivePumpDelivery,
                )
            }
        }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
    ) {
        InsulinPartnerLabScreenContent(
            runtime =
                runtime,
        )
    }
}

@Composable
internal fun InsulinPartnerLabScreenContent(
    runtime: VexRuntime,
) {
    val lab =
        remember {
            InsulinPartnerLab()
        }

    DisposableEffect(
        lab,
    ) {
        onDispose {
            lab.close()
        }
    }

    val familyState by
        lab.family
            .states
            .collectAsState()
    val storeSnapshot by
        lab.store
            .snapshots
            .collectAsState()
    val pumpStatus by
        lab.pumpRequestStatus
            .collectAsState()
    val displayState =
        lab.family
            .displayStateFor(
                familyState,
            )
    val canReceivePumpDelivery =
        pumpStatus is
            InsulinPartnerLab.PumpRequestStatus.Acknowledged

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
                    InsulinPartnerLabScreen.ReceivePenPacket ->
                        lab.receiveSyntheticPenPacket()

                    InsulinPartnerLabScreen.RequestPumpDelivery ->
                        lab.requestSyntheticPumpDelivery()

                    InsulinPartnerLabScreen.ReceivePumpDelivery ->
                        lab.receiveSyntheticPumpDelivery()
                }
            }
        }

    VexScrollableColumn(
        element =
            InsulinPartnerLabScreen.Body,
    ) {
        VexText(
            element =
                InsulinPartnerLabScreen.Intro,
        )

        VexCard(
            id =
                "pen",
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
                        "ConnectedPenPartner",
                    role =
                        TextRole.Title,
                )
                VexButton(
                    element =
                        InsulinPartnerLabScreen.ReceivePenPacket,
                    handler =
                        handler,
                )
                val dose =
                    storeSnapshot
                        .latestPenDose
                VexText(
                    id =
                        "dose",
                    text =
                        if (
                            dose ==
                            null
                        ) {
                            "Signal: waiting for pen dose"
                        } else {
                            "Signal: " +
                                dose.doseRef +
                                " • " +
                                dose.unitsHundredths +
                                " hundredths U"
                        },
                )
                VexText(
                    id =
                        "cartridge",
                    text =
                        storeSnapshot
                            .latestPenCartridge
                            ?.let {
                                "Cartridge: " +
                                    it.remainingUnitsHundredths +
                                    " hundredths U remaining"
                            }
                            ?: "Cartridge: waiting",
                    role =
                        TextRole.Supporting,
                )
            }
        }

        VexCard(
            id =
                "pump",
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
                        "InsulinPumpPartner",
                    role =
                        TextRole.Title,
                )
                VexButton(
                    element =
                        InsulinPartnerLabScreen.RequestPumpDelivery,
                    handler =
                        handler,
                )
                VexButton(
                    element =
                        InsulinPartnerLabScreen.ReceivePumpDelivery,
                    handler =
                        handler,
                    enabled =
                        canReceivePumpDelivery,
                )
                VexText(
                    id =
                        "request-status",
                    text =
                        pumpStatus
                            .toDisplayText(),
                )
                VexText(
                    id =
                        "delivery",
                    text =
                        storeSnapshot
                            .latestPumpDelivery
                            ?.let {
                                "Stored pump delivery: " +
                                    it.deliveryRef +
                                    " • request " +
                                    (
                                        it.requestRef
                                            ?: "none"
                                    )
                            }
                            ?: "Stored pump delivery: none",
                    role =
                        TextRole.Supporting,
                )
            }
        }

        VexCard(
            id =
                "convergence",
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
                        "store",
                    text =
                        "Shared Store: " +
                            storeSnapshot
                                .observationCount +
                            " admitted observations",
                )
                VexText(
                    id =
                        "family",
                    text =
                        when (
                            familyState
                        ) {
                            InsulinFamily.State.Empty ->
                                "Family State: empty"

                            is InsulinFamily.State.Admitted ->
                                "Family State: admitted Pen/Pump evidence"
                        },
                )
                VexText(
                    id =
                        "projection",
                    text =
                        when (
                            displayState
                        ) {
                            InsulinFamily.DisplayState.Empty ->
                                "Projection: waiting"

                            is InsulinFamily.DisplayState.Evidence ->
                                buildString {
                                    append(
                                        "Projection: ",
                                    )
                                    append(
                                        displayState
                                            .storedObservationCount,
                                    )
                                    append(
                                        " stored • pen=",
                                    )
                                    append(
                                        displayState
                                            .penDoseHundredths
                                            ?: "-",
                                    )
                                    append(
                                        " • pump=",
                                    )
                                    append(
                                        displayState
                                            .pumpDeliveryHundredths
                                            ?: "-",
                                    )
                                }
                        },
                    role =
                        TextRole.Title,
                )
            }
        }
    }
}

private fun InsulinPartnerLab.PumpRequestStatus.toDisplayText():
    String =
    when (
        this
    ) {
        InsulinPartnerLab.PumpRequestStatus.Idle ->
            "Pump request: none"

        is InsulinPartnerLab.PumpRequestStatus.Acknowledged ->
            "Pump request acknowledged: " +
                requestRef +
                " (not yet delivered)"

        is InsulinPartnerLab.PumpRequestStatus.Rejected ->
            "Pump request rejected: " +
                reason

        is InsulinPartnerLab.PumpRequestStatus.Errored ->
            "Pump request errored: " +
                message

        is InsulinPartnerLab.PumpRequestStatus.DeliveryObserved ->
            "Pump delivery observed: " +
                deliveryRef +
                " for " +
                requestRef
    }

@VexScreenPreviews
@Composable
private fun InsulinPartnerLabScreenPreview() {
    VexPreviewHost(
        id =
            "insulin-partner-lab",
        startScreen =
            InsulinPartnerLabScreen,
    ) { context ->
        InsulinPartnerLabScreenContent(
            runtime =
                context.runtime,
        )
    }
}

// [VXG RealForever][VEX-PARTNER][INSULIN-PARTNER-LAB-SCREEN]
