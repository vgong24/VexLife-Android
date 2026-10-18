package com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute

import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.behavior.VexPortFlow

internal typealias VexMapsDeliveryRouteConnectionFlow =
    VexPortFlow<
        VexMapsDeliveryRouteFamily,
        VexMapsDeliveryRouteState,
        VexMapsDeliveryRouteControl,
        VexMapsDeliveryRouteAct
    >

internal typealias VexMapsDeliveryRouteProjection =
    VexBehaviorProjection<
        VexMapsDeliveryRouteFamily,
        VexMapsDeliveryRouteState,
        VexMapsDeliveryRouteControl,
        VexMapsDeliveryRouteAct
    >

// [VXG RealForever]
