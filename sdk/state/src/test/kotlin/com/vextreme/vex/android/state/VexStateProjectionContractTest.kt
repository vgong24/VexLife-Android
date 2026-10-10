package com.vextreme.vex.android.state

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame

class VexStateProjectionContractTest {
    private fun snapshot(
        stateRef: String = "state.test",
        instanceRef: String = "instance.1",
        revision: Long = 0,
        transitionRefOrNull: String? = null,
        observation: VexObservation = VexObservation.PRESENT,
        semanticHash: String = "hash.a",
        value: MutableList<String>? = mutableListOf("a"),
    ) = VexStateSnapshot(
        stateRef = stateRef,
        instanceRef = instanceRef,
        revision = revision,
        transitionRefOrNull = transitionRefOrNull,
        observation = observation,
        semanticHash = semanticHash,
        valueOrNull = value,
    )

    private fun projection(initial: VexStateSnapshot<MutableList<String>>) =
        VexStateProjection(initial) { value -> value?.toMutableList() }

    @Test
    fun observationVocabularyRemainsExplicit() {
        assertEquals(
            listOf("UNOBSERVED", "EMPTY", "PRESENT", "HELD", "UNAVAILABLE"),
            VexObservation.entries.map { it.name },
        )
    }

    @Test
    fun projectionRetainsDefensiveCopies() {
        val callerOwned = mutableListOf("a")
        val projection = projection(snapshot(value = callerOwned))
        callerOwned += "mutated"

        assertEquals(listOf("a"), projection.state.value.valueOrNull?.toList())
        assertNotSame(callerOwned, projection.state.value.valueOrNull)
    }

    @Test
    fun stateValueExposureCannotMutateRetainedSnapshot() {
        val projection = projection(snapshot())
        val exposed = requireNotNull(projection.state.value.valueOrNull)
        exposed += "consumer mutation"

        val reread = projection.state.value
        assertEquals(listOf("a"), reread.valueOrNull?.toList())
        assertEquals("hash.a", reread.semanticHash)
        assertEquals(0L, reread.revision)
        assertNotSame(exposed, reread.valueOrNull)
    }

    @Test
    fun replayCacheReturnsDefensiveSnapshotCopy() {
        val projection = projection(snapshot())
        val replayed = requireNotNull(projection.state.replayCache.single().valueOrNull)
        replayed += "replay mutation"

        val reread = projection.state.value
        assertEquals(listOf("a"), reread.valueOrNull?.toList())
        assertNotSame(replayed, reread.valueOrNull)
    }

    @Test
    fun collectorsReceiveDefensiveSnapshotCopies() = runBlocking {
        val projection = projection(snapshot())
        val collected = projection.state.first()
        val exposed = requireNotNull(collected.valueOrNull)
        exposed += "collector mutation"

        val reread = projection.state.value
        assertEquals(listOf("a"), reread.valueOrNull?.toList())
        assertNotSame(exposed, reread.valueOrNull)
    }

    @Test
    fun semanticNoOpWithoutDistinctTransitionDoesNotEmit() {
        val projection = projection(snapshot())
        val result = projection.admit(snapshot(revision = 1, value = mutableListOf("a")))

        assertEquals(VexProjectionAdmission.SUPPRESSED_SEMANTIC_NO_OP, result)
        assertEquals(0L, projection.state.value.revision)
    }

    @Test
    fun sameValueWithDistinctTransitionRemainsObservable() {
        val projection = projection(snapshot())
        val result = projection.admit(
            snapshot(
                revision = 1,
                transitionRefOrNull = "transition.same-value.1",
                value = mutableListOf("a"),
            ),
        )

        assertEquals(VexProjectionAdmission.EMITTED, result)
        assertEquals(1L, projection.state.value.revision)
    }

    @Test
    fun reinstanceChangesIdentityWithoutEventLedger() {
        val projection = projection(snapshot())
        val result = projection.admit(
            snapshot(
                instanceRef = "instance.2",
                revision = 1,
                transitionRefOrNull = "transition.reinstance",
                value = mutableListOf("a"),
            ),
        )

        assertEquals(VexProjectionAdmission.EMITTED, result)
        assertEquals("instance.2", projection.state.value.instanceRef)
    }

    @Test
    fun olderRevisionFromCurrentInstanceIsRejected() {
        val projection = projection(snapshot(revision = 2, transitionRefOrNull = "transition.2"))
        val result = projection.admit(snapshot(revision = 1, transitionRefOrNull = "transition.1"))

        assertEquals(VexProjectionAdmission.REJECTED_STALE_REVISION, result)
        assertEquals(2L, projection.state.value.revision)
    }

    @Test
    fun contradictorySameRevisionFailsClosed() {
        val projection = projection(snapshot(revision = 2, transitionRefOrNull = "transition.2"))
        val result = projection.admit(
            snapshot(
                revision = 2,
                transitionRefOrNull = "transition.2",
                semanticHash = "hash.changed",
                value = mutableListOf("changed"),
            ),
        )

        assertEquals(VexProjectionAdmission.REJECTED_CONTRADICTORY_REVISION, result)
        assertEquals("hash.a", projection.state.value.semanticHash)
    }

    @Test
    fun wrongStateRefIsRejected() {
        val projection = projection(snapshot())
        assertFailsWith<IllegalArgumentException> {
            projection.admit(snapshot(stateRef = "state.other", revision = 1))
        }
    }

    @Test
    fun admittedValuesAreCopiedBeforeRetention() {
        val projection = projection(snapshot())
        val next = mutableListOf("b")
        val result = projection.admit(
            snapshot(
                revision = 1,
                transitionRefOrNull = "transition.b",
                semanticHash = "hash.b",
                value = next,
            ),
        )
        next += "mutated"

        assertEquals(VexProjectionAdmission.EMITTED, result)
        assertEquals(listOf("b"), projection.state.value.valueOrNull?.toList())
    }
}

// [VXG RealForever]
