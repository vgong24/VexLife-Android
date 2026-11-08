@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.execution

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VexExecutionTest {
    private class BoundOperation(
        private val value:
            String,
        private val calls:
            MutableList<String>,
    ) {
        suspend operator fun invoke():
            String {
            calls +=
                value
            return value
        }
    }

    private class TestRepository {
        var onceCount = 0
        val collectAttempts = AtomicInteger(0)
        val sourceCreationAttempts = AtomicInteger(0)
        val downstreamSourceSubscriptions = AtomicInteger(0)
        val childStarted = CompletableDeferred<Unit>()
        val childStopped = CompletableDeferred<Unit>()
        val independentStarted = CopyOnWriteArrayList<String>()
        val independentRelease = CompletableDeferred<Unit>()
        val earliestStarted = CopyOnWriteArrayList<String>()
        val earliestRelease = CompletableDeferred<Unit>()
        val latestStarted = CopyOnWriteArrayList<String>()
        val latestCleanupStarted = CompletableDeferred<Unit>()
        val latestCleanupRelease = CompletableDeferred<Unit>()
        val queuedStarted = CopyOnWriteArrayList<String>()
        val queuedReleaseA = CompletableDeferred<Unit>()
        val queuedReleaseB = CompletableDeferred<Unit>()

        suspend fun once() {
            onceCount += 1
        }

        suspend fun independentWork(value: String) {
            independentStarted += value
            independentRelease.await()
        }

        suspend fun earliestWork(value: String) {
            earliestStarted += value
            earliestRelease.await()
        }

        suspend fun latestWork(value: String) {
            latestStarted += value
            if (value == "A") {
                try {
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) {
                        latestCleanupStarted.complete(Unit)
                        latestCleanupRelease.await()
                    }
                }
            }
        }

        suspend fun queuedWork(value: String) {
            queuedStarted += value
            when (value) {
                "A" -> queuedReleaseA.await()
                "B" -> queuedReleaseB.await()
            }
        }

        suspend fun successfulResult(value: String): String = value

        suspend fun structuredWork(): Unit =
            coroutineScope {
                launch {
                    try {
                        childStarted.complete(Unit)
                        awaitCancellation()
                    } finally {
                        childStopped.complete(Unit)
                    }
                }
                awaitCancellation()
            }

        fun states(): Flow<Int> =
            flow {
                emit(7)
                awaitCancellation()
            }

        fun secondaryStates(): Flow<Int> =
            flow {
                emit(9)
                awaitCancellation()
            }

        fun restartableStates(): Flow<Int> =
            flow {
                val attempt = collectAttempts.incrementAndGet()
                if (attempt == 1) {
                    error("first collector attempt failed")
                }
                emit(11)
                awaitCancellation()
            }

        fun restartDelayStates(): Flow<Int> =
            flow {
                collectAttempts.incrementAndGet()
                error("fail into restart delay")
            }

        fun sourceCreationStates(): Flow<Int> {
            val attempt = sourceCreationAttempts.incrementAndGet()
            if (attempt == 1) {
                error("source creation failed")
            }
            return flow {
                emit(17)
                awaitCancellation()
            }
        }

        fun downstreamFailureStates(): Flow<Int> =
            flow {
                downstreamSourceSubscriptions.incrementAndGet()
                emit(19)
                awaitCancellation()
            }
    }

    private data class DirectRelationship(
        val value:
            String,
    ) :
        VexExecution.RelationshipId

    private data class HandoffCapture(
        val workerThread: CompletableDeferred<String>,
        val providerThread: CompletableDeferred<String>,
    )

    private class TestExecution(
        root: VexExecutionRoot,
        private val repository: TestRepository,
        private val handoffDispatcher: CoroutineDispatcher? = null,
    ) : VexExecution(root) {
        private data class Relationship(
            val value: String,
        ) : VexExecution.RelationshipId

        private val refresh = Relationship("refresh")
        private val latest = Relationship("latest")
        private val queued = Relationship("queued")
        private val cancel = Relationship("cancel")

        fun runOnce(): Job =
            launchOnce(repository::once)

        fun runIndependent(value: String): Job =
            launch(repository::independentWork, value)

        fun runExclusive(value: String): Job =
            launchOnce(repository::independentWork, value)

        fun runEarliest(value: String): Job =
            beginEarliest(refresh, repository::earliestWork, value)

        fun runRefreshQueued(value: String): Job =
            beginQueued(refresh, repository::queuedWork, value)

        fun runLatest(value: String): Job =
            beginLatest(latest, repository::latestWork, value)

        fun runQueued(value: String): Job =
            beginQueued(queued, repository::queuedWork, value)

        fun runCancellable(value: String): Job =
            beginLatest(cancel, repository::independentWork, value)

        fun cancelCancellable(): Boolean =
            cancelActiveRelationship(cancel, "test cancel")

        fun runStructured(): Job =
            launchOnce(repository::structuredWork)

        fun observeStates(values: MutableList<Int>): Job =
            launchForCollect(repository::states) { values += it }

        fun observeSecondary(values: MutableList<Int>): Job =
            launchForCollect(repository::secondaryStates) { values += it }

        fun observeRestartable(values: MutableList<Int>): Job =
            launchForCollect(repository::restartableStates) { values += it }

        fun observeSourceCreation(values: MutableList<Int>): Job =
            launchForCollect(repository::sourceCreationStates) { values += it }

        fun observeDownstreamFailure(callbackCount: AtomicInteger): Job =
            launchForCollect(repository::downstreamFailureStates) {
                callbackCount.incrementAndGet()
                error("downstream consumer failed")
            }

        fun observeRestartDelay(): Job =
            launchForCollect(repository::restartDelayStates) {}

        fun stopRestartDelay(): Boolean =
            forceStop(repository::restartDelayStates, "stop restart delay")

        fun stopOwned(): Boolean =
            forceStopOwned("test owner stop")

        suspend fun joinOwned(): Boolean =
            joinStopOwned("test owner join")

        fun runResultAdmissionFailure(operationFailureCount: AtomicInteger): Job =
            begin(
                operation = repository::successfulResult,
                argument = "success",
                onResult = {
                    error("result admission failed")
                },
                onFailure = {
                    operationFailureCount.incrementAndGet()
                },
            )

        fun runHandoff(capture: HandoffCapture): Job =
            launch(::handoffForExecution, capture)

        private suspend fun handoffForExecution(capture: HandoffCapture) {
            capture.workerThread.complete(Thread.currentThread().name)
            handoff(requireNotNull(handoffDispatcher)) {
                capture.providerThread.complete(Thread.currentThread().name)
            }
        }
    }

    @Test
    fun launchOnceRunsAndRejectsASecondUnfinishedInvocation() =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                val first = execution.runExclusive("A")

                withTimeout(2_000) {
                    while (repository.independentStarted.isEmpty()) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertFalse(
                    runCatching {
                        execution.runExclusive("B")
                    }.isSuccess,
                )

                repository.independentRelease.complete(Unit)
                first.join()
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun independentLaunchesUseTheActualJobsAsDistinctRuntimeOccurrences() =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                val first = execution.runIndependent("A")
                val second = execution.runIndependent("B")

                withTimeout(2_000) {
                    while (repository.independentStarted.size < 2) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertFalse(first === second)

                repository.independentRelease.complete(Unit)
                first.join()
                second.join()
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun earliestReturnsTheExistingJobForTheSameRelationship() =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                val first = execution.runEarliest("A")

                withTimeout(2_000) {
                    while (repository.earliestStarted.isEmpty()) {
                        kotlinx.coroutines.yield()
                    }
                }

                val second = execution.runEarliest("B")

                assertSame(first, second)
                assertEquals(listOf("A"), repository.earliestStarted.toList())

                repository.earliestRelease.complete(Unit)
                first.join()
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun completedRelationshipIsRemovedAndMayLaterUseAnotherApproach() =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                val first = execution.runEarliest("A")
                withTimeout(2_000) {
                    while (repository.earliestStarted.isEmpty()) {
                        kotlinx.coroutines.yield()
                    }
                }
                repository.earliestRelease.complete(Unit)
                first.join()

                val queued = execution.runRefreshQueued("C")
                queued.join()

                assertEquals(
                    listOf("C"),
                    repository.queuedStarted.toList(),
                )
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun latestWaitsThroughCancelledPredecessorCleanup() =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                execution.runLatest("A")
                withTimeout(2_000) {
                    while (!repository.latestStarted.contains("A")) {
                        kotlinx.coroutines.yield()
                    }
                }

                execution.runLatest("B")
                withTimeout(2_000) {
                    repository.latestCleanupStarted.await()
                }

                val latest = execution.runLatest("C")
                kotlinx.coroutines.yield()

                assertEquals(listOf("A"), repository.latestStarted.toList())

                repository.latestCleanupRelease.complete(Unit)
                withTimeout(2_000) {
                    latest.join()
                }

                assertEquals(
                    listOf("A", "C"),
                    repository.latestStarted.toList(),
                )
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun queuedRelationshipPreservesArrivalOrder() =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                val first = execution.runQueued("A")
                val second = execution.runQueued("B")
                val third = execution.runQueued("C")

                withTimeout(2_000) {
                    while (repository.queuedStarted.isEmpty()) {
                        kotlinx.coroutines.yield()
                    }
                }
                assertEquals(listOf("A"), repository.queuedStarted.toList())

                repository.queuedReleaseA.complete(Unit)
                withTimeout(2_000) {
                    while (repository.queuedStarted.size < 2) {
                        kotlinx.coroutines.yield()
                    }
                }
                assertEquals(listOf("A", "B"), repository.queuedStarted.toList())

                repository.queuedReleaseB.complete(Unit)
                first.join()
                second.join()
                third.join()

                assertEquals(
                    listOf("A", "B", "C"),
                    repository.queuedStarted.toList(),
                )
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun relationshipCancellationUsesTheSameRelationshipAddressAndAbsentIsNoOp() =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                assertFalse(execution.cancelCancellable())

                val job = execution.runCancellable("A")
                withTimeout(2_000) {
                    while (repository.independentStarted.isEmpty()) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertTrue(execution.cancelCancellable())
                job.join()
                assertTrue(job.isCancelled)

                assertFalse(execution.cancelCancellable())
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun stoppingOneExecutionOwnerDoesNotTerminateSiblingOwnerOnTheSameRoot() =
        runBlocking {
            val root = VexExecutionRoot()
            val firstRepository = TestRepository()
            val secondRepository = TestRepository()
            val first = TestExecution(root, firstRepository)
            val second = TestExecution(root, secondRepository)

            try {
                val firstJob = first.runIndependent("first")
                val secondJob = second.runIndependent("second")

                withTimeout(2_000) {
                    while (
                        firstRepository.independentStarted.isEmpty() ||
                        secondRepository.independentStarted.isEmpty()
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertTrue(first.stopOwned())
                firstJob.join()

                assertTrue(firstJob.isCancelled)
                assertTrue(secondJob.isActive)

                secondRepository.independentRelease.complete(Unit)
                secondJob.join()
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun structuredChildrenRemainInsideOwnedJobLifetime() =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                val job = execution.runStructured()
                withTimeout(2_000) {
                    repository.childStarted.await()
                }

                assertTrue(execution.stopOwned())
                job.join()

                withTimeout(2_000) {
                    repository.childStopped.await()
                }
                assertTrue(job.isCancelled)
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun maintainedFlowSourcesRunAsSiblingJobsAndDuplicateObservationFailsClosed() =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val values = CopyOnWriteArrayList<Int>()

            try {
                val first = execution.observeStates(values)
                val second = execution.observeSecondary(values)

                withTimeout(2_000) {
                    while (!values.containsAll(listOf(7, 9))) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertTrue(first.isActive)
                assertTrue(second.isActive)

                assertFalse(
                    runCatching {
                        execution.observeStates(values)
                    }.isSuccess,
                )
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun currentProjectionDerivesActiveObservationCustodyWithoutExposingJobHandles() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val repository =
                TestRepository()
            val execution =
                TestExecution(
                    root,
                    repository,
                )
            val values =
                CopyOnWriteArrayList<Int>()

            try {
                execution.observeStates(
                    values,
                )
                execution.observeSecondary(
                    values,
                )

                withTimeout(
                    2_000,
                ) {
                    while (
                        !values.containsAll(
                            listOf(
                                7,
                                9,
                            ),
                        )
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                val projection =
                    execution.currentExecution

                assertEquals(
                    2,
                    projection.activeJobCount,
                )
                assertEquals(
                    listOf(
                        "secondaryStates",
                        "states",
                    ),
                    projection.sources
                        .map { source ->
                            source.sourceLabel
                        },
                )
                assertTrue(
                    projection.sources
                        .all { source ->
                            source.maintainedObservation
                        },
                )
                assertTrue(
                    projection.relationships.isEmpty(),
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun directOwnerProjectionAndSourceStopReuseCanonicalCustody() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val repository =
                TestRepository()
            val owner =
                Any()
            val values =
                CopyOnWriteArrayList<Int>()

            try {
                val job =
                    VexExecution.observe(
                        executionRoot =
                            root,
                        owner =
                            owner,
                        source =
                            repository::states,
                    ) { value ->
                        values +=
                            value
                    }

                withTimeout(
                    2_000,
                ) {
                    while (
                        7 !in
                        values
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                val current =
                    VexExecution.currentExecution(
                        executionRoot =
                            root,
                        owner =
                            owner,
                    )

                assertEquals(
                    1,
                    current.activeJobCount,
                )
                assertEquals(
                    "states",
                    current.sources.single().sourceLabel,
                )
                assertTrue(
                    current.sources.single().maintainedObservation,
                )

                assertTrue(
                    VexExecution.forceStop(
                        executionRoot =
                            root,
                        owner =
                            owner,
                        source =
                            repository::states,
                        reason =
                            "test direct source stop",
                    ),
                )

                job.join()

                assertTrue(
                    job.isCancelled,
                )
                assertEquals(
                    0,
                    VexExecution.currentExecution(
                        executionRoot =
                            root,
                        owner =
                            owner,
                    ).activeJobCount,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun sourceCreationAndUpstreamFailureMayRecoverButDownstreamFailureDoesNotRestart() =
        runBlocking {
            val root =
                VexExecutionRoot(
                    maxCollectRestarts = 1,
                    collectRestartDelayMillis = 0,
                )
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val values = CopyOnWriteArrayList<Int>()

            try {
                val restartable = execution.observeRestartable(values)
                withTimeout(2_000) {
                    while (!values.contains(11)) {
                        kotlinx.coroutines.yield()
                    }
                }
                assertEquals(2, repository.collectAttempts.get())
                restartable.cancel()
                restartable.join()

                val sourceValues = CopyOnWriteArrayList<Int>()
                val source = execution.observeSourceCreation(sourceValues)
                withTimeout(2_000) {
                    while (!sourceValues.contains(17)) {
                        kotlinx.coroutines.yield()
                    }
                }
                assertEquals(2, repository.sourceCreationAttempts.get())
                source.cancel()
                source.join()

                val callbackCount = AtomicInteger(0)
                val downstream = execution.observeDownstreamFailure(callbackCount)
                downstream.join()

                assertEquals(1, repository.downstreamSourceSubscriptions.get())
                assertEquals(1, callbackCount.get())
                assertTrue(downstream.isCompleted)
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun sourceCancellationDuringRestartDelaySuppressesAnotherAttempt() =
        runBlocking {
            val root =
                VexExecutionRoot(
                    maxCollectRestarts = 5,
                    collectRestartDelayMillis = 60_000,
                )
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                val job = execution.observeRestartDelay()

                withTimeout(2_000) {
                    while (repository.collectAttempts.get() < 1) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertTrue(execution.stopRestartDelay())
                job.join()

                assertEquals(1, repository.collectAttempts.get())
                assertTrue(job.isCancelled)
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun resultAdmissionFailureDoesNotBecomeOperationFailure() =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val operationFailureCount = AtomicInteger(0)

            try {
                val job =
                    execution.runResultAdmissionFailure(
                        operationFailureCount,
                    )
                job.join()

                assertEquals(0, operationFailureCount.get())
                assertTrue(job.isCompleted)
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun handoffChangesProviderContextInsideTheSameJob() =
        runBlocking {
            val workerExecutor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "vex-worker")
                }
            val providerExecutor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "vex-provider")
                }
            val workerDispatcher = workerExecutor.asCoroutineDispatcher()
            val providerDispatcher = providerExecutor.asCoroutineDispatcher()
            val root = VexExecutionRoot(dispatcher = workerDispatcher)
            val execution =
                TestExecution(
                    root = root,
                    repository = TestRepository(),
                    handoffDispatcher = providerDispatcher,
                )
            val capture =
                HandoffCapture(
                    workerThread = CompletableDeferred(),
                    providerThread = CompletableDeferred(),
                )

            try {
                val job = execution.runHandoff(capture)
                job.join()

                assertTrue(capture.workerThread.await().startsWith("vex-worker"))
                assertTrue(capture.providerThread.await().startsWith("vex-provider"))
                assertTrue(job.isCompleted)
            } finally {
                root.joinStopAll("test complete")
                workerDispatcher.close()
                providerDispatcher.close()
                workerExecutor.shutdownNow()
                providerExecutor.shutdownNow()
            }
        }

    @Test
    fun directAuthoredRelationshipAcceptsFullyBoundZeroArgumentMethod() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val owner =
                Any()
            val relationship =
                DirectRelationship(
                    "bound-save",
                )
            val calls =
                mutableListOf<String>()
            val results =
                mutableListOf<String>()
            val method =
                BoundOperation(
                    value =
                        "Dark",
                    calls =
                        calls,
                )

            try {
                val job =
                    VexExecution.begin(
                        executionRoot =
                            root,
                        owner =
                            owner,
                        approach =
                            VexExecution.Approach.Latest,
                        relationshipId =
                            relationship,
                        operation =
                            method::invoke,
                        onResult = { result ->
                            results +=
                                result
                        },
                    )

                job.join()

                assertEquals(
                    listOf(
                        "Dark",
                    ),
                    calls,
                )
                assertEquals(
                    listOf(
                        "Dark",
                    ),
                    results,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun directAuthoredLatestRetainsOwnerScopedCustody() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val repository =
                TestRepository()
            val owner =
                Any()
            val relationship =
                DirectRelationship(
                    "save",
                )

            try {
                VexExecution.begin(
                    executionRoot =
                        root,
                    owner =
                        owner,
                    approach =
                        VexExecution.Approach.Latest,
                    relationshipId =
                        relationship,
                    operation =
                        repository::latestWork,
                    argument =
                        "A",
                )

                withTimeout(
                    2_000,
                ) {
                    while (
                        repository.latestStarted !=
                        listOf(
                            "A",
                        )
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                val latest =
                    VexExecution.begin(
                        executionRoot =
                            root,
                        owner =
                            owner,
                        approach =
                            VexExecution.Approach.Latest,
                        relationshipId =
                            relationship,
                        operation =
                            repository::latestWork,
                        argument =
                            "B",
                    )

                withTimeout(
                    2_000,
                ) {
                    repository.latestCleanupStarted.await()
                }

                assertEquals(
                    listOf(
                        "A",
                    ),
                    repository.latestStarted,
                )

                repository.latestCleanupRelease.complete(
                    Unit,
                )
                latest.join()

                assertEquals(
                    listOf(
                        "A",
                        "B",
                    ),
                    repository.latestStarted,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun rootShutdownClosesAdmissionAndCancelsTheSharedTree() =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val running = execution.runIndependent("A")

            withTimeout(2_000) {
                while (repository.independentStarted.isEmpty()) {
                    kotlinx.coroutines.yield()
                }
            }

            root.joinStopAll("terminal stop")

            assertTrue(running.isCancelled)
            assertFalse(
                runCatching {
                    execution.runOnce()
                }.isSuccess,
            )
        }
}

// [VXG RealForever]
