package com.vextreme.vexlife.android.relay

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Base Relay contract proof.
 *
 * These tests intentionally avoid Events/product meaning. A future Relay consumer inherits these
 * guarantees without needing to rediscover them in feature tests.
 *
 * StateFlow is current-state projection, not an event ledger:
 * - intermediate racing states may be conflated for slow collectors;
 * - the final retained state must be coherent;
 * - equal semantic input/output must not manufacture duplicate revisions/emissions.
 */
class VexRelayContractTest {
    private data class CounterState(
        val count: Int = 0,
    ) : VexState<CounterState>

    private class CounterRelay :
        VexRelay<CounterState>(
            relayRef = "relay.contract.counter",
            initialState = CounterState(),
        ) {
        fun increment() {
            updateCurrent { current ->
                current.copy(
                    count = current.count + 1,
                )
            }
        }

        fun set(value: Int) {
            send(
                CounterState(
                    count = value,
                ),
            )
        }
    }

    private data class ModeProbeState(
        val value: Int = 0,
    ) : VexState<ModeProbeState>

    private class ModeProbeRelay :
        VexRelay<ModeProbeState>(
            relayRef = "relay.contract.mode-probe",
            initialState = ModeProbeState(),
        ) {
        var processStateCalls: Int =
            0
            private set

        override fun processState(
            inputState: ModeProbeState,
            currentState: ModeProbeState,
        ): ModeProbeState {
            processStateCalls += 1
            return inputState
        }

        fun inputDriven(value: Int) {
            send(
                ModeProbeState(
                    value = value,
                ),
            )
        }

        fun currentDriven(value: Int) {
            updateCurrent { current ->
                current.copy(
                    value = value,
                )
            }
        }
    }

    private data class SharedLeaf(
        val value: Int,
    )

    private data class ImmutableProbeInput(
        val left: SharedLeaf? = null,
        val right: SharedLeaf? = null,
        val context: String? = null,
        val blocked: Boolean? = null,
    ) : VexInputState<ImmutableProbeInput>

    private data class ImmutableProbeOutput(
        val sum: Int = 0,
    ) : VexOutputState<ImmutableProbeOutput>

    private class ImmutableProbeCompoundRelay :
        VexCompoundRelay<ImmutableProbeInput, ImmutableProbeOutput>(
            relayRef = "relay.contract.immutable-compound",
            initialInputState = ImmutableProbeInput(),
            initialOutputState = ImmutableProbeOutput(),
        ) {
        fun patch(
            left: SharedLeaf? = null,
            right: SharedLeaf? = null,
            context: String? = null,
            blocked: Boolean? = null,
        ) {
            send(
                ImmutableProbeInput(
                    left = left,
                    right = right,
                    context = context,
                    blocked = blocked,
                ),
            )
        }

        override fun processStates(
            inputState: ImmutableProbeInput,
            currentState: VexCompoundState<
                ImmutableProbeInput,
                ImmutableProbeOutput
            >,
        ): VexCompoundState<
            ImmutableProbeInput,
            ImmutableProbeOutput
        > {
            val previous =
                currentState.inputState

            val coherent =
                previous.copy(
                    left =
                        inputState.left
                            ?: previous.left,
                    right =
                        inputState.right
                            ?: previous.right,
                    context =
                        inputState.context
                            ?: previous.context,
                    blocked =
                        inputState.blocked
                            ?: previous.blocked,
                )

            return currentState.copy(
                inputState = coherent,
                outputState =
                    ImmutableProbeOutput(
                        sum =
                            (coherent.left?.value ?: 0) +
                                (coherent.right?.value ?: 0),
                    ),
            )
        }
    }

    private data class ProbeInput(
        val left: Int? = null,
        val right: Int? = null,
        val context: String? = null,
        val notes: MutableList<String>? = null,
    ) : VexInputState<ProbeInput> {
        override fun deepCopy(): ProbeInput =
            copy(
                notes = notes?.toMutableList(),
            )
    }

    private data class ProbeOutput(
        val sum: Int = 0,
        val notes: MutableList<String> = mutableListOf(),
    ) : VexOutputState<ProbeOutput> {
        override fun deepCopy(): ProbeOutput =
            copy(
                notes = notes.toMutableList(),
            )
    }

    private class ProbeCompoundRelay :
        VexCompoundRelay<ProbeInput, ProbeOutput>(
            relayRef = "relay.contract.compound",
            initialInputState = ProbeInput(),
            initialOutputState = ProbeOutput(),
        ) {
        fun left(value: Int) {
            send(
                ProbeInput(
                    left = value,
                ),
            )
        }

        fun right(value: Int) {
            send(
                ProbeInput(
                    right = value,
                ),
            )
        }

        fun context(value: String) {
            send(
                ProbeInput(
                    context = value,
                ),
            )
        }

        fun notes(values: MutableList<String>) {
            send(
                ProbeInput(
                    notes = values,
                ),
            )
        }

        override fun processStates(
            inputState: ProbeInput,
            currentState: VexCompoundState<ProbeInput, ProbeOutput>,
        ): VexCompoundState<ProbeInput, ProbeOutput> {
            val previous =
                currentState.inputState

            val coherent =
                ProbeInput(
                    left =
                        inputState.left
                            ?: previous.left,
                    right =
                        inputState.right
                            ?: previous.right,
                    context =
                        inputState.context
                            ?: previous.context,
                    notes =
                        (
                            inputState.notes
                                ?: previous.notes
                        )?.toMutableList(),
                )

            return currentState.copy(
                inputState = coherent,
                outputState =
                    ProbeOutput(
                        sum =
                            (coherent.left ?: 0) +
                                (coherent.right ?: 0),
                        notes =
                            coherent.notes
                                ?.toMutableList()
                                ?: mutableListOf(),
                    ),
            )
        }
    }

    private data class RecoverableState(
        val value: Int = 0,
        val fail: Boolean = false,
    ) : VexState<RecoverableState> {
        override fun deepCopy(): RecoverableState =
            copy()
    }

    private class RecoverableRelay :
        VexRelay<RecoverableState>(
            relayRef = "relay.contract.recoverable",
            initialState = RecoverableState(),
        ) {
        override fun processState(
            inputState: RecoverableState,
            currentState: RecoverableState,
        ): RecoverableState {
            check(!inputState.fail) {
                "fixture processing failure"
            }
            return inputState.deepCopy()
        }

        fun fail() {
            send(
                RecoverableState(
                    fail = true,
                ),
            )
        }

        fun set(value: Int) {
            send(
                RecoverableState(
                    value = value,
                ),
            )
        }
    }

    private data class RecoverableCompoundInput(
        val value: Int? = null,
        val fail: Boolean? = null,
    ) : VexInputState<RecoverableCompoundInput> {
        override fun deepCopy(): RecoverableCompoundInput =
            copy()
    }

    private data class RecoverableCompoundOutput(
        val value: Int = 0,
    ) : VexOutputState<RecoverableCompoundOutput> {
        override fun deepCopy(): RecoverableCompoundOutput =
            copy()
    }

    private class RecoverableCompoundRelay :
        VexCompoundRelay<
            RecoverableCompoundInput,
            RecoverableCompoundOutput
        >(
            relayRef = "relay.contract.recoverable-compound",
            initialInputState = RecoverableCompoundInput(),
            initialOutputState = RecoverableCompoundOutput(),
        ) {
        fun fail() {
            send(
                RecoverableCompoundInput(
                    fail = true,
                ),
            )
        }

        fun set(value: Int) {
            send(
                RecoverableCompoundInput(
                    value = value,
                    fail = false,
                ),
            )
        }

        override fun processStates(
            inputState: RecoverableCompoundInput,
            currentState: VexCompoundState<
                RecoverableCompoundInput,
                RecoverableCompoundOutput
            >,
        ): VexCompoundState<
            RecoverableCompoundInput,
            RecoverableCompoundOutput
        > {
            check(inputState.fail != true) {
                "fixture Compound processing failure"
            }

            val coherent =
                RecoverableCompoundInput(
                    value =
                        inputState.value
                            ?: currentState.inputState.value,
                    fail = false,
                )

            return currentState.copy(
                inputState = coherent,
                outputState =
                    RecoverableCompoundOutput(
                        value = coherent.value ?: 0,
                    ),
            )
        }
    }

    @Test
    fun sendAndUpdateCurrentHaveDistinctReducerOwnership() {
        val relay =
            ModeProbeRelay()

        relay.inputDriven(1)

        assertEquals(
            1,
            relay.processStateCalls,
        )
        assertEquals(
            1,
            relay.current.value,
        )

        relay.currentDriven(2)

        assertEquals(
            1,
            relay.processStateCalls,
        )
        assertEquals(
            2,
            relay.current.value,
        )
    }

    @Test
    fun immutableSimpleStateCanBeSharedAcrossReadBoundaries() {
        val relay =
            CounterRelay()

        relay.set(7)

        val first =
            relay.current
        val second =
            relay.current

        assertSame(
            first,
            second,
        )
        assertEquals(
            7,
            first.count,
        )
    }

    @Test
    fun oneImmutableCompoundPatchCanChangeSeveralFactsAndReuseUnchangedChildren() {
        val relay =
            ImmutableProbeCompoundRelay()
        val left =
            SharedLeaf(10)
        val right =
            SharedLeaf(32)

        relay.patch(
            left = left,
            right = right,
            context = "home",
            blocked = false,
        )

        val before =
            relay.currentInput

        relay.patch(
            context = "events",
            blocked = true,
        )

        val after =
            relay.currentInput

        assertNotSame(
            before,
            after,
        )
        assertSame(
            before.left,
            after.left,
        )
        assertSame(
            before.right,
            after.right,
        )
        assertEquals(
            "events",
            after.context,
        )
        assertEquals(
            true,
            after.blocked,
        )
        assertEquals(
            42,
            relay.currentOutput.sum,
        )

        // Immutable projection reads may share the exact retained instance.
        assertSame(
            after,
            relay.currentInput,
        )
    }

    @Test
    fun concurrentSimpleUpdatesDoNotLoseIncrements() {
        val relay =
            CounterRelay()

        runConcurrently(
            count = 24,
        ) {
            relay.increment()
        }

        assertEquals(
            24,
            relay.current.count,
        )
    }

    @Test
    fun repeatedIdenticalSimpleTriggerDoesNotCreateDifferentCurrentMeaning() {
        val relay =
            CounterRelay()

        relay.set(7)
        repeat(50) {
            relay.set(7)
        }

        assertEquals(
            CounterState(count = 7),
            relay.current,
        )
    }

    @Test
    fun concurrentSiblingPatchesConvergeWithoutLostDependencyTruth() {
        val relay =
            ProbeCompoundRelay()

        val ready =
            CountDownLatch(3)
        val go =
            CountDownLatch(1)
        val executor =
            Executors.newFixedThreadPool(3)

        try {
            val futures =
                listOf(
                    executor.submit {
                        ready.countDown()
                        go.await()
                        relay.left(10)
                    },
                    executor.submit {
                        ready.countDown()
                        go.await()
                        relay.right(32)
                    },
                    executor.submit {
                        ready.countDown()
                        go.await()
                        relay.context("events")
                    },
                )

            assertTrue(
                ready.await(
                    2L,
                    TimeUnit.SECONDS,
                ),
            )
            go.countDown()

            futures.forEach { future ->
                future.get(
                    2L,
                    TimeUnit.SECONDS,
                )
            }
        } finally {
            executor.shutdownNow()
        }

        assertEquals(
            10,
            relay.currentInput.left,
        )
        assertEquals(
            32,
            relay.currentInput.right,
        )
        assertEquals(
            "events",
            relay.currentInput.context,
        )
        assertEquals(
            42,
            relay.currentOutput.sum,
        )
        assertEquals(
            3L,
            relay.current.revision,
        )
    }

    @Test
    fun concurrentIdenticalCompoundTriggersCollapseToOneSemanticRevision() {
        val relay =
            ProbeCompoundRelay()

        runConcurrently(
            count = 24,
        ) {
            relay.context("events")
        }

        assertEquals(
            "events",
            relay.currentInput.context,
        )
        assertEquals(
            0,
            relay.currentOutput.sum,
        )
        assertEquals(
            1L,
            relay.current.revision,
        )
    }

    @Test
    fun outputProjectionSuppressesManyInputOnlyAndDuplicateTriggers() = runBlocking {
        val relay =
            ProbeCompoundRelay()

        val observed =
            mutableListOf<ProbeOutput>()

        val collection =
            async(
                start = CoroutineStart.UNDISPATCHED,
            ) {
                relay.outputStates
                    .take(2)
                    .toList(observed)
            }

        repeat(25) {
            relay.context("events")
        }

        // First real derived-output change releases the second expected emission.
        relay.left(5)

        withTimeout(1_000L) {
            collection.await()
        }

        assertEquals(
            listOf(
                0,
                5,
            ),
            observed.map(ProbeOutput::sum),
        )
    }

    @Test
    fun inputProjectionSuppressesDuplicateDependencyMeaning() = runBlocking {
        val relay =
            ProbeCompoundRelay()

        val observed =
            mutableListOf<ProbeInput>()

        val collection =
            async(
                start = CoroutineStart.UNDISPATCHED,
            ) {
                relay.inputStates
                    .take(3)
                    .toList(observed)
            }

        repeat(20) {
            relay.context("events")
        }

        // StateFlow is a current projection and may conflate rapid distinct intermediates for a slow
        // collector. Yield once because this test intentionally asserts that the coherent context
        // projection is observable before the next semantically different input arrives.
        yield()

        relay.left(5)

        withTimeout(1_000L) {
            collection.await()
        }

        assertEquals(
            3,
            observed.size,
        )
        assertEquals(
            null,
            observed[0].context,
        )
        assertEquals(
            "events",
            observed[1].context,
        )
        assertEquals(
            5,
            observed[2].left,
        )
    }

    @Test
    fun multipleOutputCollectorsReceiveEquivalentButDetachedCopies() = runBlocking {
        val relay =
            ProbeCompoundRelay()

        val first =
            mutableListOf<ProbeOutput>()
        val second =
            mutableListOf<ProbeOutput>()

        val firstCollection =
            async(
                start = CoroutineStart.UNDISPATCHED,
            ) {
                relay.outputStates
                    .take(2)
                    .toList(first)
            }
        val secondCollection =
            async(
                start = CoroutineStart.UNDISPATCHED,
            ) {
                relay.outputStates
                    .take(2)
                    .toList(second)
            }

        val callerNotes =
            mutableListOf("provider")

        relay.notes(callerNotes)

        withTimeout(1_000L) {
            firstCollection.await()
            secondCollection.await()
        }

        callerNotes +=
            "caller-mutated"

        assertEquals(
            listOf("provider"),
            relay.currentOutput.notes,
        )
        assertEquals(
            listOf("provider"),
            first.last().notes,
        )
        assertEquals(
            listOf("provider"),
            second.last().notes,
        )
        assertNotSame(
            first.last().notes,
            second.last().notes,
        )

        first.last().notes +=
            "collector-mutated"

        assertEquals(
            listOf("provider"),
            relay.currentOutput.notes,
        )
        assertEquals(
            listOf("provider"),
            second.last().notes,
        )
    }

    @Test
    fun processingFailureReleasesSerializationBoundaryForNextTrigger() {
        val relay =
            RecoverableRelay()

        assertThrows(
            IllegalStateException::class.java,
        ) {
            relay.fail()
        }

        relay.set(9)

        assertEquals(
            9,
            relay.current.value,
        )
    }

    @Test
    fun compoundProcessingFailureReleasesSerializationBoundaryForNextTrigger() {
        val relay =
            RecoverableCompoundRelay()

        assertThrows(
            IllegalStateException::class.java,
        ) {
            relay.fail()
        }

        relay.set(11)

        assertEquals(
            11,
            relay.currentInput.value,
        )
        assertEquals(
            11,
            relay.currentOutput.value,
        )
        assertEquals(
            1L,
            relay.current.revision,
        )
    }

    private fun runConcurrently(
        count: Int,
        block: () -> Unit,
    ) {
        require(count > 0)

        val ready =
            CountDownLatch(count)
        val go =
            CountDownLatch(1)
        val executor =
            Executors.newFixedThreadPool(count)

        try {
            val futures =
                (0 until count).map {
                    executor.submit {
                        ready.countDown()
                        go.await()
                        block()
                    }
                }

            assertTrue(
                ready.await(
                    2L,
                    TimeUnit.SECONDS,
                ),
            )
            go.countDown()

            futures.forEach { future ->
                future.get(
                    3L,
                    TimeUnit.SECONDS,
                )
            }
        } finally {
            executor.shutdownNow()
        }
    }
}

// [VXG RealForever]
