package com.vextreme.vexlife.android.screens.shared.cgm

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
import com.vextreme.vexlife.android.cgm.CgmPartnerLab
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
import com.vextreme.vexlife.android.elements.localEffect
import com.vextreme.vexlife.android.preview.VexPreviewHost
import com.vextreme.vexlife.android.preview.VexScreenPreviews
import com.vextreme.vexlife.android.runtime.VexRuntime
import com.vextreme.vexlife.android.screens.AppScreenBlueprint

/**
 * Human-visible patient zero for the Partner circuit.
 *
 * The button controls the synthetic Lab source rather than pretending outside data is a
 * [CgmFamily.Control]. Each click runs the real example path:
 *
 *     bytes
 *     -> CgmPartner.Connection.Decoder
 *     -> CgmPartner.Signal
 *     -> Connection.Observation
 *     -> Connection.Receiver
 *     -> CgmStore
 *     -> CgmFamily
 *     -> DisplayState
 *     -> this Vex Compose projection
 *
 * This Screen is intentionally diagnostic/educational. Live Debug uses the Room-backed CgmStore
 * realization so accepted synthetic observations survive Screen/process reconstruction. It does not
 * claim real BLE behavior, clinical currentness, or glucose treatment meaning.
 */
object CgmPartnerLabScreen : AppScreenBlueprint(
    screenKey = "cgm-partner-lab",
    titleRes = R.string.cgm_partner_lab_title,
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
                R.string.cgm_partner_lab_intro,
            role =
                TextRole.Supporting,
        )

    val ReceivePacket =
        buttonRef(
            id =
                "body.actions.receive-packet",
            labelRes =
                R.string.cgm_partner_lab_receive_packet,
            icon =
                ElementIcon.Add,
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
                    ReceivePacket,
                )
            }
        }

    @Composable
    internal override fun renderContent(
        runtime: VexRuntime,
        repositories: AppRepositories,
    ) {
        CgmPartnerLabScreenContent(
            runtime =
                runtime,
        )
    }
}

@Composable
internal fun CgmPartnerLabScreenContent(
    runtime: VexRuntime,
    durableStore:
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
            durableStore,
        ) {
            if (
                durableStore
            ) {
                CgmPartnerLab.production(
                    context,
                )
            } else {
                CgmPartnerLab()
            }
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
            .collectAsState(
                initial =
                    CgmStore.Snapshot(),
            )
    val displayState =
        lab.family
            .displayStateFor(
                familyState,
            )

    val handler =
        remember(
            runtime,
            lab,
        ) {
            ElementActionHandler { element ->
                runtime.activate(
                    element,
                )

                if (
                    element ===
                        CgmPartnerLabScreen.ReceivePacket
                ) {
                    lab.receiveSyntheticPacket()
                }
            }
        }

    VexScrollableColumn(
        element =
            CgmPartnerLabScreen.Body,
    ) {
        VexText(
            element =
                CgmPartnerLabScreen.Intro,
        )

        VexButton(
            element =
                CgmPartnerLabScreen.ReceivePacket,
            handler =
                handler,
        )

        VexCard(
            id =
                "circuit",
        ) {
            VexColumn(
                id =
                    "content",
                modifier =
                    VexModifier
                        .padding(
                            16.dp,
                        ),
            ) {
                VexText(
                    id =
                        "partner",
                    text =
                        "Partner: CgmPartner",
                    role =
                        TextRole.Label,
                )

                val latestObservation =
                    storeSnapshot
                        .observations
                        .lastOrNull()

                VexText(
                    id =
                        "connection",
                    text =
                        "Connection: " +
                            (
                                latestObservation
                                    ?.connectionId
                                    ?.toString()
                                    ?: "waiting"
                            ),
                )

                val latestGlucose =
                    storeSnapshot
                        .latestObservedGlucose
                val latestTemperature =
                    storeSnapshot
                        .latestObservedTemperature

                VexText(
                    id =
                        "signal",
                    text =
                        if (
                            latestGlucose ==
                            null
                        ) {
                            "Signal: waiting for Partner input"
                        } else {
                            "Signal: glucose " +
                                latestGlucose.mgDl +
                                " mg/dL, trend " +
                                latestGlucose.reportedTrend.name +
                                ", sequence " +
                                latestGlucose.sequence
                        },
                )

                VexText(
                    id =
                        "temperature",
                    text =
                        if (
                            latestTemperature ==
                            null
                        ) {
                            "Second Signal: waiting"
                        } else {
                            "Second Signal: temperature " +
                                latestTemperature.celsiusHundredths +
                                " hundredths °C"
                        },
                    role =
                        TextRole.Supporting,
                )

                VexText(
                    id =
                        "store",
                    text =
                        "Store: " +
                            storeSnapshot
                                .observations
                                .size +
                            " admitted observations",
                )

                VexText(
                    id =
                        "family-state",
                    text =
                        when (
                            familyState
                        ) {
                            CgmFamily.State.Waiting ->
                                "Family State: waiting"

                            is CgmFamily.State.LatestAdmitted ->
                                "Family State: latest admitted Partner glucose"
                        },
                )

                VexText(
                    id =
                        "projection",
                    text =
                        when (
                            displayState
                        ) {
                            CgmFamily.DisplayState.Waiting ->
                                "Projection: waiting"

                            is CgmFamily.DisplayState.Reading ->
                                "Projection: " +
                                    displayState.mgDl +
                                    " mg/dL • " +
                                    displayState.trend.name +
                                    " • source " +
                                    displayState.sourceRef
                        },
                    role =
                        TextRole.Title,
                )
            }
        }
    }
}

@VexScreenPreviews
@Composable
private fun CgmPartnerLabScreenPreview() {
    VexPreviewHost(
        id =
            "cgm-partner-lab",
        startScreen =
            CgmPartnerLabScreen,
    ) { context ->
        CgmPartnerLabScreenContent(
            runtime =
                context.runtime,
            durableStore =
                false,
        )
    }
}

// [VXG RealForever][VEX-CGM-LABS][PARTNER-CIRCUIT-SCREEN]
