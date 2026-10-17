package com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

internal sealed interface VexMapsDeliveryRouteAct :
    VexBehaviorFamily.Act<VexMapsDeliveryRouteFamily> {
    data object PrepareRoute :
        VexMapsDeliveryRouteAct

    data object StartRoute :
        VexMapsDeliveryRouteAct

    data object ArriveAtDelivery :
        VexMapsDeliveryRouteAct

    data object ArriveAtStart :
        VexMapsDeliveryRouteAct

    data object MarkDelivered :
        VexMapsDeliveryRouteAct

    data object ClearRoute :
        VexMapsDeliveryRouteAct
}

// [VXG RealForever]
