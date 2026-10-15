package com.vextreme.vexlife.android.screens.shared.maps.distribution

import com.vextreme.vexlife.android.screens.shared.maps.distribution.context.VexMapsDistributionControlLayer
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.mode.VexMapsDistributionModeState
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunControl
import com.vextreme.vexlife.android.screens.shared.maps.distribution.run.VexMapsDistributionRunState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexMapsDistributionExperienceTest {
    @Test
    fun draftTruthPullsModeThroughControlThenConvergesAgain() {
        val experience =
            VexMapsDistributionExperience()

        assertEquals(
            VexMapsDistributionControlLayer.Regular,
            experience.controlContext.layer,
        )

        assertTrue(
            experience.observeDraftStopCount(
                3,
            ),
        )

        assertEquals(
            VexMapsDistributionModeState.Distribution,
            experience.mode
                .currentProjection
                .state,
        )
        assertEquals(
            VexMapsDistributionRunState.Draft(
                stopCount = 3,
            ),
            experience.run
                .currentProjection
                .state,
        )
        assertEquals(
            VexMapsDistributionControlLayer.DistributionDraft,
            experience.controlContext.layer,
        )
        assertNull(
            experience.controlContext
                .requestedModeControl,
        )
    }

    @Test
    fun activeDistributionTruthRejectsModeExitByReenteringThroughControl() {
        val experience =
            VexMapsDistributionExperience()

        experience.observeDraftStopCount(
            2,
        )

        experience.activateMode(
            VexMapsDistributionModeControl.ToggleDistribution,
        )

        assertEquals(
            VexMapsDistributionModeState.Distribution,
            experience.mode
                .currentProjection
                .state,
        )
        assertEquals(
            VexMapsDistributionControlLayer.DistributionDraft,
            experience.controlContext.layer,
        )
    }

    @Test
    fun clearedRunAllowsRegularModeToRemainRegular() {
        val experience =
            VexMapsDistributionExperience()

        experience.observeDraftStopCount(
            2,
        )
        experience.activateRun(
            VexMapsDistributionRunControl.Reset,
        )
        experience.activateMode(
            VexMapsDistributionModeControl.ToggleDistribution,
        )

        assertEquals(
            VexMapsDistributionModeState.Regular,
            experience.mode
                .currentProjection
                .state,
        )
        assertEquals(
            VexMapsDistributionControlLayer.Regular,
            experience.controlContext.layer,
        )
    }

    @Test
    fun executingRunLocksDynamicDraftReplacement() {
        val experience =
            VexMapsDistributionExperience()

        experience.observeDraftStopCount(
            2,
        )
        experience.activateRun(
            VexMapsDistributionRunControl.Prepare,
        )
        experience.activateRun(
            VexMapsDistributionRunControl.Start,
        )

        assertFalse(
            experience.observeDraftStopCount(
                5,
            ),
        )
        assertTrue(
            experience.run.currentProjection.state is
                VexMapsDistributionRunState.Running,
        )
    }
}

// [VXG RealForever]
