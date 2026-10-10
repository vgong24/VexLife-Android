package com.vextreme.vex.android.architecture.execution

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VexAppScopeTest {
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
                    CompletableDeferred<Unit>().await()
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
                    CompletableDeferred<Unit>().await()
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
                    CompletableDeferred<Unit>().await()
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
}

// [VXG RealForever]
