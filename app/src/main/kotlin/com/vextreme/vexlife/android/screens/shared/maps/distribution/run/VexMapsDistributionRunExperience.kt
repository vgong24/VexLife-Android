package com.vextreme.vexlife.android.screens.shared.maps.distribution.run

import kotlinx.coroutines.flow.StateFlow

/**
 * Feature-local membrane for the finite Distribution Run Experience.
 */
internal interface VexMapsDistributionRunExperience {
    val currentProjection:
        VexMapsDistributionRunProjection

    val projections:
        StateFlow<VexMapsDistributionRunProjection>

    fun activate(
        control:
            VexMapsDistributionRunControl,
    )

    fun observeDraftStopCount(
        stopCount: Int,
    ): Boolean
}

internal class CurrentVexMapsDistributionRunExperience(
    private val connection:
        VexMapsDistributionRunConnection =
        VexMapsDistributionRunConnection(),
) : VexMapsDistributionRunExperience {
    override val currentProjection:
        VexMapsDistributionRunProjection
        get() =
            connection.currentProjection

    override val projections:
        StateFlow<VexMapsDistributionRunProjection>
        get() =
            connection.projections

    override fun activate(
        control:
            VexMapsDistributionRunControl,
    ) {
        connection.activate(
            control,
        )
    }

    override fun observeDraftStopCount(
        stopCount: Int,
    ): Boolean =
        connection.observeDraftStopCount(
            stopCount,
        )
}

// [VXG RealForever]
