package com.vextreme.vexlife.android.screens.shared.maps.distribution.deliveryroute

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

internal enum class VexMapsDeliveryRouteControl :
    VexBehaviorFamily.Control<VexMapsDeliveryRouteFamily> {
    Prepare,
    Start,
    Arrive,
    MarkDelivered,
    Clear,
}

// [VXG RealForever]
