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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VexExecutionTest {
    private class TestRepository {
        var onceCount = 0
        var argumentTotal = 0
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

        suspend fun add(value: Int) {
            argumentTotal += value
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

        suspend fun queuedWork(value: String) {
            queuedStarted += value

            when (value) {
                "A" ->
                    queuedReleaseA.await()

                "B" ->
                    queuedReleaseB.await()
            }
        }

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

        val propertyStates: Flow<Int>
            get() =
                flow {
                    emit(13)
                    awaitCancellation()
                }

        fun failedStates(): Flow<Int> =
            flow {
                error("collector failed")
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
            val attempt =
                sourceCreationAttempts
                    .incrementAndGet()

            if (attempt == 1) {
                error(
                    "source creation failed",
                )
            }

            return flow {
                emit(17)
                awaitCancellation()
            }
        }

        fun downstreamFailureStates(): Flow<Int> =
            flow {
                downstreamSourceSubscriptions
                    .incrementAndGet()
                emit(19)
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
        private val root: VexExecutionRoot,
        private val repository: TestRepository,
        private val handoffDispatcher:
            CoroutineDispatcher? =
            null,
    ) : VexExecution(root) {
        /**
         * Engine-only fixture. Semantic/product authoring must obtain RelationshipId from its
         * rightful typed owner (for Direct Family: Control -> ResourceId -> Family RelationshipId).
         */
        private data class EngineRelationshipFixture(
            val semanticRef:
                String,
        ) :
            VexExecution.RelationshipId

        private val refreshRelationship =
            EngineRelationshipFixture(
                semanticRef =
                    "test.refresh",
            )

        private val searchRelationship =
            EngineRelationshipFixture(
                semanticRef =
                    "test.search",
            )

        private val uploadRelationship =
            EngineRelationshipFixture(
                semanticRef =
                    "test.upload",
            )

        fun runOnce(): VexCoroutineHandle =
            launchOnce(repository::once)

        fun runWithArgument(value: Int): VexCoroutineHandle =
            launchOnce(
                repository::add,
                value,
            )

        fun runIndependent(value: String): VexExecution.Handle =
            launch(
                repository::independentWork,
                value,
            )

        fun runExclusive(value: String): VexExecution.Handle =
            launchOnce(
                repository::independentWork,
                value,
            )

        fun runEarliest(value: String): VexExecution.Handle =
            VexExecution.beginEarliest(
                executionRoot =
                    root,
                owner =
                    TestExecution::class,
                relationshipId =
                    refreshRelationship,
                operation =
                    repository::earliestWork,
                argument =
                    value,
            )

        fun runLatest(value: String): VexExecution.Handle =
            VexExecution.beginLatest(
                executionRoot =
                    root,
                owner =
                    TestExecution::class,
                relationshipId =
                    searchRelationship,
                operation =
                    repository::latestWork,
                argument =
                    value,
            )

        fun runQueued(value: String): VexExecution.Handle =
            VexExecution.beginQueued(
                executionRoot =
                    root,
                owner =
                    TestExecution::class,
                relationshipId =
                    uploadRelationship,
                operation =
                    repository::queuedWork,
                argument =
                    value,
            )

        fun runStructured(): VexCoroutineHandle =
            launchOnce(repository::structuredWork)

        fun runHandoff(
            capture: HandoffCapture,
        ): VexCoroutineHandle =
            launch(
                ::handoffForExecution,
                capture,
            )

        private suspend fun handoffForExecution(
            capture: HandoffCapture,
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

        fun observeStates(
            values: MutableList<Int>,
        ): VexCoroutineHandle =
            launchForCollect(
                repository::states,
            ) { value ->
                values += value
            }

        fun observeSecondary(
            values: MutableList<Int>,
        ): VexCoroutineHandle =
            launchForCollect(
                repository::secondaryStates,
            ) { value ->
                values += value
            }

        fun observeProperty(
            values: MutableList<Int>,
        ): VexCoroutineHandle =
            launchForCollect(
                repository::propertyStates,
            ) { value ->
                values += value
            }

        fun observeFailed(): VexCoroutineHandle =
            launchForCollect(
                source = repository::failedStates,
                restartOnSourceFailure = false,
            ) {}

        fun observeSourceCreation(
            values: MutableList<Int>,
        ): VexCoroutineHandle =
            launchForCollect(
                repository::sourceCreationStates,
            ) { value ->
                values += value
            }

        fun observeDownstreamFailure(
            callbackCount: AtomicInteger,
        ): VexCoroutineHandle =
            launchForCollect(
                repository::downstreamFailureStates,
            ) {
                callbackCount
                    .incrementAndGet()
                error(
                    "downstream consumer failed",
                )
            }

        fun observeRestartable(
            values: MutableList<Int>,
        ): VexCoroutineHandle =
            launchForCollect(
                repository::restartableStates,
            ) { value ->
                values += value
            }

        fun observeRestartDelay(): VexCoroutineHandle =
            launchForCollect(
                repository::restartDelayStates,
            ) {}

        fun forceStopRestartDelay(): Boolean =
            forceStop(repository::restartDelayStates)
    }

    @Test
    fun launchOnceInvokesReferencedSuspendFunctionExactlyOnce(): Unit =
        runBlocking {
            val root =
                VexExecutionRoot(
                    dispatcher = Dispatchers.Default,
                )
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                val handle = execution.runOnce()
                handle.awaitCompletion()

                assertEquals(1, repository.onceCount)
                assertEquals(
                    VexCoroutineStatus.COMPLETED,
                    handle.state.value.status,
                )
                assertTrue(
                    handle.sourceRef.value.endsWith(
                        ".TestExecution.once",
                    ),
                )
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun launchOnceKeepsOneArgumentInvocationTypeSafe(): Unit =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                execution
                    .runWithArgument(4)
                    .awaitCompletion()

                assertEquals(4, repository.argumentTotal)
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun handoffSwitchesProviderContextWithoutCreatingAnotherExecution(): Unit =
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
                    root = root,
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
                val handle =
                    execution.runHandoff(
                        capture,
                    )
                handle.awaitCompletion()

                val workerThread =
                    capture.workerThread.await()
                val providerThread =
                    capture.providerThread.await()

                assertTrue(
                    workerThread.startsWith(
                        "vex-worker",
                    ),
                )
                assertTrue(
                    providerThread.startsWith(
                        "vex-provider",
                    ),
                )
                assertFalse(
                    workerThread ==
                        providerThread,
                )
                assertEquals(
                    VexCoroutineStatus.COMPLETED,
                    handle.state.value.status,
                )
                assertEquals(
                    1,
                    root.executions.value
                        .keys
                        .count {
                            it ==
                                handle.instanceId
                        },
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
    fun launchForCollectInvokesProviderAndOwnsCollection(): Unit =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val values = CopyOnWriteArrayList<Int>()

            try {
                val handle = execution.observeStates(values)

                withTimeout(2_000L) {
                    while (values.isEmpty()) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(listOf(7), values)
                assertEquals(
                    VexCoroutineStatus.RUNNING,
                    handle.state.value.status,
                )
                assertTrue(
                    handle.joinStop(
                        "test stop",
                    ),
                )
                assertEquals(
                    VexCoroutineStatus.CANCELLED,
                    handle.state.value.status,
                )
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun launchForCollectAcceptsFlowPropertyReference(): Unit =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val values = CopyOnWriteArrayList<Int>()

            try {
                val handle =
                    execution.observeProperty(values)

                withTimeout(2_000L) {
                    while (values.isEmpty()) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(listOf(13), values)
                handle.joinStop("test complete")
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun joinStopWaitsForStructuredDescendantsToFinish(): Unit =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                val handle = execution.runStructured()

                withTimeout(2_000L) {
                    repository.childStarted.await()
                }

                assertTrue(
                    handle.joinStop(
                        "replace structured work",
                    ),
                )

                assertTrue(repository.childStopped.isCompleted)
                assertEquals(
                    VexCoroutineStatus.CANCELLED,
                    handle.state.value.status,
                )
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun failedExecutionDoesNotCancelSiblingExecution(): Unit =
        runBlocking {
            val root =
                VexExecutionRoot(
                    maxCollectRestarts = 0,
                )
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val values = CopyOnWriteArrayList<Int>()

            try {
                val healthy = execution.observeSecondary(values)
                val failed = execution.observeFailed()

                failed.awaitCompletion()
                withTimeout(2_000L) {
                    while (values.isEmpty()) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(
                    VexCoroutineStatus.RUNNING,
                    healthy.state.value.status,
                )
                assertEquals(
                    VexCoroutineStatus.FAILED,
                    failed.state.value.status,
                )
                assertEquals(listOf(9), values)

                healthy.joinStop("test complete")
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun launchForCollectCanRecoverWithBoundedRestart(): Unit =
        runBlocking {
            val root =
                VexExecutionRoot(
                    maxCollectRestarts = 2,
                    collectRestartDelayMillis = 0L,
                )
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val values = CopyOnWriteArrayList<Int>()

            try {
                val handle = execution.observeRestartable(values)

                withTimeout(2_000L) {
                    handle.state.first { state ->
                        state.status ==
                            VexCoroutineStatus.RUNNING &&
                            state.attempt == 2
                    }

                    /*
                     * RUNNING means the recovered attempt has started; it does not mean the emitted
                     * value has already crossed the downstream consumer boundary. Wait for the
                     * observable effect this assertion actually claims rather than racing on status.
                     */
                    while (!values.contains(11)) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(
                    2,
                    repository.collectAttempts.get(),
                )
                assertEquals(listOf(11), values.toList())
                assertEquals(
                    2,
                    handle.state.value.attempt,
                )
                assertEquals(
                    1,
                    handle.state.value.restartCount,
                )

                handle.joinStop("test complete")
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun sourceCreationFailureCanRecover(): Unit =
        runBlocking {
            val root =
                VexExecutionRoot(
                    maxCollectRestarts = 2,
                    collectRestartDelayMillis = 0L,
                )
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val values = CopyOnWriteArrayList<Int>()

            try {
                val handle =
                    execution.observeSourceCreation(
                        values,
                    )

                withTimeout(2_000L) {
                    handle.state.first { state ->
                        state.status ==
                            VexCoroutineStatus.RUNNING &&
                            state.attempt == 2
                    }

                    while (!values.contains(17)) {
                        kotlinx.coroutines.yield()
                    }
                }

                assertEquals(
                    2,
                    repository
                        .sourceCreationAttempts
                        .get(),
                )
                assertEquals(
                    listOf(17),
                    values.toList(),
                )
                assertEquals(
                    1,
                    handle.state.value
                        .restartCount,
                )

                handle.joinStop(
                    "test complete",
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun downstreamOnEachFailureDoesNotAutoRestart(): Unit =
        runBlocking {
            val root =
                VexExecutionRoot(
                    maxCollectRestarts = 5,
                    collectRestartDelayMillis = 0L,
                )
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val callbackCount =
                AtomicInteger(0)

            try {
                val handle =
                    execution.observeDownstreamFailure(
                        callbackCount,
                    )

                withTimeout(2_000L) {
                    handle.awaitCompletion()
                }

                assertEquals(
                    1,
                    repository
                        .downstreamSourceSubscriptions
                        .get(),
                )
                assertEquals(
                    1,
                    callbackCount.get(),
                )
                assertEquals(
                    VexCoroutineStatus.FAILED,
                    handle.state.value.status,
                )
                assertEquals(
                    0,
                    handle.state.value.restartCount,
                )
                assertEquals(
                    "IllegalStateException",
                    handle.state.value.failureClass,
                )
                assertEquals(
                    "downstream consumer failed",
                    handle.state.value.failureMessage,
                )
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun forceStopDuringRestartDelaySuppressesRestart(): Unit =
        runBlocking {
            val root =
                VexExecutionRoot(
                    maxCollectRestarts = 5,
                    collectRestartDelayMillis =
                        60_000L,
                )
            val repository = TestRepository()
            val execution = TestExecution(root, repository)

            try {
                val handle =
                    execution.observeRestartDelay()

                withTimeout(2_000L) {
                    handle.state.first { state ->
                        state.status ==
                            VexCoroutineStatus
                                .RESTARTING
                    }
                }

                assertTrue(
                    execution
                        .forceStopRestartDelay(),
                )
                handle.awaitCompletion()

                assertEquals(
                    1,
                    repository.collectAttempts.get(),
                )
                assertEquals(
                    VexCoroutineStatus.CANCELLED,
                    handle.state.value.status,
                )
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun duplicateActiveSourceRefFailsClosed(): Unit =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val values = CopyOnWriteArrayList<Int>()

            try {
                val first =
                    execution.observeStates(values)

                withTimeout(2_000L) {
                    first.state.first { state ->
                        state.status ==
                            VexCoroutineStatus.RUNNING
                    }
                }

                val duplicateAccepted =
                    runCatching {
                        execution
                            .observeStates(values)
                    }.isSuccess

                assertFalse(duplicateAccepted)
                first.joinStop("test complete")
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun registryPreservesAdmissionOrderAndRejectsExternalMutation(): Unit =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val values = CopyOnWriteArrayList<Int>()

            try {
                val first =
                    execution.observeStates(values)
                val second =
                    execution.observeSecondary(values)

                withTimeout(2_000L) {
                    first.state.first {
                        it.status ==
                            VexCoroutineStatus.RUNNING
                    }
                    second.state.first {
                        it.status ==
                            VexCoroutineStatus.RUNNING
                    }
                }

                assertEquals(
                    listOf(
                        first.instanceId,
                        second.instanceId,
                    ),
                    root.executions
                        .value
                        .keys
                        .toList(),
                )

                @Suppress("UNCHECKED_CAST")
                val exposed =
                    root.executions.value
                        as MutableMap<
                            VexExecution.InstanceId,
                            VexExecution.State
                        >

                val mutationAccepted =
                    runCatching {
                        exposed.clear()
                    }.isSuccess

                assertFalse(mutationAccepted)
                assertNotNull(
                    root.executions.value[
                        first.instanceId
                    ],
                )

                first.joinStop("test complete")
                second.joinStop("test complete")
            } finally {
                root.joinStopAll("test complete")
            }
        }

    @Test
    fun launchAdmitsIndependentInstancesForTheSameCallable(): Unit =
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
                val first =
                    execution.runIndependent(
                        "A",
                    )
                val second =
                    execution.runIndependent(
                        "B",
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

                assertFalse(
                    first.instanceId ==
                        second.instanceId,
                )
                assertEquals(
                    first.sourceRef,
                    second.sourceRef,
                )

                repository.independentRelease.complete(
                    Unit,
                )
                first.awaitCompletion()
                second.awaitCompletion()
            } finally {
                root.joinStopAll(
                    "test complete",
                )
            }
        }

    @Test
    fun launchOnceRejectsASecondUnfinishedInvocationFromTheSameSource(): Unit =
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
                    execution.runExclusive(
                        "A",
                    )

                withTimeout(
                    2_000L,
                ) {
                    while (
                        repository.independentStarted
                            .isEmpty()
                    ) {
                        kotlinx.coroutines.yield()
                    }
                }

                val duplicateAccepted =
                    runCatching {
                        execution.runExclusive(
                            "B",
                        )
                    }.isSuccess

                assertFalse(
                    duplicateAccepted,
                )

                repository.independentRelease.complete(
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
    fun launchEarliestReturnsTheExistingActiveRelationshipInstance(): Unit =
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
                        repository.earliestStarted
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
                    repository.earliestStarted
                        .toList(),
                )

                repository.earliestRelease.complete(
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
    fun launchLatestWaitsForEveryUnfinishedPredecessorTeardown(): Unit =
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
                        !repository.latestStarted
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
                    repository.latestCleanupStarted
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
                    repository.latestStarted
                        .toList(),
                )

                repository.latestCleanupRelease
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
    fun launchQueuedPreservesEveryLaunchInArrivalOrder(): Unit =
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

                assertEquals(
                    listOf(
                        "A",
                        "B",
                    ),
                    repository.queuedStarted
                        .toList(),
                )

                repository.queuedReleaseB.complete(
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
    fun rootShutdownClosesAdmissionBeforeCancellation(): Unit =
        runBlocking {
            val root = VexExecutionRoot()
            val repository = TestRepository()
            val execution = TestExecution(root, repository)
            val values = CopyOnWriteArrayList<Int>()

            val running =
                execution.observeStates(values)

            withTimeout(2_000L) {
                running.state.first { state ->
                    state.status ==
                        VexCoroutineStatus.RUNNING
                }
            }

            root.joinStopAll(
                "terminal test stop",
            )

            assertEquals(
                VexCoroutineStatus.CANCELLED,
                running.state.value.status,
            )

            val launchAccepted =
                runCatching {
                    execution.runOnce()
                }.isSuccess

            assertFalse(launchAccepted)
        }
}

// [VXG RealForever]
