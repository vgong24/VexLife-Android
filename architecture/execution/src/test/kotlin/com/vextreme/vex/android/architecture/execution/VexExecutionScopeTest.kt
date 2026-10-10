package com.vextreme.vex.android.architecture.execution

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VexExecutionScopeTest {
    @Test
    fun namedExecutionCanBeObservedAndCancelled() = runBlocking {
        val parent = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val scope = VexExecutionScope.childOf(
            name = VexExecutionName("test.scope"),
            parentScope = parent,
        )
        val started = CompletableDeferred<Unit>()

        try {
            val handle = scope.launch(
                executionName = VexExecutionName("server.alpha"),
            ) {
                started.complete(Unit)
                CompletableDeferred<Unit>().await()
            }

            withTimeout(2_000L) { started.await() }
            assertEquals(VexExecutionStatus.RUNNING, handle.state.value.status)
            assertNotNull(scope.executions.value[VexExecutionName("server.alpha")])

            handle.cancel("test stop")
            handle.join()
            assertEquals(VexExecutionStatus.CANCELLED, handle.state.value.status)
        } finally {
            parent.cancel()
        }
    }

    @Test
    fun siblingFailureDoesNotCancelAnotherExecution() = runBlocking {
        val parent = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val scope = VexExecutionScope.childOf(
            name = VexExecutionName("test.supervision"),
            parentScope = parent,
        )
        val siblingStarted = CompletableDeferred<Unit>()

        try {
            val sibling = scope.launch(
                executionName = VexExecutionName("server.healthy"),
            ) {
                siblingStarted.complete(Unit)
                CompletableDeferred<Unit>().await()
            }

            val failed = scope.launch(
                executionName = VexExecutionName("server.failed"),
            ) {
                error("boom")
            }

            withTimeout(2_000L) {
                siblingStarted.await()
                failed.join()
            }

            assertEquals(VexExecutionStatus.FAILED, failed.state.value.status)
            assertEquals(VexExecutionStatus.RUNNING, sibling.state.value.status)

            sibling.cancel()
            sibling.join()
        } finally {
            parent.cancel()
        }
    }

    @Test
    fun explicitRestartPolicyCanRecoverAFailedExecution() = runBlocking {
        val parent = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val scope = VexExecutionScope.childOf(
            name = VexExecutionName("test.restart"),
            parentScope = parent,
        )
        val runs = AtomicInteger(0)

        try {
            val handle = scope.launch(
                executionName = VexExecutionName("server.restartable"),
                restartPolicy = VexRestartPolicy.OnFailure(maxRestarts = 2),
            ) {
                if (runs.incrementAndGet() == 1) {
                    error("first run dies")
                }
            }

            withTimeout(2_000L) { handle.join() }

            assertEquals(2, runs.get())
            assertEquals(VexExecutionStatus.COMPLETED, handle.state.value.status)
            assertEquals(2, handle.state.value.attempt)
            assertEquals(1, handle.state.value.restartCount)
        } finally {
            parent.cancel()
        }
    }
}

// [VXG RealForever]
