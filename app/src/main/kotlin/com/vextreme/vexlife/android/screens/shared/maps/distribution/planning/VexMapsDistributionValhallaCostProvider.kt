package com.vextreme.vexlife.android.screens.shared.maps.distribution.planning

import android.content.Context
import com.vextreme.vexlife.android.screens.shared.maps.VexMapCoordinate
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsOfflineRoutingProcess
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsRegionPack
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsRouteIdentity
import com.vextreme.vexlife.android.screens.shared.maps.VexMapsTravelMode
import com.vextreme.vexlife.android.screens.shared.maps.VexOfflineRouteRequest

/**
 * Provider boundary used only to price candidate Distribution legs.
 *
 * Valhalla route summary time becomes the optimization cost. Missing provider duration is rejected
 * instead of mixing seconds and distance into one meaningless score.
 */
internal class VexMapsDistributionValhallaCostProvider(
    context: Context,
    private val pack:
        VexMapsRegionPack,
) : VexMapsDistributionLegCostProvider {
    private val appContext =
        context.applicationContext

    override suspend fun cost(
        from: VexMapCoordinate,
        to: VexMapCoordinate,
        travelMode: VexMapsTravelMode,
    ): VexMapsDistributionLegCost {
        val result =
            VexMapsOfflineRoutingProcess
                .planner
                .route(
                    context =
                        appContext,
                    pack =
                        pack,
                    request =
                        VexOfflineRouteRequest(
                            routeRef =
                                "route.vexmaps.distribution.cost",
                            routeRevision =
                                VexMapsRouteIdentity
                                    .nextRevision(),
                            origin =
                                from,
                            destination =
                                to,
                            destinationLabel =
                                "Distribution cost",
                            travelMode =
                                travelMode,
                        ),
                )

        return VexMapsDistributionLegCost(
            durationSeconds =
                requireNotNull(
                    result
                        .estimatedDurationSeconds,
                ) {
                    "Valhalla Distribution optimization requires route duration"
                }.toLong(),
        )
    }
}

// [VXG RealForever]
