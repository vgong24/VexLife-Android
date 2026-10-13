package com.vextreme.vexlife.android.screens.shared.maps

import org.junit.Assert.assertSame
import org.junit.Test

class VexMapsPlanningStateTest {
    @Test
    fun routeOwnedStateClearsWhenRouteInputsChange() {
        assertSame(
            VexMapsPlanningState.Idle,
            VexMapsPlanningState.Routing
                .afterVexMapsRouteInputChanged(),
        )
        assertSame(
            VexMapsPlanningState.Idle,
            VexMapsPlanningState
                .Error(
                    VexMapsPlanningError.ROUTE,
                )
                .afterVexMapsRouteInputChanged(),
        )
    }

    @Test
    fun regionImportStateKeepsItsOwnershipAcrossRouteInputChanges() {
        assertSame(
            VexMapsPlanningState.Importing,
            VexMapsPlanningState.Importing
                .afterVexMapsRouteInputChanged(),
        )

        val regionError =
            VexMapsPlanningState.Error(
                VexMapsPlanningError.REGION_IMPORT,
            )
        assertSame(
            regionError,
            regionError
                .afterVexMapsRouteInputChanged(),
        )
    }

    @Test
    fun idleStateRemainsIdle() {
        assertSame(
            VexMapsPlanningState.Idle,
            VexMapsPlanningState.Idle
                .afterVexMapsRouteInputChanged(),
        )
    }
}

// [VXG RealForever]
