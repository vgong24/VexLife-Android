package com.vextreme.vexlife.android.screens.shared.maps.distribution.run

import com.vextreme.vexlife.android.behavior.VexBehaviorProjection
import com.vextreme.vexlife.android.behavior.VexPortFlow

internal typealias VexMapsDistributionRunConnectionFlow =
    VexPortFlow<
        VexMapsDistributionRunFamily,
        VexMapsDistributionRunState,
        VexMapsDistributionRunControl,
        VexMapsDistributionRunAct
    >

internal typealias VexMapsDistributionRunProjection =
    VexBehaviorProjection<
        VexMapsDistributionRunFamily,
        VexMapsDistributionRunState,
        VexMapsDistributionRunControl,
        VexMapsDistributionRunAct
    >

// [VXG RealForever]
