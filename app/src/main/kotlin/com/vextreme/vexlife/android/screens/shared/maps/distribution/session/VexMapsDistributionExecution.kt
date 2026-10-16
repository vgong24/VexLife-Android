package com.vextreme.vexlife.android.screens.shared.maps.distribution.session

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot

/**
 * Managed coroutine custody for one VexMaps Distribution session.
 *
 * Session owns Distribution/Delivery Route meaning, currentness, provider result meaning and retry law. This class
 * owns only execution mechanics: maintained convergence observation plus finite draft/preparation/
 * leg operations through the accepted VexExecution foundation.
 */
internal class VexMapsDistributionExecution(
    private val root:
        VexExecutionRoot,
    private val session:
        VexMapsDistributionSession,
) :
    VexExecution(
        root,
    ) {
    fun start() {
        launchForCollect(
            source =
                session::modeDeliveryRouteOutputsForExecution,
        ) { output ->
            session.acceptModeDeliveryRouteOutput(
                output,
            )
        }

        launchForCollect(
            source =
                session::navigationOutputsForExecution,
        ) { output ->
            session.acceptNavigationOutput(
                output,
            )
        }
    }

    fun restoreDraft() {
        launchOnce(
            session::restoreDraftForExecution,
        )
    }

    fun addStop(
        work:
            VexMapsDistributionAddStopWork,
    ) {
        launchOnce(
            session::addStopForExecution,
            work,
        )
    }

    fun removeStop(
        stopRef: String,
    ) {
        launchOnce(
            session::removeStopForExecution,
            stopRef,
        )
    }

    fun setReturnToStart(
        enabled: Boolean,
    ) {
        launchOnce(
            session::setReturnToStartForExecution,
            enabled,
        )
    }

    fun clearDraft() {
        launchOnce(
            session::clearDraftForExecution,
        )
    }

    fun startPreparation(
        work:
            VexMapsDistributionPreparationWork,
    ) {
        cancelPreparation()
        cancelLeg()
        launchOnce(
            session::prepareForExecution,
            work,
        )
    }

    fun cancelPreparation() {
        forceStop(
            session::prepareForExecution,
            "Distribution preparation superseded",
        )
    }

    fun startLeg(
        work:
            VexMapsDistributionLegWork,
    ) {
        cancelLeg()
        launchOnce(
            session::prepareLegForExecution,
            work,
        )
    }

    fun cancelLeg() {
        forceStop(
            session::prepareLegForExecution,
            "Distribution leg preparation superseded",
        )
    }

    suspend fun shutdown() {
        root.joinStopAll(
            "VexMaps Distribution session shut down",
        )
    }
}

// [VXG RealForever]
