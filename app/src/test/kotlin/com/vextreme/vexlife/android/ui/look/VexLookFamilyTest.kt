package com.vextreme.vexlife.android.ui.look

import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class VexLookFamilyTest {
    private val aurora =
        VexLookPreference(choice = VexLookChoice.Preset(VexLookPreset.Aurora))

    @Test
    fun choosingAdmitsTheLookImmediatelyAndAuthorsALatestSave() {
        val family = VexLookFamily()
        val parameter = VexLookFamily.Parameter.Preference(aurora)

        val accepted =
            requireNotNull(
                family.choose(VexLookFamily.Control.Choose(parameter)),
            )
        val begin = requireNotNull(accepted.realizationBegin)

        assertEquals(aurora, family.currentState)
        assertSame(parameter, (begin.effect as VexLookFamily.Effect.Save).preference)
        assertSame(VexExecution.Approach.Latest, begin.approach)
    }

    @Test
    fun staleStoreObservationCannotOverwriteAnUnsettledChoice() {
        val family = VexLookFamily()
        val parameter = VexLookFamily.Parameter.Preference(aurora)
        family.choose(VexLookFamily.Control.Choose(parameter))

        // The Store still reports the old preference while the save is in flight.
        family.observeStored(VexLookPreference())
        assertEquals(aurora, family.currentState)

        // Once settled, Store truth is admitted again.
        family.settle(parameter)
        family.observeStored(VexLookPreference(motion = VexMotion.Calm))
        assertEquals(VexMotion.Calm, family.currentState.motion)
    }

    @Test
    fun anOlderSettlementCannotClearANewerChoice() {
        val family = VexLookFamily()
        val first = VexLookFamily.Parameter.Preference(aurora)
        val second =
            VexLookFamily.Parameter.Preference(
                VexLookPreference(choice = VexLookChoice.Custom(330)),
            )
        family.choose(VexLookFamily.Control.Choose(first))
        family.choose(VexLookFamily.Control.Choose(second))

        family.settle(first)
        family.observeStored(aurora)

        assertEquals(VexLookChoice.Custom(330), family.currentState.choice)
    }

    @Test
    fun activationSavesDurablyAndSettles() =
        runBlocking {
            val root = VexExecutionRoot()
            val store = InMemoryVexLookStore()
            val family = VexLookFamily()
            observeVexLookStore(family, store, root)
            try {
                val chosen = VexLookPreference(choice = VexLookChoice.Custom(120), ambient = false)
                activateVexLook(family, chosen, store, root)

                assertEquals(chosen, family.currentState)
                withTimeout(5_000) {
                    while (store.states.first() != chosen) yield()
                }
                assertEquals(chosen, store.states.first())
            } finally {
                root.forceStopAll()
            }
        }

    @Test
    fun aSaveThatDoesNotCommitKeepsTheVisibleChoiceForThisProcess() =
        runBlocking {
            val root = VexExecutionRoot()
            val store = InMemoryVexLookStore(commits = false)
            val family = VexLookFamily()
            try {
                activateVexLook(family, aurora, store, root)
                assertEquals(aurora, family.currentState)
                assertFalse(store.states.first() == aurora)
            } finally {
                root.forceStopAll()
            }
        }
}

// [VXG RealForever][UI][LOOK][FAMILY][PROOF]
