package com.vextreme.vexlife.android.screens.shared.maps.distribution.run

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

internal enum class VexMapsDistributionRunControl :
    VexBehaviorFamily.Control<VexMapsDistributionRunFamily> {
    Prepare,
    Start,
    Arrive,
    CompleteStop,
    Reset,
}

// [VXG RealForever]
