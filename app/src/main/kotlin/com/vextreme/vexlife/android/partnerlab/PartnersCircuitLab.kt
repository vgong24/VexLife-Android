package com.vextreme.vexlife.android.partnerlab

import android.content.Context

import com.vextreme.vexlife.android.cgm.CgmFamily
import com.vextreme.vexlife.android.cgm.CgmPartner
import com.vextreme.vexlife.android.cgm.CgmPartnerLab
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import com.vextreme.vexlife.android.insulin.InsulinPartnerLab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Closed synthetic Partner feedback circuit.
 *
 * This is a lab-owned convergence rule, not medical treatment logic:
 *
 *     CGM Partner
 *     -> CgmStore
 *     -> CgmFamily
 *     -> synthetic threshold rule
 *     -> InsulinFamily.Control
 *     -> Effect
 *     -> Act.Method
 *     -> Pump Partner
 *     -> later Pump Signal
 *     -> InsulinStore
 *     -> InsulinFamily
 *
 * The rule exists specifically to prove that one source-addressed listener can react to semantic
 * Family truth and lawfully enter another Family through its Control. It does not bypass directly to
 * Pump Connection or Store.
 */
internal class PartnersCircuitLab(
    val cgm:
        CgmPartnerLab =
        CgmPartnerLab(),
    val insulin:
        InsulinPartnerLab =
        InsulinPartnerLab(),
    private val executionRoot:
        VexExecutionRoot =
        VexExecutionRoot(),
) {
    /**
     * Arbitrary fixture values chosen only to make the architecture loop easy to exercise.
     * They are not clinical thresholds or dosing guidance.
     */
    data class SyntheticRule(
        val enabled:
            Boolean =
            false,
        val thresholdMgDl:
            Int =
            106,
        val requestUnitsHundredths:
            Int =
            100,
    )

    sealed interface AutomationStatus {
        data object Disabled :
            AutomationStatus

        data class Armed(
            val latestMgDl:
                Int?,
            val thresholdMgDl:
                Int,
        ) :
            AutomationStatus

        data class Requested(
            val sourceRef:
                String,
            val sequence:
                Long,
            val mgDl:
                Int,
        ) :
            AutomationStatus

        data class PartnerAcknowledged(
            val requestRef:
                String,
        ) :
            AutomationStatus

        data class PumpOutcomeObserved(
            val deliveryRef:
                String,
        ) :
            AutomationStatus
    }

    private data class CgmTriggerKey(
        val source:
            CgmPartner.Source,
        val sequence:
            Long,
    )

    private val mutableRule =
        MutableStateFlow(
            SyntheticRule(),
        )

    val rule:
        StateFlow<SyntheticRule> =
        mutableRule
            .asStateFlow()

    private val mutableAutomationStatus =
        MutableStateFlow<AutomationStatus>(
            AutomationStatus.Disabled,
        )

    val automationStatus:
        StateFlow<AutomationStatus> =
        mutableAutomationStatus
            .asStateFlow()

    private var lastTriggered:
        CgmTriggerKey? =
        null

    init {
        /*
         * This maintained listener is source-addressed, not ResourceId-addressed. The exact
         * CgmFamily.states Flow is the local source; this lab is the live execution owner.
         */
        VexExecution.observe(
            executionRoot =
                executionRoot,
            owner =
                this,
            source =
                cgm.family::states,
        ) { state ->
            evaluateCgmState(
                state,
            )
        }

        /*
         * Pump acknowledgement/outcome is another exact Flow source. Watching it lets the lab show
         * that Method success is still only provider acknowledgement and that later Partner Signal
         * evidence is the thing that closes the durable loop.
         */
        VexExecution.observe(
            executionRoot =
                executionRoot,
            owner =
                this,
            source =
                insulin::pumpRequestStatus,
        ) { status ->
            when (
                status
            ) {
                InsulinPartnerLab.PumpRequestStatus.Idle ->
                    Unit

                is InsulinPartnerLab.PumpRequestStatus.Acknowledged ->
                    mutableAutomationStatus.value =
                        AutomationStatus.PartnerAcknowledged(
                            requestRef =
                                status.requestRef,
                        )

                is InsulinPartnerLab.PumpRequestStatus.DeliveryObserved -> {
                    mutableAutomationStatus.value =
                        AutomationStatus.PumpOutcomeObserved(
                            deliveryRef =
                                status.deliveryRef,
                        )

                    /*
                     * If a newer high CGM reading arrived while one Pump request was unresolved,
                     * re-evaluate only after the previous outcome is observed.
                     */
                    evaluateCgmState(
                        cgm.family
                            .currentState,
                    )
                }

                is InsulinPartnerLab.PumpRequestStatus.Rejected ->
                    evaluateCgmState(
                        cgm.family
                            .currentState,
                    )

                is InsulinPartnerLab.PumpRequestStatus.Errored ->
                    evaluateCgmState(
                        cgm.family
                            .currentState,
                    )
            }
        }
    }

    fun toggleSyntheticRule():
        SyntheticRule {
        val next =
            mutableRule
                .value
                .let { current ->
                    current.copy(
                        enabled =
                            !current.enabled,
                    )
                }

        mutableRule.value =
            next

        if (
            next.enabled
        ) {
            evaluateCgmState(
                cgm.family
                    .currentState,
            )
        } else {
            mutableAutomationStatus.value =
                AutomationStatus.Disabled
        }

        return next
    }

    fun receiveSyntheticCgmPacket() =
        receiveSyntheticCgmBlePacket()

    fun receiveSyntheticCgmBlePacket() =
        cgm.receiveSyntheticBlePacket()

    fun receiveSyntheticCgmHttpResponse() =
        cgm.receiveSyntheticHttpResponse()

    fun receiveSyntheticPenPacket() =
        insulin.receiveSyntheticPenPacket()

    fun requestSyntheticPumpDelivery() =
        insulin.requestSyntheticPumpDelivery()

    fun receiveSyntheticPumpOutcome() =
        insulin.receiveSyntheticPumpDelivery()

    private fun evaluateCgmState(
        state:
            CgmFamily.State,
    ) {
        val currentRule =
            rule.value

        if (
            !currentRule.enabled
        ) {
            mutableAutomationStatus.value =
                AutomationStatus.Disabled
            return
        }

        val reading =
            state as?
                CgmFamily.State.LatestAdmitted

        if (
            reading ==
            null
        ) {
            mutableAutomationStatus.value =
                AutomationStatus.Armed(
                    latestMgDl =
                        null,
                    thresholdMgDl =
                        currentRule
                            .thresholdMgDl,
                )
            return
        }

        val glucose =
            reading.glucose

        if (
            glucose.mgDl <
                currentRule.thresholdMgDl
        ) {
            mutableAutomationStatus.value =
                AutomationStatus.Armed(
                    latestMgDl =
                        glucose.mgDl,
                    thresholdMgDl =
                        currentRule
                            .thresholdMgDl,
                )
            return
        }

        val trigger =
            CgmTriggerKey(
                source =
                    glucose.source,
                sequence =
                    glucose.sequence,
            )

        if (
            trigger ==
            lastTriggered
        ) {
            return
        }

        when (
            insulin
                .pumpRequestStatus
                .value
        ) {
            is InsulinPartnerLab.PumpRequestStatus.Acknowledged ->
                return

            else ->
                Unit
        }

        lastTriggered =
            trigger

        mutableAutomationStatus.value =
            AutomationStatus.Requested(
                sourceRef =
                    glucose
                        .source
                        .deviceRef,
                sequence =
                    glucose.sequence,
                mgDl =
                    glucose.mgDl,
            )

        /*
         * Critical circuit law:
         *
         *     CGM semantic State
         *     -> another Family's Control
         *
         * Never:
         *
         *     CGM listener
         *     -> Pump Connection directly
         *
         * The Insulin Family remains the semantic owner of whether/how the Pump Effect is authored.
         */
        insulin.requestSyntheticPumpDelivery(
            unitsHundredths =
                currentRule
                    .requestUnitsHundredths,
        )
    }

    fun close() {
        executionRoot.forceStopAll(
            "Partners Circuit Lab disposed",
        )
        cgm.close()
        insulin.close()
    }

    companion object {
        fun production(
            context:
                Context,
        ):
            PartnersCircuitLab =
            PartnersCircuitLab(
                cgm =
                    CgmPartnerLab.production(
                        context,
                    ),
            )
    }
}

// [VXG RealForever][VEX-PARTNER][FEEDBACK-CIRCUIT-PATIENT-ZERO]
