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
 * - equal semantic input/output must not manufacture duplicate current-state emissions.
 */
class VexRelayContractTest {
    private data class CounterState(
        val count: Int = 0,
    ) : VexState<CounterState> {
        override fun deepCopy(): CounterState =
            copy()
    }

    private class CounterRelay :
        VexRelay<CounterState>(
            relayRef = "relay.contract.counter",
            initialState = CounterState(),
        ) {
        fun increment() {
            update { current ->
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

    private data class ProbeInput(
        val left: Int? = null,
        val right: Int? = null,
        val context: String? = null,
        val notes: MutableList<String>? = null,
    ) : VexInputState<ProbeInput> {
        override fun latest(
            incomingInputState:
                ProbeInput,
        ):
            ProbeInput =
            ProbeInput(
                left =
                    incomingInputState.left
                        ?: left,
                right =
                    incomingInputState.right
                        ?: right,
                context =
                    incomingInputState.context
                        ?: context,
                notes =
                    (
                        incomingInputState.notes
                            ?: notes
                    )?.toMutableList(),
            )

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

        override fun process(
            latestStateInput:
                ProbeInput,
            currentState:
                VexCompoundState<
                    ProbeInput,
                    ProbeOutput
                >,
        ):
            ProbeOutput =
            ProbeOutput(
                sum =
                    (latestStateInput.left ?: 0) +
                        (latestStateInput.right ?: 0),
                notes =
                    latestStateInput.notes
                        ?.toMutableList()
                        ?: mutableListOf(),
            )

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
        override fun latest(
            incomingInputState:
                RecoverableCompoundInput,
        ):
            RecoverableCompoundInput =
            RecoverableCompoundInput(
                value =
                    incomingInputState.value
                        ?: value,
                fail =
                    incomingInputState.fail
                        ?: fail,
            )

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

        override fun process(
            latestStateInput:
                RecoverableCompoundInput,
            currentState:
                VexCompoundState<
                    RecoverableCompoundInput,
                    RecoverableCompoundOutput
                >,
        ):
            RecoverableCompoundOutput {
            check(
                latestStateInput.fail !=
                    true,
            ) {
                "fixture Compound processing failure"
            }

            return RecoverableCompoundOutput(
                value =
                    latestStateInput.value
                        ?: 0,
            )
        }

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
    }

    @Test
    fun repeatedIdenticalCompoundMeaningDoesNotEmitAnotherCurrentState() = runBlocking {
        val relay =
            ProbeCompoundRelay()
        val observed =
            mutableListOf<VexCompoundState<ProbeInput, ProbeOutput>>()

        val collection =
            async(
                start = CoroutineStart.UNDISPATCHED,
            ) {
                relay.states
                    .take(3)
                    .toList(observed)
            }

        relay.context("events")
        repeat(24) {
            relay.context("events")
        }

        yield()

        // A genuinely different current meaning releases the third expected emission.
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
            observed[0].inputState.context,
        )
        assertEquals(
            "events",
            observed[1].inputState.context,
        )
        assertEquals(
            5,
            observed[2].inputState.left,
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
