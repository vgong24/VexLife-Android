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
            experience.observeDraft(
                stopCount = 3,
                returnToStart = true,
            ),
        )

        assertEquals(
            VexMapsDistributionModeState.Distribution,
            experience.modeProjection.state,
        )
        assertEquals(
            VexMapsDistributionRunState.Draft(
                stopCount = 3,
                returnToStart = true,
            ),
            experience.runProjection.state,
        )
        assertEquals(
            VexMapsDistributionControlLayer.DistributionDraft,
            experience.controlContext.layer,
        )
        assertNull(
            experience.controlContext.requestedControl,
        )
    }

    @Test
    fun activeDistributionTruthRejectsModeExitByReenteringThroughControl() {
        val experience =
            VexMapsDistributionExperience()

        experience.observeDraft(
            stopCount = 2,
            returnToStart = true,
        )
        experience.activateMode(
            VexMapsDistributionModeControl.ToggleDistribution,
        )

        assertEquals(
            VexMapsDistributionModeState.Distribution,
            experience.modeProjection.state,
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

        experience.observeDraft(
            stopCount = 2,
            returnToStart = true,
        )
        experience.activateRun(
            VexMapsDistributionRunControl.Reset,
        )
        experience.activateMode(
            VexMapsDistributionModeControl.ToggleDistribution,
        )

        assertEquals(
            VexMapsDistributionModeState.Regular,
            experience.modeProjection.state,
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

        experience.observeDraft(
            stopCount = 2,
            returnToStart = true,
        )
        experience.activateRun(
            VexMapsDistributionRunControl.Prepare,
        )
        experience.activateRun(
            VexMapsDistributionRunControl.Start,
        )

        assertFalse(
            experience.observeDraft(
                stopCount = 5,
                returnToStart = false,
            ),
        )
        assertTrue(
            experience.runProjection.state is
                VexMapsDistributionRunState.Running,
        )
    }
}

// [VXG RealForever]
