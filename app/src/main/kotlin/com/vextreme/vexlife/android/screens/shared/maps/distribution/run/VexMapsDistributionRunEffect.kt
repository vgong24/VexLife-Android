package com.vextreme.vexlife.android.screens.shared.maps.distribution.run

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

internal sealed interface VexMapsDistributionRunEffect :
    VexBehaviorFamily.Effect<VexMapsDistributionRunFamily> {
    data object SealDraft :
        VexMapsDistributionRunEffect

    data object BeginRun :
        VexMapsDistributionRunEffect

    data object MarkArrived :
        VexMapsDistributionRunEffect

    data object AdvanceToNextStop :
        VexMapsDistributionRunEffect

    data object CompleteRun :
        VexMapsDistributionRunEffect

    data object ClearRun :
        VexMapsDistributionRunEffect
}

// [VXG RealForever]
