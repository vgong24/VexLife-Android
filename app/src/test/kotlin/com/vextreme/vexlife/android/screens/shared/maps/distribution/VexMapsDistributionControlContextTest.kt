package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.context.VexMapsDistributionControlContextInputState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.context.VexMapsDistributionControlLayer
import com.vextreme.vexlife.android.screens.shared.maps.distribution.context.projectVexMapsDistributionControlContext
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeConnection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunConnection
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VexMapsDistributionControlContextTest {
    @Test
    fun returningRunProjectsAStableReturningLayer() {
        val mode =
            VexMapsDistributionModeConnection(
                initialState =
                    VexMapsDistributionModeState.Distribution,
            )
        val run =
            VexMapsDistributionRunConnection(
                initialState =
                    VexMapsDistributionRunState.Returning(
                        stopCount = 3,
                    ),
            )

        val context =
            projectVexMapsDistributionControlContext(
                VexMapsDistributionControlContextInputState(
                    mode =
                        mode.currentProjection,
                    run =
                        run.currentProjection,
                ),
            )

        assertEquals(
            VexMapsDistributionControlLayer.DistributionReturning,
            context.layer,
        )
        assertNull(
            context.requestedControl,
        )
    }
}

// [VXG RealForever]
