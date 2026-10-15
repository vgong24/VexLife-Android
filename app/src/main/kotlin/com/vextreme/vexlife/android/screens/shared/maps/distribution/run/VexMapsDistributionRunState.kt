package com.vextreme.vexlife.android.screens.shared.maps.distribution.run

import com.vextreme.vexlife.android.behavior.VexBehaviorFamily

internal sealed interface VexMapsDistributionRunState :
    VexBehaviorFamily.State<VexMapsDistributionRunFamily> {
    data object Empty :
        VexMapsDistributionRunState

    data class Draft(
        val stopCount: Int,
        val returnToStart: Boolean,
    ) : VexMapsDistributionRunState {
        init { require(stopCount > 0) }
    }

    data class Ready(
        val stopCount: Int,
        val returnToStart: Boolean,
    ) : VexMapsDistributionRunState {
        init { require(stopCount > 0) }
    }

    data class Running(
        val stopCount: Int,
        val currentStopIndex: Int,
        val returnToStart: Boolean,
    ) : VexMapsDistributionRunState {
        init {
            require(stopCount > 0)
            require(currentStopIndex in 0 until stopCount)
        }
    }

    data class AtStop(
        val stopCount: Int,
        val currentStopIndex: Int,
        val returnToStart: Boolean,
    ) : VexMapsDistributionRunState {
        init {
            require(stopCount > 0)
            require(currentStopIndex in 0 until stopCount)
        }

        val isLastStop: Boolean
            get() =
                currentStopIndex ==
                    stopCount - 1
    }

    data class Returning(
        val stopCount: Int,
    ) : VexMapsDistributionRunState {
        init { require(stopCount > 0) }
    }

    data class Completed(
        val stopCount: Int,
    ) : VexMapsDistributionRunState {
        init { require(stopCount > 0) }
    }
}

// [VXG RealForever]
