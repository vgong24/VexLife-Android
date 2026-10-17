package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.R
import com.vextreme.vexlife.android.screens.shared.maps.distribution.convergence.VexMapsDeliveryRouteLegFailureStage
import org.junit.Assert.assertEquals
import org.junit.Test

class VexMapsDistributionPresentationTest {
    @Test
    fun deliveryRouteFailureMessagesPreserveOwnedFailureStageMeaning() {
        assertEquals(
            R.string.vexmaps_distribution_route_preparation_failed,
            vexMapsDeliveryRouteFailureMessageResource(
                VexMapsDeliveryRouteLegFailureStage.RoutePreparation,
            ),
        )
        assertEquals(
            R.string.vexmaps_distribution_navigation_start_failed,
            vexMapsDeliveryRouteFailureMessageResource(
                VexMapsDeliveryRouteLegFailureStage.NavigationStart,
            ),
        )
        assertEquals(
            R.string.vexmaps_distribution_previous_leg_cleanup_failed,
            vexMapsDeliveryRouteFailureMessageResource(
                VexMapsDeliveryRouteLegFailureStage.PreviousLegCleanup,
            ),
        )
    }
}

// [VXG RealForever]
