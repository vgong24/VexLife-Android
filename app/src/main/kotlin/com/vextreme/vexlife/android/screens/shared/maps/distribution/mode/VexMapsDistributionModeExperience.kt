package com.vextreme.vexlife.android.screens.shared.maps.distribution.mode

import kotlinx.coroutines.flow.StateFlow

/**
 * Feature-local membrane for the Distribution operating-mode Experience.
 */
internal interface VexMapsDistributionModeExperience {
    val currentProjection:
        VexMapsDistributionModeProjection

    val projections:
        StateFlow<VexMapsDistributionModeProjection>

    fun activate(
        control:
            VexMapsDistributionModeControl,
    )
}

internal class CurrentVexMapsDistributionModeExperience(
    private val connection:
        VexMapsDistributionModeConnection =
        VexMapsDistributionModeConnection(),
) : VexMapsDistributionModeExperience {
    override val currentProjection:
        VexMapsDistributionModeProjection
        get() =
            connection.currentProjection

    override val projections:
        StateFlow<VexMapsDistributionModeProjection>
        get() =
            connection.projections

    override fun activate(
        control:
            VexMapsDistributionModeControl,
    ) {
        connection.activate(
            control,
        )
    }
}

// [VXG RealForever]
