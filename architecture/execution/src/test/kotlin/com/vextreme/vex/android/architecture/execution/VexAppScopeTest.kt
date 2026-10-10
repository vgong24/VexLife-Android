package com.vextreme.vex.android.architecture.execution

import com.vextreme.vex.android.architecture.observability.VexImmediateLogExecutor
import com.vextreme.vex.android.architecture.observability.VexLogEvent
import com.vextreme.vex.android.architecture.observability.VexLogFilter
import com.vextreme.vex.android.architecture.observability.VexLogSink
import com.vextreme.vex.android.architecture.observability.VexLogger
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VexAppScopeTest {
    @Test
    fun coroutineRefRejectsUnboundedIdentity() {
        assertFailsWith<IllegalArgumentException> {
            VexCoroutineRef(
                "c".repeat(257),
            )
        }
    }

    @Test
    fun coroutineLifecycleIsEmittedToStructuredLogger() = runBlocking {
        val logger =
            VexLogger(
                executor = VexImmediateLogExecutor(),
            )
        val appScope =
            VexAppScope(
                logSink = logger,
            )

        try {
            val handle =
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("logging.sample"),
                ) {}

            handle.join()

            val records =
                logger.snapshot(
                    VexLogFilter(
                        sourcePrefix = "execution.vex.app",
                        eventPrefix = "coroutine.",
                    ),
                )

            assertEquals(
                listOf(
                    "coroutine.starting",
                    "coroutine.running",
                    "coroutine.completed",
                ),
                records.map {
                    it.event.eventRef.value
                },
            )
            assertTrue(
                records.all {
                    it.event.originRef?.value == "logging.sample"
                },
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun loggingFailureCannotPreventManagedExecutionOrCancellation() = runBlocking {
        val throwingSink =
            object : VexLogSink {
                override fun log(event: VexLogEvent) {
                    error("diagnostic sink unavailable")
                }
            }
        val appScope =
            VexAppScope(
                logSink = throwingSink,
            )
        val started = CompletableDeferred<Unit>()

        try {
            val handle =
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("logging.failure.isolated"),
                ) {
                    started.complete(Unit)
                    awaitCancellation()
                }

            withTimeout(2_000L) {
                started.await()
            }

            assertTrue(
                handle.forceStop("logging failure must not block cancellation"),
            )
            handle.join()

            assertEquals(
                VexCoroutineStatus.CANCELLED,
                handle.state.value.status,
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun namedCoroutineCanBeObservedAndForceStopped() = runBlocking {
        val appScope = VexAppScope()
        val started = CompletableDeferred<Unit>()

        try {
            val handle =
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("server.alpha"),
                ) {
                    started.complete(Unit)
                    awaitCancellation()
                }

            withTimeout(2_000L) {
                started.await()
            }

            assertEquals(
                VexCoroutineStatus.RUNNING,
                handle.state.value.status,
            )
            assertNotNull(
                appScope.executions.value[
                    VexCoroutineRef("server.alpha")
                ],
            )

            assertTrue(
                handle.forceStop("test stop"),
            )
            handle.join()

            assertEquals(
                VexCoroutineStatus.CANCELLED,
                handle.state.value.status,
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun childFailureDoesNotCancelParentOrSibling() = runBlocking {
        val appScope = VexAppScope()
        val siblingStarted = CompletableDeferred<Unit>()

        try {
            val sibling =
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("server.healthy"),
                ) {
                    siblingStarted.complete(Unit)
                    awaitCancellation()
                }

            val failed =
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("server.failed"),
                ) {
                    error("boom")
                }

            withTimeout(2_000L) {
                siblingStarted.await()
                failed.join()
            }

            assertEquals(
                VexCoroutineStatus.FAILED,
                failed.state.value.status,
            )
            assertEquals(
                VexCoroutineStatus.RUNNING,
                sibling.state.value.status,
            )

            sibling.forceStop("test sibling stop")
            sibling.join()
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun managedExecutionContainsNestedChildren() = runBlocking {
        val appScope = VexAppScope()
        val childStarted = CompletableDeferred<Unit>()
        val childStopped = CompletableDeferred<Unit>()

        try {
            val handle =
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("structured.parent"),
                ) {
                    launch {
                        try {
                            childStarted.complete(Unit)
                            awaitCancellation()
                        } finally {
                            childStopped.complete(Unit)
                        }
                    }
                }

            withTimeout(2_000L) {
                childStarted.await()
            }

            handle.forceStop("stop whole managed tree")

            withTimeout(2_000L) {
                handle.join()
                childStopped.await()
            }

            assertEquals(
                VexCoroutineStatus.CANCELLED,
                handle.state.value.status,
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun restartPolicyRejectsAttemptCounterOverflow() {
        assertFailsWith<IllegalArgumentException> {
            VexCoroutineRestartPolicy.OnFailure(
                maxRestarts = Int.MAX_VALUE,
            )
        }
    }

    @Test
    fun retainedFailureMessageIsBounded() = runBlocking {
        val appScope = VexAppScope()

        try {
            val handle =
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("failure.message.bound"),
                ) {
                    error("x".repeat(10_000))
                }

            handle.join()

            assertEquals(
                VexCoroutineStatus.FAILED,
                handle.state.value.status,
            )
            assertEquals(
                2_048,
                handle.state.value.failureMessage?.length,
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun explicitRestartPolicyCanRecoverAFailedWatcher() = runBlocking {
        val appScope = VexAppScope()
        val runs = AtomicInteger(0)

        try {
            val handle =
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("server.restartable"),
                    restartPolicy =
                        VexCoroutineRestartPolicy.OnFailure(
                            maxRestarts = 2,
                        ),
                ) {
                    if (runs.incrementAndGet() == 1) {
                        error("first run dies")
                    }
                }

            withTimeout(2_000L) {
                handle.join()
            }

            assertEquals(2, runs.get())
            assertEquals(
                VexCoroutineStatus.COMPLETED,
                handle.state.value.status,
            )
            assertEquals(2, handle.state.value.attempt)
            assertEquals(1, handle.state.value.restartCount)
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun jvmFatalExecutionClassificationIsNarrow() {
        assertTrue(
            OutOfMemoryError("oom").isJvmFatalExecutionFailure(),
        )
        assertTrue(
            LinkageError("linkage").isJvmFatalExecutionFailure(),
        )
        assertFalse(
            AssertionError("assertion").isJvmFatalExecutionFailure(),
        )
        assertFalse(
            IllegalStateException("ordinary").isJvmFatalExecutionFailure(),
        )
    }

    @Test
    fun fatalErrorIsTerminalAndNeverRestarted() = runBlocking {
        val appScope = VexAppScope()
        val runs = AtomicInteger(0)

        try {
            val handle =
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("server.fatal"),
                    restartPolicy =
                        VexCoroutineRestartPolicy.OnFailure(
                            maxRestarts = 3,
                        ),
                ) {
                    runs.incrementAndGet()
                    throw AssertionError("fatal test error")
                }

            handle.join()

            assertEquals(
                1,
                runs.get(),
            )
            assertEquals(
                VexCoroutineStatus.FAILED,
                handle.state.value.status,
            )
            assertEquals(
                "AssertionError",
                handle.state.value.failureClass,
            )
            assertEquals(
                0,
                handle.state.value.restartCount,
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun forceStopDuringRestartDelayBecomesCancelled() = runBlocking {
        val appScope = VexAppScope()

        try {
            val handle =
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("server.restart-delay"),
                    restartPolicy =
                        VexCoroutineRestartPolicy.OnFailure(
                            maxRestarts = 5,
                            delayMillis = 60_000L,
                        ),
                ) {
                    error("fail into restart delay")
                }

            withTimeout(2_000L) {
                handle.state.first { state ->
                    state.status == VexCoroutineStatus.RESTARTING
                }
            }

            handle.forceStop("cancel during restart delay")
            handle.join()

            assertEquals(
                VexCoroutineStatus.CANCELLED,
                handle.state.value.status,
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun forceStopSuppressesRestartEvenWhenRestartPolicyIsEnabled() = runBlocking {
        val appScope = VexAppScope()
        val runs = AtomicInteger(0)
        val started = CompletableDeferred<Unit>()

        try {
            val handle =
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("server.stop-does-not-restart"),
                    restartPolicy =
                        VexCoroutineRestartPolicy.OnFailure(
                            maxRestarts = 10,
                            delayMillis = 1L,
                        ),
                ) {
                    runs.incrementAndGet()
                    started.complete(Unit)
                    while (true) {
                        delay(10L)
                    }
                }

            withTimeout(2_000L) {
                started.await()
            }

            handle.forceStop("intentional stop")
            handle.join()

            assertEquals(1, runs.get())
            assertEquals(
                VexCoroutineStatus.CANCELLED,
                handle.state.value.status,
            )
            assertEquals(0, handle.state.value.restartCount)
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun duplicateActiveCoroutineRefFailsClosed() = runBlocking {
        val appScope = VexAppScope()
        val started = CompletableDeferred<Unit>()

        try {
            val first =
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("listener.same"),
                ) {
                    started.complete(Unit)
                    awaitCancellation()
                }

            withTimeout(2_000L) {
                started.await()
            }

            val duplicateAccepted =
                runCatching {
                    appScope.launchCoroutine(
                        coroutineRef = VexCoroutineRef("listener.same"),
                    ) {}
                }.isSuccess

            assertFalse(duplicateAccepted)

            first.forceStop()
            first.join()
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun terminalCompletionReleasesRefBeforeSequentialReuse() = runBlocking {
        val appScope = VexAppScope()
        val ref =
            VexCoroutineRef("listener.reusable")

        try {
            repeat(25) {
                val handle =
                    appScope.launchCoroutine(
                        coroutineRef = ref,
                    ) {}

                handle.join()

                assertEquals(
                    VexCoroutineStatus.COMPLETED,
                    handle.state.value.status,
                )
            }

            assertEquals(
                VexCoroutineStatus.COMPLETED,
                appScope.executions.value[ref]?.status,
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun retainedExecutionRegistryIsBoundedButStableRefsRemainReusable() = runBlocking {
        val appScope =
            VexAppScope(
                maxRetainedExecutionRefs = 2,
            )
        val firstRef =
            VexCoroutineRef("registry.bound.first")
        val secondRef =
            VexCoroutineRef("registry.bound.second")

        try {
            appScope.launchCoroutine(firstRef) {}.join()
            appScope.launchCoroutine(secondRef) {}.join()

            assertFailsWith<IllegalStateException> {
                appScope.launchCoroutine(
                    VexCoroutineRef("registry.bound.third"),
                ) {}
            }

            val reused =
                appScope.launchCoroutine(firstRef) {}
            reused.join()

            assertEquals(
                VexCoroutineStatus.COMPLETED,
                reused.state.value.status,
            )
            assertEquals(
                setOf(firstRef, secondRef),
                appScope.executions.value.keys,
            )
        } finally {
            appScope.stopApplication("test complete")
        }
    }

    @Test
    fun callerCancellationCannotStrandTerminalApplicationShutdown() = runBlocking {
        val appScope = VexAppScope()
        val cleanupStarted =
            CompletableDeferred<Unit>()
        val allowCleanup =
            CompletableDeferred<Unit>()

        appScope.launchCoroutine(
            coroutineRef = VexCoroutineRef("shutdown.slow-cleanup"),
        ) {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    cleanupStarted.complete(Unit)
                    allowCleanup.await()
                }
            }
        }

        val shutdown =
            launch {
                appScope.stopApplication("terminal shutdown test")
            }

        withTimeout(2_000L) {
            cleanupStarted.await()
        }

        shutdown.cancel()
        assertFalse(
            shutdown.isCompleted,
        )

        allowCleanup.complete(Unit)

        withTimeout(2_000L) {
            shutdown.join()
        }

        assertFailsWith<IllegalStateException> {
            appScope.launchCoroutine(
                VexCoroutineRef("shutdown.after"),
            ) {}
        }
    }

    @Test
    fun launchAfterApplicationStopFailsClosed() = runBlocking {
        val appScope = VexAppScope()

        appScope.stopApplication("terminal test stop")

        val launchAccepted =
            runCatching {
                appScope.launchCoroutine(
                    coroutineRef = VexCoroutineRef("after.stop"),
                ) {}
            }.isSuccess

        assertFalse(launchAccepted)
    }

    @Test
    fun executionRegistryExposureCannotMutateRetainedRegistry() = runBlocking {
        val appScope = VexAppScope()
        val started = CompletableDeferred<Unit>()
        val ref = VexCoroutineRef("registry.safe")

        try {
            val handle =
                appScope.launchCoroutine(
                    coroutineRef = ref,
                ) {
                    started.complete(Unit)
                    awaitCancellation()
                }

            withTimeout(2_000L) {
                started.await()
            }

            @Suppress("UNCHECKED_CAST")
            val exposed =
                appScope.executions.value as MutableMap<VexCoroutineRef, VexCoroutineState>

            exposed.clear()

            assertNotNull(
                appScope.executions.value[ref],
            )

            handle.forceStop()
            handle.join()
        } finally {
            appScope.stopApplication("test complete")
        }
    }
}

// [VXG RealForever]
