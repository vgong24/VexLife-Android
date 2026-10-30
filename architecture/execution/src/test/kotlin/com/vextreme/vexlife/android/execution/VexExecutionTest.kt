package com.vextreme.vexlife.android.execution

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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

        val collectAttempts =
            AtomicInteger(
                0,
            )
        val downstreamSubscriptions =
            AtomicInteger(
                0,
            )

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

            when (value) {
                "A" ->
                    queuedReleaseA.await()

                "B" ->
                    queuedReleaseB.await()
            }
        }

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
                        "first source attempt failed",
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

    private class TestExecution(
        root:
            VexExecutionRoot,
        private val repository:
            TestRepository,
    ) :
        VexExecution(
            root,
        ) {
        private data object EarliestWork :
            CoordinationKey

        private data object LatestWork :
            CoordinationKey

        private data object QueuedWork :
            CoordinationKey

        fun runIndependent(
            value:
                String,
        ):
            Handle =
            launch(
                repository::independentWork,
                value,
            )

        fun runOnce(
            value:
                String,
        ):
            Handle =
            launchOnce(
                repository::independentWork,
                value,
            )

        fun runEarliest(
            value:
                String,
        ):
            Handle =
            launchEarliest(
                key =
                    EarliestWork,
                action =
                    repository::earliestWork,
                argument =
                    value,
            )

        fun runLatest(
            value:
                String,
        ):
            Handle =
            launchLatest(
                key =
                    LatestWork,
                action =
                    repository::latestWork,
                argument =
                    value,
            )

        fun runQueued(
            value:
                String,
        ):
            Handle =
            launchQueued(
                key =
                    QueuedWork,
                action =
                    repository::queuedWork,
                argument =
                    value,
            )

        fun observeStates(
            values:
                MutableList<Int>,
        ):
            Handle =
            observe(
                repository::states,
            ) { value ->
                values +=
                    value
            }

        fun observeRestartable(
            values:
                MutableList<Int>,
        ):
            Handle =
            observe(
                repository::restartableStates,
            ) { value ->
                values +=
                    value
            }

        fun observeDownstreamFailure(
            callbackCount:
                AtomicInteger,
        ):
            Handle =
            observe(
                repository::downstreamFailureStates,
            ) {
                callbackCount
                    .incrementAndGet()
                error(
                    "downstream consumer failed",
                )
            }

        fun stopIndependent(
            reason:
                String,
        ):
            Boolean =
            forceStop(
                repository::independentWork,
                reason,
            )

        suspend fun stopOwner(
            reason:
                String,
        ):
            Boolean =
            joinStopAll(
                reason,
            )
    }

    @Test
    fun sameDiagnosticSourceAcrossTwoLiveOwnersDoesNotCollide() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val firstRepository =
                TestRepository()
            val secondRepository =
                TestRepository()
            val first =
                TestExecution(
                    root,
                    firstRepository,
                )
            val second =
                TestExecution(
                    root,
                    secondRepository,
                )

            try {
                val firstHandle =
                    first.runOnce(
                        "first",
                    )
                val secondHandle =
                    second.runOnce(
                        "second",
                    )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        firstRepository
                            .independentStarted
                            .isEmpty() ||
                        secondRepository
                            .independentStarted
                            .isEmpty()
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(
                    firstHandle.sourceRef,
                    secondHandle.sourceRef,
                )
                assertFalse(
                    firstHandle.instanceId ==
                        secondHandle.instanceId,
                )

                firstRepository
                    .independentRelease
                    .complete(
                        Unit,
                    )
                secondRepository
                    .independentRelease
                    .complete(
                        Unit,
                    )

                firstHandle.awaitCompletion()
                secondHandle.awaitCompletion()
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun sourceCancellationIsScopedToExactLiveOwner() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val firstRepository =
                TestRepository()
            val secondRepository =
                TestRepository()
            val first =
                TestExecution(
                    root,
                    firstRepository,
                )
            val second =
                TestExecution(
                    root,
                    secondRepository,
                )

            try {
                val firstHandle =
                    first.runIndependent(
                        "first",
                    )
                val secondHandle =
                    second.runIndependent(
                        "second",
                    )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        firstRepository
                            .independentStarted
                            .isEmpty() ||
                        secondRepository
                            .independentStarted
                            .isEmpty()
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertTrue(
                    first.stopIndependent(
                        "first owner stop",
                    ),
                )

                firstHandle.awaitCompletion()

                assertEquals(
                    VexExecution.Status.CANCELLED,
                    firstHandle.state.value.status,
                )
                assertEquals(
                    VexExecution.Status.RUNNING,
                    secondHandle.state.value.status,
                )

                secondRepository
                    .independentRelease
                    .complete(
                        Unit,
                    )
                secondHandle.awaitCompletion()
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun duplicateObservationFailsOnlyWithinSameLiveOwner() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val repository =
                TestRepository()
            val first =
                TestExecution(
                    root,
                    repository,
                )
            val second =
                TestExecution(
                    root,
                    repository,
                )
            val firstValues =
                mutableListOf<Int>()
            val secondValues =
                mutableListOf<Int>()

            try {
                val firstHandle =
                    first.observeStates(
                        firstValues,
                    )

                withTimeout(
                    2_000L,
                ) {
                    firstHandle.state.first { state ->
                        state.status ==
                            VexExecution.Status.RUNNING
                    }
                }

                val duplicateAccepted =
                    runCatching {
                        first.observeStates(
                            firstValues,
                        )
                    }.isSuccess

                assertFalse(
                    duplicateAccepted,
                )

                val secondHandle =
                    second.observeStates(
                        secondValues,
                    )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        firstValues.isEmpty() ||
                        secondValues.isEmpty()
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(
                    listOf(
                        7,
                    ),
                    firstValues,
                )
                assertEquals(
                    listOf(
                        7,
                    ),
                    secondValues,
                )

                firstHandle.joinStop(
                    "test complete",
                )
                secondHandle.joinStop(
                    "test complete",
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun earliestReturnsExistingOwnerLocalCoordinationInstance() =
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
                val first =
                    execution.runEarliest(
                        "A",
                    )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        repository
                            .earliestStarted
                            .isEmpty()
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                val second =
                    execution.runEarliest(
                        "B",
                    )

                assertEquals(
                    first.instanceId,
                    second.instanceId,
                )
                assertEquals(
                    listOf(
                        "A",
                    ),
                    repository
                        .earliestStarted
                        .toList(),
                )

                repository
                    .earliestRelease
                    .complete(
                        Unit,
                    )
                first.awaitCompletion()
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun latestWaitsForPredecessorCleanupBeforeNewestStarts() =
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
                execution.runLatest(
                    "A",
                )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        !repository
                            .latestStarted
                            .contains(
                                "A",
                            )
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                execution.runLatest(
                    "B",
                )

                withTimeout(
                    2_000L,
                ) {
                    repository
                        .latestCleanupStarted
                        .await()
                }

                val latest =
                    execution.runLatest(
                        "C",
                    )

                kotlinx.coroutines.yield()

                assertEquals(
                    listOf(
                        "A",
                    ),
                    repository
                        .latestStarted
                        .toList(),
                )

                repository
                    .latestCleanupRelease
                    .complete(
                        Unit,
                    )

                withTimeout(
                    2_000L,
                ) {
                    latest.awaitCompletion()
                }

                assertEquals(
                    listOf(
                        "A",
                        "C",
                    ),
                    repository
                        .latestStarted
                        .toList(),
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun queuedPreservesEveryOwnerLocalLaunchInArrivalOrder() =
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
                val first =
                    execution.runQueued(
                        "A",
                    )
                val second =
                    execution.runQueued(
                        "B",
                    )
                val third =
                    execution.runQueued(
                        "C",
                    )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        repository
                            .queuedStarted
                            .isEmpty()
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(
                    listOf(
                        "A",
                    ),
                    repository
                        .queuedStarted
                        .toList(),
                )

                repository
                    .queuedReleaseA
                    .complete(
                        Unit,
                    )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        repository
                            .queuedStarted
                            .size <
                        2
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(
                    listOf(
                        "A",
                        "B",
                    ),
                    repository
                        .queuedStarted
                        .toList(),
                )

                repository
                    .queuedReleaseB
                    .complete(
                        Unit,
                    )

                first.awaitCompletion()
                second.awaitCompletion()
                third.awaitCompletion()

                assertEquals(
                    listOf(
                        "A",
                        "B",
                        "C",
                    ),
                    repository
                        .queuedStarted
                        .toList(),
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun sourceFailureMayRestartButDownstreamFailureDoesNot() =
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
                mutableListOf<Int>()

            try {
                val restartable =
                    execution.observeRestartable(
                        values,
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
                    repository
                        .collectAttempts
                        .get(),
                )
                assertEquals(
                    1,
                    restartable
                        .state
                        .value
                        .restartCount,
                )

                restartable.joinStop(
                    "restart proof complete",
                )

                val callbacks =
                    AtomicInteger(
                        0,
                    )
                val downstream =
                    execution.observeDownstreamFailure(
                        callbacks,
                    )

                withTimeout(
                    2_000L,
                ) {
                    downstream.awaitCompletion()
                }

                assertEquals(
                    1,
                    repository
                        .downstreamSubscriptions
                        .get(),
                )
                assertEquals(
                    1,
                    callbacks.get(),
                )
                assertEquals(
                    VexExecution.Status.FAILED,
                    downstream
                        .state
                        .value
                        .status,
                )
                assertEquals(
                    0,
                    downstream
                        .state
                        .value
                        .restartCount,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun ownerShutdownDoesNotCloseSharedRoot() =
        runBlocking {
            val root =
                VexExecutionRoot()
            val firstRepository =
                TestRepository()
            val secondRepository =
                TestRepository()
            val first =
                TestExecution(
                    root,
                    firstRepository,
                )
            val second =
                TestExecution(
                    root,
                    secondRepository,
                )

            try {
                val firstHandle =
                    first.runIndependent(
                        "first",
                    )
                val secondHandle =
                    second.runIndependent(
                        "second",
                    )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        firstRepository
                            .independentStarted
                            .isEmpty() ||
                        secondRepository
                            .independentStarted
                            .isEmpty()
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertTrue(
                    first.stopOwner(
                        "first owner shutdown",
                    ),
                )
                assertEquals(
                    VexExecution.Status.CANCELLED,
                    firstHandle.state.value.status,
                )
                assertEquals(
                    VexExecution.Status.RUNNING,
                    secondHandle.state.value.status,
                )

                secondRepository
                    .independentRelease
                    .complete(
                        Unit,
                    )
                secondHandle.awaitCompletion()

                val third =
                    second.runIndependent(
                        "third",
                    )
                secondRepository
                    .independentRelease
                    .complete(
                        Unit,
                    )
                third.awaitCompletion()
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun rootShutdownClosesAdmissionForEveryOwner() =
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
            val running =
                execution.runIndependent(
                    "running",
                )

            withTimeout(
                2_000L,
            ) {
                while (
                    repository
                        .independentStarted
                        .isEmpty()
                ) {
                    kotlinx.coroutines.yield()
                }
            }

            root.joinStopAll(
                "terminal test stop",
            )

            assertEquals(
                VexExecution.Status.CANCELLED,
                running.state.value.status,
            )

            assertFalse(
                runCatching {
                    execution.runIndependent(
                        "rejected",
                    )
                }.isSuccess,
            )
        }
}

// [VXG RealForever]
