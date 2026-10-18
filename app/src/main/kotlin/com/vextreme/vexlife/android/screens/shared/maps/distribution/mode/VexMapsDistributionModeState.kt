package com.vextreme.vexlife.android.screens.shared.maps.distribution.mode

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

internal sealed interface VexMapsDistributionModeState :
    VexBehaviorFamily.State<VexMapsDistributionModeFamily> {
    data object Regular :
        VexMapsDistributionModeState

    data object Distribution :
        VexMapsDistributionModeState
}

// [VXG RealForever]
