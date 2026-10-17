package com.vextreme.vexlife.android.screens.shared.maps.distribution.mode

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

internal sealed interface VexMapsDistributionModeAct :
    VexBehaviorFamily.Act<VexMapsDistributionModeFamily> {
    data object EnterDistribution :
        VexMapsDistributionModeAct

    data object ExitDistribution :
        VexMapsDistributionModeAct
}

// [VXG RealForever]
