@file:OptIn(com.vextreme.vexlife.android.execution.VexExecution.RelationshipFoundationApi::class)

package com.vextreme.vexlife.android.execution

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VexExecutionTest {
    private class TestRepository {
        val independentStarted =
            CopyOnWriteArrayList<String>()
        val independentRelease =
            CompletableDeferred<Unit>()
        val earliestStarted =
            CopyOnWriteArrayList<String>()
        val earliestRelease =
            CompletableDeferred<Unit>()
        val latestStarted =
            CopyOnWriteArrayList<String>()
        val latestCleanupStarted =
            CompletableDeferred<Unit>()
        val latestCleanupRelease =
            CompletableDeferred<Unit>()
        val queuedStarted =
            CopyOnWriteArrayList<String>()
        val queuedReleaseA =
            CompletableDeferred<Unit>()
        val queuedReleaseB =
            CompletableDeferred<Unit>()
        val childStarted =
            CompletableDeferred<Unit>()
        val childStopped =
            CompletableDeferred<Unit>()
        val collectAttempts =
            AtomicInteger(0)
        val downstreamSubscriptions =
            AtomicInteger(0)

        suspend fun independentWork(
            value:
                String,
        ) {
            independentStarted +=
                value
            independentRelease.await()
        }

        suspend fun earliestWork(
            value:
                String,
        ) {
            earliestStarted +=
                value
            earliestRelease.await()
        }

        suspend fun latestWork(
            value:
                String,
        ) {
            latestStarted +=
                value

            if (
                value ==
                "A"
            ) {
                try {
                    awaitCancellation()
                } finally {
                    withContext(
                        NonCancellable,
                    ) {
                        latestCleanupStarted.complete(
                            Unit,
                        )
                        latestCleanupRelease.await()
                    }
                }
            }
        }

        suspend fun queuedWork(
            value:
                String,
        ) {
            queuedStarted +=
                value

            when (
                value
            ) {
                "A" ->
                    queuedReleaseA.await()

                "B" ->
                    queuedReleaseB.await()
            }
        }

        suspend fun structuredWork():
            Unit =
            coroutineScope {
                launch {
                    try {
                        childStarted.complete(
                            Unit,
                        )
                        awaitCancellation()
                    } finally {
                        childStopped.complete(
                            Unit,
                        )
                    }
                }
                awaitCancellation()
            }

        suspend fun successfulResult(
            value:
                String,
        ):
            String =
            value

        fun states():
            Flow<Int> =
            flow {
                emit(
                    7,
                )
                awaitCancellation()
            }

        fun restartableStates():
            Flow<Int> =
            flow {
                val attempt =
                    collectAttempts
                        .incrementAndGet()

                if (
                    attempt ==
                    1
                ) {
                    error(
                        "first collector attempt failed",
                    )
                }

                emit(
                    11,
                )
                awaitCancellation()
            }

        fun downstreamFailureStates():
            Flow<Int> =
            flow {
                downstreamSubscriptions
                    .incrementAndGet()
                emit(
                    19,
                )
                awaitCancellation()
            }
    }

    private data class HandoffCapture(
        val workerThread:
            CompletableDeferred<String>,
        val providerThread:
            CompletableDeferred<String>,
    )

    private class TestExecution(
        private val root:
            VexExecutionRoot,
        private val repository:
            TestRepository,
        private val handoffDispatcher:
            CoroutineDispatcher? =
            null,
    ) :
        VexExecution<
            TestExecution
        >(
            root,
        ) {
        enum class Relationship :
            VexExecution.RelationshipId<
                TestExecution
            > {
            IndependentA,
            IndependentB,
            SharedIndependent,
            Earliest,
            Latest,
            Queued,
            Structured,
            ObservationA,
            ObservationB,
            RestartableObservation,
            DownstreamFailureObservation,
            Handoff,
            ResultAdmission,
        }

        fun runIndependent(
            relationship:
                Relationship,
            value:
                String,
        ):
            Boolean =
            launch(
                relationshipId =
                    relationship,
                approach =
                    VexExecution.Approach.Independent,
                action =
                    repository::independentWork,
                argument =
                    value,
            )

        fun runEarliest(
            value:
                String,
        ):
            Boolean =
            launch(
                relationshipId =
                    Relationship.Earliest,
                approach =
                    VexExecution.Approach.Earliest,
                action =
                    repository::earliestWork,
                argument =
                    value,
            )

        fun runLatest(
            value:
                String,
        ):
            Boolean =
            launch(
                relationshipId =
                    Relationship.Latest,
                approach =
                    VexExecution.Approach.Latest,
                action =
                    repository::latestWork,
                argument =
                    value,
            )

        fun runQueued(
            value:
                String,
        ):
            Boolean =
            launch(
                relationshipId =
                    Relationship.Queued,
                approach =
                    VexExecution.Approach.Queued,
                action =
                    repository::queuedWork,
                argument =
                    value,
            )

        fun runStructured():
            Boolean =
            launch(
                relationshipId =
                    Relationship.Structured,
                approach =
                    VexExecution.Approach.Earliest,
                action =
                    repository::structuredWork,
            )

        fun observe(
            relationship:
                Relationship,
            values:
                MutableList<Int>,
        ):
            Boolean =
            launchForCollect(
                relationshipId =
                    relationship,
                source =
                    repository::states,
            ) { value ->
                values +=
                    value
            }

        fun observeRestartable(
            values:
                MutableList<Int>,
        ):
            Boolean =
            launchForCollect(
                relationshipId =
                    Relationship.RestartableObservation,
                source =
                    repository::restartableStates,
            ) { value ->
                values +=
                    value
            }

        fun observeDownstreamFailure(
            callbackCount:
                AtomicInteger,
        ):
            Boolean =
            launchForCollect(
                relationshipId =
                    Relationship.DownstreamFailureObservation,
                source =
                    repository::downstreamFailureStates,
            ) {
                callbackCount
                    .incrementAndGet()
                error(
                    "downstream consumer failed",
                )
            }

        fun runHandoff(
            capture:
                HandoffCapture,
        ):
            Boolean =
            launch(
                relationshipId =
                    Relationship.Handoff,
                approach =
                    VexExecution.Approach.Earliest,
                action =
                    ::handoffForExecution,
                argument =
                    capture,
            )

        private suspend fun handoffForExecution(
            capture:
                HandoffCapture,
        ) {
            capture.workerThread.complete(
                Thread.currentThread().name,
            )
            handoff(
                requireNotNull(
                    handoffDispatcher,
                ),
            ) {
                capture.providerThread.complete(
                    Thread.currentThread().name,
                )
            }
        }

        fun runResultAdmissionFailure(
            operationFailureCount:
                AtomicInteger,
        ):
            Boolean =
            VexExecution.begin(
                executionRoot =
                    root,
                owner =
                    TestExecution::class,
                relationshipId =
                    Relationship.ResultAdmission,
                approach =
                    VexExecution.Approach.Earliest,
                operation =
                    repository::successfulResult,
                argument =
                    "success",
                onResult = {
                    error(
                        "result admission failed",
                    )
                },
                onFailure = {
                    operationFailureCount
                        .incrementAndGet()
                },
            )

        fun stop(
            relationship:
                Relationship,
        ):
            Boolean =
            forceStop(
                relationshipId =
                    relationship,
                reason =
                    "test stop",
            )

        suspend fun joinStop(
            relationship:
                Relationship,
        ):
            Boolean =
            joinStop(
                relationshipId =
                    relationship,
                reason =
                    "test join stop",
            )

        suspend fun await(
            relationship:
                Relationship,
        ):
            Boolean =
            awaitCompletion(
                relationshipId =
                    relationship,
            )
    }

    private suspend fun awaitStatus(
        root:
            VexExecutionRoot,
        relationship:
            TestExecution.Relationship,
        status:
            VexExecution.Status,
    ):
        VexExecution.State =
        withTimeout(
            2_000L,
        ) {
            root.executions.first { executions ->
                executions[
                    relationship
                ]
                    ?.lastOrNull()
                    ?.status ==
                    status
            }[
                relationship
            ]
                .orEmpty()
                .last()
        }

    @Test
    fun executionContractHasNoHandleInstanceIdOrCallableControlSurface() {
        val nestedNames =
            VexExecution::class.java
                .declaredClasses
                .map {
                    it.simpleName
                }
                .toSet()

        assertFalse(
            "Handle" in
                nestedNames,
        )
        assertFalse(
            "InstanceId" in
                nestedNames,
        )

        val executionMethods =
            VexExecution::class.java
                .declaredMethods
                .filterNot {
                    it.isSynthetic
                }

        assertFalse(
            executionMethods.any {
                it.name ==
                    "launchOnce"
            },
        )

        executionMethods
            .filter {
                it.name ==
                    "forceStop" ||
                    it.name ==
                    "joinStop" ||
                    it.name ==
                    "awaitCompletion"
            }
            .forEach { method ->
                assertEquals(
                    VexExecution.RelationshipId::class.java,
                    method.parameterTypes.first(),
                )
            }

        val beginMethods =
            VexExecution.Companion::class.java
                .declaredMethods
                .filterNot {
                    it.isSynthetic
                }
                .filter {
                    it.name.startsWith(
                        "begin",
                    )
                }

        assertEquals(
            listOf(
                "begin",
            ),
            beginMethods
                .map {
                    it.name
                }
                .distinct()
                .sorted(),
        )
        assertTrue(
            beginMethods.all { method ->
                method.parameterTypes.contains(
                    VexExecution.RelationshipId::class.java,
                )
            },
        )
    }

    @Test
    fun sameCallableUnderDifferentRelationshipKeysRemainsIndependent(): Unit =
        runBlocking {
            val root =
                VexExecutionRoot(
                    dispatcher =
                        Dispatchers.Default,
                )
            val repository =
                TestRepository()
            val execution =
                TestExecution(
                    root,
                    repository,
                )

            try {
                assertTrue(
                    execution.runIndependent(
                        TestExecution.Relationship.IndependentA,
                        "A",
                    ),
                )
                assertTrue(
                    execution.runIndependent(
                        TestExecution.Relationship.IndependentB,
                        "B",
                    ),
                )

                awaitStatus(
                    root,
                    TestExecution.Relationship.IndependentA,
                    VexExecution.Status.RUNNING,
                )
                awaitStatus(
                    root,
                    TestExecution.Relationship.IndependentB,
                    VexExecution.Status.RUNNING,
                )

                val firstSource =
                    root.executions.value[
                        TestExecution.Relationship.IndependentA
                    ]
                        .orEmpty()
                        .last()
                        .sourceRef
                val secondSource =
                    root.executions.value[
                        TestExecution.Relationship.IndependentB
                    ]
                        .orEmpty()
                        .last()
                        .sourceRef

                assertEquals(
                    firstSource,
                    secondSource,
                )

                assertTrue(
                    execution.stop(
                        TestExecution.Relationship.IndependentA,
                    ),
                )

                awaitStatus(
                    root,
                    TestExecution.Relationship.IndependentA,
                    VexExecution.Status.CANCELLED,
                )

                assertEquals(
                    VexExecution.Status.RUNNING,
                    root.executions.value[
                        TestExecution.Relationship.IndependentB
                    ]
                        .orEmpty()
                        .last()
                        .status,
                )

                repository.independentRelease.complete(
                    Unit,
                )
                execution.await(
                    TestExecution.Relationship.IndependentB,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun independentApproachAllowsMultipleJobsWithinOneRelationship(): Unit =
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

            try {
                assertTrue(
                    execution.runIndependent(
                        TestExecution.Relationship.SharedIndependent,
                        "A",
                    ),
                )
                assertTrue(
                    execution.runIndependent(
                        TestExecution.Relationship.SharedIndependent,
                        "B",
                    ),
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        repository.independentStarted.size <
                        2
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(
                    2,
                    root.executions.value[
                        TestExecution.Relationship.SharedIndependent
                    ]
                        .orEmpty()
                        .count {
                            it.status ==
                                VexExecution.Status.RUNNING
                        },
                )

                repository.independentRelease.complete(
                    Unit,
                )
                execution.await(
                    TestExecution.Relationship.SharedIndependent,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun earliestApproachKeepsTheExistingRelationshipJob(): Unit =
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

            try {
                assertTrue(
                    execution.runEarliest(
                        "A",
                    ),
                )
                awaitStatus(
                    root,
                    TestExecution.Relationship.Earliest,
                    VexExecution.Status.RUNNING,
                )

                assertFalse(
                    execution.runEarliest(
                        "B",
                    ),
                )
                assertEquals(
                    listOf(
                        "A",
                    ),
                    repository.earliestStarted
                        .toList(),
                )

                repository.earliestRelease.complete(
                    Unit,
                )
                execution.await(
                    TestExecution.Relationship.Earliest,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun latestApproachWaitsForSupersededCleanupBeforeStartingReplacement(): Unit =
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

            try {
                assertTrue(
                    execution.runLatest(
                        "A",
                    ),
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        !repository.latestStarted
                            .contains(
                                "A",
                            )
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertTrue(
                    execution.runLatest(
                        "B",
                    ),
                )

                withTimeout(
                    2_000L,
                ) {
                    repository.latestCleanupStarted
                        .await()
                }

                assertEquals(
                    listOf(
                        "A",
                    ),
                    repository.latestStarted
                        .toList(),
                )

                repository.latestCleanupRelease.complete(
                    Unit,
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        !repository.latestStarted
                            .contains(
                                "B",
                            )
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                execution.await(
                    TestExecution.Relationship.Latest,
                )

                assertEquals(
                    listOf(
                        "A",
                        "B",
                    ),
                    repository.latestStarted
                        .toList(),
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun queuedApproachPreservesEveryAcceptedJobInArrivalOrder(): Unit =
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

            try {
                assertTrue(
                    execution.runQueued(
                        "A",
                    ),
                )
                assertTrue(
                    execution.runQueued(
                        "B",
                    ),
                )
                assertTrue(
                    execution.runQueued(
                        "C",
                    ),
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        repository.queuedStarted
                            .isEmpty()
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(
                    listOf(
                        "A",
                    ),
                    repository.queuedStarted
                        .toList(),
                )

                repository.queuedReleaseA.complete(
                    Unit,
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        repository.queuedStarted.size <
                        2
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                repository.queuedReleaseB.complete(
                    Unit,
                )

                execution.await(
                    TestExecution.Relationship.Queued,
                )

                assertEquals(
                    listOf(
                        "A",
                        "B",
                        "C",
                    ),
                    repository.queuedStarted
                        .toList(),
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun sameFlowSourceCanBeObservedByDifferentRelationshipKeys(): Unit =
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
            val first =
                CopyOnWriteArrayList<Int>()
            val second =
                CopyOnWriteArrayList<Int>()

            try {
                assertTrue(
                    execution.observe(
                        TestExecution.Relationship.ObservationA,
                        first,
                    ),
                )
                assertTrue(
                    execution.observe(
                        TestExecution.Relationship.ObservationB,
                        second,
                    ),
                )
                assertFalse(
                    execution.observe(
                        TestExecution.Relationship.ObservationA,
                        first,
                    ),
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        first.isEmpty() ||
                        second.isEmpty()
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(
                    listOf(
                        7,
                    ),
                    first.toList(),
                )
                assertEquals(
                    listOf(
                        7,
                    ),
                    second.toList(),
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun relationshipJoinStopWaitsForStructuredDescendants(): Unit =
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

            try {
                assertTrue(
                    execution.runStructured(),
                )

                withTimeout(
                    2_000L,
                ) {
                    repository.childStarted.await()
                }

                assertTrue(
                    execution.joinStop(
                        TestExecution.Relationship.Structured,
                    ),
                )

                assertTrue(
                    repository.childStopped
                        .isCompleted,
                )
                assertEquals(
                    VexExecution.Status.CANCELLED,
                    root.executions.value[
                        TestExecution.Relationship.Structured
                    ]
                        .orEmpty()
                        .last()
                        .status,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun flowSourceFailureCanRecoverWithoutChangingRelationshipIdentity(): Unit =
        runBlocking {
            val root =
                VexExecutionRoot(
                    maxCollectRestarts =
                        2,
                    collectRestartDelayMillis =
                        0L,
                )
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
                assertTrue(
                    execution.observeRestartable(
                        values,
                    ),
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        !values.contains(
                            11,
                        )
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(
                    2,
                    repository.collectAttempts
                        .get(),
                )
                assertEquals(
                    2,
                    root.executions.value[
                        TestExecution.Relationship.RestartableObservation
                    ]
                        .orEmpty()
                        .last()
                        .attempt,
                )
                assertEquals(
                    1,
                    root.executions.value[
                        TestExecution.Relationship.RestartableObservation
                    ]
                        .orEmpty()
                        .last()
                        .restartCount,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun downstreamConsumerFailureDoesNotBecomeSourceRecovery(): Unit =
        runBlocking {
            val root =
                VexExecutionRoot(
                    maxCollectRestarts =
                        5,
                    collectRestartDelayMillis =
                        0L,
                )
            val repository =
                TestRepository()
            val execution =
                TestExecution(
                    root,
                    repository,
                )
            val callbackCount =
                AtomicInteger(
                    0,
                )

            try {
                assertTrue(
                    execution.observeDownstreamFailure(
                        callbackCount,
                    ),
                )

                val failed =
                    awaitStatus(
                        root,
                        TestExecution.Relationship.DownstreamFailureObservation,
                        VexExecution.Status.FAILED,
                    )

                assertEquals(
                    1,
                    repository.downstreamSubscriptions
                        .get(),
                )
                assertEquals(
                    1,
                    callbackCount.get(),
                )
                assertEquals(
                    0,
                    failed.restartCount,
                )
                assertEquals(
                    "IllegalStateException",
                    failed.failureClass,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun resultAdmissionFailureDoesNotBecomeOperationFailure(): Unit =
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
            val operationFailureCount =
                AtomicInteger(
                    0,
                )

            try {
                assertTrue(
                    execution.runResultAdmissionFailure(
                        operationFailureCount,
                    ),
                )

                val failed =
                    awaitStatus(
                        root,
                        TestExecution.Relationship.ResultAdmission,
                        VexExecution.Status.FAILED,
                    )

                assertEquals(
                    0,
                    operationFailureCount.get(),
                )
                assertEquals(
                    "result admission failed",
                    failed.failureMessage,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun handoffChangesProviderContextWithoutChangingRelationship(): Unit =
        runBlocking {
            val workerExecutor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(
                        runnable,
                        "vex-worker",
                    )
                }
            val providerExecutor =
                Executors.newSingleThreadExecutor { runnable ->
                    Thread(
                        runnable,
                        "vex-provider",
                    )
                }
            val workerDispatcher =
                workerExecutor.asCoroutineDispatcher()
            val providerDispatcher =
                providerExecutor.asCoroutineDispatcher()
            val root =
                VexExecutionRoot(
                    dispatcher =
                        workerDispatcher,
                )
            val execution =
                TestExecution(
                    root =
                        root,
                    repository =
                        TestRepository(),
                    handoffDispatcher =
                        providerDispatcher,
                )
            val capture =
                HandoffCapture(
                    workerThread =
                        CompletableDeferred(),
                    providerThread =
                        CompletableDeferred(),
                )

            try {
                assertTrue(
                    execution.runHandoff(
                        capture,
                    ),
                )

                awaitStatus(
                    root,
                    TestExecution.Relationship.Handoff,
                    VexExecution.Status.COMPLETED,
                )

                assertTrue(
                    capture.workerThread.await()
                        .startsWith(
                            "vex-worker",
                        ),
                )
                assertTrue(
                    capture.providerThread.await()
                        .startsWith(
                            "vex-provider",
                        ),
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
                workerDispatcher.close()
                providerDispatcher.close()
                workerExecutor.shutdownNow()
                providerExecutor.shutdownNow()
            }
        }

    @Test
    fun rootShutdownClosesAdmissionBeforeCancellation(): Unit =
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

            assertTrue(
                execution.runIndependent(
                    TestExecution.Relationship.IndependentA,
                    "A",
                ),
            )

            awaitStatus(
                root,
                TestExecution.Relationship.IndependentA,
                VexExecution.Status.RUNNING,
            )

            root.joinStopAll(
                "terminal test stop",
            )

            assertEquals(
                VexExecution.Status.CANCELLED,
                root.executions.value[
                    TestExecution.Relationship.IndependentA
                ]
                    .orEmpty()
                    .last()
                    .status,
            )

            val launchAccepted =
                runCatching {
                    execution.runIndependent(
                        TestExecution.Relationship.IndependentB,
                        "B",
                    )
                }.isSuccess

            assertFalse(
                launchAccepted,
            )
        }
}

// [VXG RealForever]
