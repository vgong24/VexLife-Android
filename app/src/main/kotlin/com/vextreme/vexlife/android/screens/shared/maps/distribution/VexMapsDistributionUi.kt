package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.VexMapCoordinate
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsRegionPack
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsTravelMode
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteNavigationOutput
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute.VexMapsDeliveryRouteProjection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.session.VexMapsDistributionSession
import com.vextreme.vexlife.android.screens.shared.maps.distribution.session.VexMapsDistributionSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * UI-facing boundary over one process-lifetime Distribution session.
 *
 * Compose reads projections here and sends human actions here. Session/provider/convergence mechanics
 * remain behind this boundary instead of being reconstructed by VexMapsMapSurface.
 */
internal class VexMapsDistributionUi(
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

    val deliveryRouteProjections:
        StateFlow<VexMapsDeliveryRouteProjection>
        get() =
            session.deliveryRouteProjections

    val modeProjection:
        VexMapsDistributionModeProjection
        get() =
            session.currentMode

    val deliveryRouteProjection:
        VexMapsDeliveryRouteProjection
        get() =
            session.currentDeliveryRoute

    val navigationOutputs:
        Flow<VexMapsDeliveryRouteNavigationOutput>
        get() =
            session.navigationOutputs

    val currentNavigation:
        VexMapsDeliveryRouteNavigationOutput
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

    fun prepareDeliveryRoute(
        origin: VexMapCoordinate,
        travelMode: VexMapsTravelMode,
        pack: VexMapsRegionPack,
    ) =
        session.prepareDeliveryRoute(
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

    fun startPreparedDeliveryRoute() =
        session.startPreparedDeliveryRoute()

    fun markCurrentDeliveryComplete() =
        session.markCurrentDeliveryComplete()

    fun retryFailedLeg() =
        session.retryFailedLeg()

    fun cancelDeliveryRoutePreservingDraft() =
        session.cancelDeliveryRoutePreservingDraft()

    fun clearDeliveryRoute() =
        session.clearDeliveryRoute()
}

// [VXG RealForever]
