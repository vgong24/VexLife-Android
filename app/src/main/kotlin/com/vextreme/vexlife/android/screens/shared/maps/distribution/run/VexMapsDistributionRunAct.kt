package com.vextreme.vexlife.android.screens.shared.maps.distribution.run

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

internal sealed interface VexMapsDistributionRunAct :
    VexBehaviorFamily.Act<VexMapsDistributionRunFamily> {
    data object PrepareRun :
        VexMapsDistributionRunAct

    data object BeginRun :
        VexMapsDistributionRunAct

    data object MarkArrival :
        VexMapsDistributionRunAct

    data object CompleteStop :
        VexMapsDistributionRunAct

    data object ResetRun :
        VexMapsDistributionRunAct
}

// [VXG RealForever]
