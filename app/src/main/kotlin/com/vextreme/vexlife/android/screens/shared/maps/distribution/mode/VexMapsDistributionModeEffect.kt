package com.vextreme.vexlife.android.screens.shared.maps.distribution.mode

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

internal sealed interface VexMapsDistributionModeEffect :
    VexBehaviorFamily.Effect<VexMapsDistributionModeFamily> {
    data object UseDistributionMode :
        VexMapsDistributionModeEffect

    data object UseRegularMode :
        VexMapsDistributionModeEffect
}

// [VXG RealForever]
