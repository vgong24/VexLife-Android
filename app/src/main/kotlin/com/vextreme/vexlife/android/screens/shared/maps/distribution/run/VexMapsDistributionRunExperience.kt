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

    fun observeDraft(
        revision: Long,
        stopRefs: List<String>,
        returnToStart: Boolean,
    ): Boolean

    fun preparationSucceeded(
        preparationRef: String,
        planRef: String,
    ): Boolean

    fun preparationFailed(
        preparationRef: String,
    ): Boolean

    fun invalidateReadyPlan(
        planRef: String,
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

    override fun observeDraft(
        revision: Long,
        stopRefs: List<String>,
        returnToStart: Boolean,
    ): Boolean =
        connection.observeDraft(
            revision =
                revision,
            stopRefs =
                stopRefs,
            returnToStart =
                returnToStart,
        )

    override fun preparationSucceeded(
        preparationRef: String,
        planRef: String,
    ): Boolean =
        connection.preparationSucceeded(
            preparationRef =
                preparationRef,
            planRef =
                planRef,
        )

    override fun preparationFailed(
        preparationRef: String,
    ): Boolean =
        connection.preparationFailed(
            preparationRef,
        )

    override fun invalidateReadyPlan(
        planRef: String,
    ): Boolean =
        connection.invalidateReadyPlan(
            planRef,
        )
}

// [VXG RealForever]
