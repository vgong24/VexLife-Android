package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.VexMapCoordinate
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsRegionPack
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsTravelMode
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDistributionNavigationOutput
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.session.VexMapsDistributionSession
import com.vextreme.vexlife.android.screens.shared.maps.distribution.session.VexMapsDistributionSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * UI-facing membrane over one process-lifetime Distribution session.
 *
 * Compose reads projections here and sends human actions here. Session/provider/convergence mechanics
 * remain behind the membrane instead of being reconstructed by VexMapsMapSurface.
 */
internal class VexMapsDistributionExperience(
    private val session:
        VexMapsDistributionSession,
) {
    val states:
        StateFlow<VexMapsDistributionSessionState>
        get() =
            session.states

    val modeProjections:
        StateFlow<VexMapsDistributionModeProjection>
        get() =
            session.modeProjections

    val runProjections:
        StateFlow<VexMapsDistributionRunProjection>
        get() =
            session.runProjections

    val modeProjection:
        VexMapsDistributionModeProjection
        get() =
            session.currentMode

    val runProjection:
        VexMapsDistributionRunProjection
        get() =
            session.currentRun

    val navigationOutputs:
        Flow<VexMapsDistributionNavigationOutput>
        get() =
            session.navigationOutputs

    val currentNavigation:
        VexMapsDistributionNavigationOutput
        get() =
            session.currentNavigation

    fun toggleMode() =
        session.toggleMode()

    fun addStop(
        placeRef: String?,
        name: String,
        subtitle: String?,
        latitude: Double,
        longitude: Double,
    ) =
        session.addStop(
            placeRef =
                placeRef,
            name =
                name,
            subtitle =
                subtitle,
            latitude =
                latitude,
            longitude =
                longitude,
        )

    fun removeStop(
        stopRef: String,
    ) =
        session.removeStop(
            stopRef,
        )

    fun setReturnToStart(
        enabled: Boolean,
    ) =
        session.setReturnToStart(
            enabled,
        )

    fun prepare(
        origin: VexMapCoordinate,
        travelMode: VexMapsTravelMode,
        pack: VexMapsRegionPack,
    ) =
        session.prepare(
            origin =
                origin,
            travelMode =
                travelMode,
            pack =
                pack,
        )

    fun canReplaceRoutingRegion():
        Boolean =
        session.canReplaceRoutingRegion()

    fun routingRegionReplacementRequested():
        Boolean =
        session.routingRegionReplacementRequested()

    fun humanPlanningInputChanged() =
        session.humanPlanningInputChanged()

    fun startPreparedRun() =
        session.startPreparedRun()

    fun completeCurrentStop() =
        session.completeCurrentStop()

    fun retryFailedLeg() =
        session.retryFailedLeg()

    fun cancelExecutionPreservingDraft() =
        session.cancelExecutionPreservingDraft()

    fun clearAll() =
        session.clearAll()
}

// [VXG RealForever]
