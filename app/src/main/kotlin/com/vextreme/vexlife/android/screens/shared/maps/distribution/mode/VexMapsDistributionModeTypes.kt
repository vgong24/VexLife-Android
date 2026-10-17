package com.vextreme.vexlife.android.screens.shared.maps.distribution.mode

import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.behavior.VexPortFlow

internal typealias VexMapsDistributionModeConnectionFlow =
    VexPortFlow<
        VexMapsDistributionModeFamily,
        VexMapsDistributionModeState,
        VexMapsDistributionModeControl,
        VexMapsDistributionModeAct
    >

internal typealias VexMapsDistributionModeProjection =
    VexBehaviorProjection<
        VexMapsDistributionModeFamily,
        VexMapsDistributionModeState,
        VexMapsDistributionModeControl,
        VexMapsDistributionModeAct
    >

// [VXG RealForever]
