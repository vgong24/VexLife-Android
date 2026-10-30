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
    private sealed interface RelationshipKind {
        data object ModeDeliveryRouteObservation :
            RelationshipKind

        data object NavigationObservation :
            RelationshipKind

        data object RestoreDraft :
            RelationshipKind

        data object AddStop :
            RelationshipKind

        data class RemoveStop(
            val stopRef:
                String,
        ) :
            RelationshipKind

        data object ReturnToStart :
            RelationshipKind

        data object ClearDraft :
            RelationshipKind

        data object Preparation :
            RelationshipKind

        data object Leg :
            RelationshipKind
    }

    private class Relationship(
        private val session:
            VexMapsDistributionSession,
        private val kind:
            RelationshipKind,
    ) :
        VexExecution.RelationshipId<
            VexMapsDistributionSession
        > {
        override fun equals(
            other:
                Any?,
        ):
            Boolean =
            other is
                Relationship &&
                session ===
                other.session &&
                kind ==
                other.kind

        override fun hashCode():
            Int =
            31 *
                System.identityHashCode(
                    session,
                ) +
                kind.hashCode()
    }

    private fun relationship(
        kind:
            RelationshipKind,
    ):
        Relationship =
        Relationship(
            session =
                session,
            kind =
                kind,
        )

    fun start() {
        launchForCollect(
            relationshipId =
                relationship(
                    RelationshipKind.ModeDeliveryRouteObservation,
                ),
            source =
                session::modeDeliveryRouteOutputsForExecution,
        ) { output ->
            session.acceptModeDeliveryRouteOutput(
                output,
            )
        }

        launchForCollect(
            relationshipId =
                relationship(
                    RelationshipKind.NavigationObservation,
                ),
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
                relationship(
                    RelationshipKind.RestoreDraft,
                ),
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
                relationship(
                    RelationshipKind.AddStop,
                ),
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
                relationship(
                    RelationshipKind.RemoveStop(
                        stopRef,
                    ),
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
                relationship(
                    RelationshipKind.ReturnToStart,
                ),
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
                relationship(
                    RelationshipKind.ClearDraft,
                ),
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
                relationship(
                    RelationshipKind.Preparation,
                ),
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
                relationship(
                    RelationshipKind.Preparation,
                ),
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
                relationship(
                    RelationshipKind.Leg,
                ),
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
                relationship(
                    RelationshipKind.Leg,
                ),
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
