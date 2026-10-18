package com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

internal sealed interface VexMapsDeliveryRouteEffect :
    VexBehaviorFamily.Effect<VexMapsDeliveryRouteFamily> {
    data object BeginPreparation : VexMapsDeliveryRouteEffect
    data object BeginDeliveryRoute : VexMapsDeliveryRouteEffect
    data object RecordDeliveryArrival : VexMapsDeliveryRouteEffect
    data object AdvanceToNextDelivery : VexMapsDeliveryRouteEffect
    data object BeginReturnToStart : VexMapsDeliveryRouteEffect
    data object CompleteDeliveryRoute : VexMapsDeliveryRouteEffect
    data object ClearDeliveryRoute : VexMapsDeliveryRouteEffect
}

// [VXG RealForever]
