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
@OptIn(VexExecution.RelationshipFoundationApi::class)
internal class VexMapsDistributionExecution(
    private val root:
        VexExecutionRoot,
    private val session:
        VexMapsDistributionSession,
) :
    VexExecution<
        VexMapsDistributionSession
    >(
        root,
    ) {
    private object ModeDeliveryRouteObservation :
        VexExecution.RelationshipId<
            VexMapsDistributionSession
        >

    private object NavigationObservation :
        VexExecution.RelationshipId<
            VexMapsDistributionSession
        >

    private object RestoreDraftRelationship :
        VexExecution.RelationshipId<
            VexMapsDistributionSession
        >

    private object AddStopRelationship :
        VexExecution.RelationshipId<
            VexMapsDistributionSession
        >

    private data class RemoveStopRelationship(
        val stopRef:
            String,
    ) :
        VexExecution.RelationshipId<
            VexMapsDistributionSession
        >

    private object ReturnToStartRelationship :
        VexExecution.RelationshipId<
            VexMapsDistributionSession
        >

    private object ClearDraftRelationship :
        VexExecution.RelationshipId<
            VexMapsDistributionSession
        >

    private object PreparationRelationship :
        VexExecution.RelationshipId<
            VexMapsDistributionSession
        >

    private object LegRelationship :
        VexExecution.RelationshipId<
            VexMapsDistributionSession
        >

    fun start() {
        launchForCollect(
            relationshipId =
                ModeDeliveryRouteObservation,
            source =
                session::modeDeliveryRouteOutputsForExecution,
        ) { output ->
            session.acceptModeDeliveryRouteOutput(
                output,
            )
        }

        launchForCollect(
            relationshipId =
                NavigationObservation,
            source =
                session::navigationOutputsForExecution,
        ) { output ->
            session.acceptNavigationOutput(
                output,
            )
        }
    }

    fun restoreDraft() {
        launch(
            relationshipId =
                RestoreDraftRelationship,
            approach =
                VexExecution.Approach.Earliest,
            action =
                session::restoreDraftForExecution,
        )
    }

    fun addStop(
        work:
            VexMapsDistributionAddStopWork,
    ) {
        launch(
            relationshipId =
                AddStopRelationship,
            approach =
                VexExecution.Approach.Queued,
            action =
                session::addStopForExecution,
            argument =
                work,
        )
    }

    fun removeStop(
        stopRef: String,
    ) {
        launch(
            relationshipId =
                RemoveStopRelationship(
                    stopRef,
                ),
            approach =
                VexExecution.Approach.Earliest,
            action =
                session::removeStopForExecution,
            argument =
                stopRef,
        )
    }

    fun setReturnToStart(
        enabled: Boolean,
    ) {
        launch(
            relationshipId =
                ReturnToStartRelationship,
            approach =
                VexExecution.Approach.Latest,
            action =
                session::setReturnToStartForExecution,
            argument =
                enabled,
        )
    }

    fun clearDraft() {
        launch(
            relationshipId =
                ClearDraftRelationship,
            approach =
                VexExecution.Approach.Earliest,
            action =
                session::clearDraftForExecution,
        )
    }

    fun startPreparation(
        work:
            VexMapsDistributionPreparationWork,
    ) {
        cancelLeg()
        launch(
            relationshipId =
                PreparationRelationship,
            approach =
                VexExecution.Approach.Latest,
            action =
                session::prepareForExecution,
            argument =
                work,
        )
    }

    fun cancelPreparation() {
        forceStop(
            relationshipId =
                PreparationRelationship,
            reason =
                "Distribution preparation superseded",
        )
    }

    fun startLeg(
        work:
            VexMapsDistributionLegWork,
    ) {
        launch(
            relationshipId =
                LegRelationship,
            approach =
                VexExecution.Approach.Latest,
            action =
                session::prepareLegForExecution,
            argument =
                work,
        )
    }

    fun cancelLeg() {
        forceStop(
            relationshipId =
                LegRelationship,
            reason =
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
