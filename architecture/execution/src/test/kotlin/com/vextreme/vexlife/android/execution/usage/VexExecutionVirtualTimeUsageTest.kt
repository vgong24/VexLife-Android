package com.vextreme.vexlife.android.execution.usage

import com.vextreme.vexlife.android.execution.VexCoroutineHandle
import com.vextreme.vexlife.android.execution.VexCoroutineStatus
import com.vextreme.vexlife.android.execution.VexExecution
import com.vextreme.vexlife.android.execution.VexExecutionRoot
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Consumer-side execution test blueprint.
 *
 * This test deliberately lives outside the foundation package and uses only the public VexExecution
 * surface plus kotlinx-coroutines-test. Feature tests should follow this shape when they need to
 * prove scheduling, races, cancellation, or recovery without sleeping on real wall-clock time.
 *
 * The important seam is VexExecutionRoot(dispatcher = StandardTestDispatcher(testScheduler)):
 * every managed Job then runs on the test scheduler, so restart delays and structured cleanup can be
 * advanced deterministically with runCurrent()/advanceTimeBy().
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VexExecutionVirtualTimeUsageTest {
    private class ExampleRepository {
        val restartAttempts =
            AtomicInteger(0)

        fun primaryStates(): Flow<Int> =
            flow {
                emit(1)
                awaitCancellation()
            }

        fun secondaryStates(): Flow<Int> =
            flow {
                emit(2)
                awaitCancellation()
            }

        fun restartableStates(): Flow<Int> =
            flow {
                restartAttempts.incrementAndGet()
                error("restartable collector failed")
            }

        suspend fun operationWithCleanup() {
            try {
                awaitCancellation()
            } finally {
                /*
                 * Simulate teardown that really must finish before a replacement operation starts.
                 * NonCancellable is intentional for this test fixture; production cleanup should use
                 * it only when the concrete resource contract genuinely requires bounded teardown.
                 */
                withContext(NonCancellable) {
                    delay(1_000L)
                }
            }
        }
    }

    private class ExampleFeatureExecution(
        root: VexExecutionRoot,
        private val repository: ExampleRepository,
    ) : VexExecution(root) {
        fun observePrimary(
            values: MutableList<Int>,
        ): VexCoroutineHandle =
            launchForCollect(
                repository::primaryStates,
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

        fun observeRestartable(): VexCoroutineHandle =
            launchForCollect(
                repository::restartableStates,
            ) {}

        fun runCleanupOperation(): VexCoroutineHandle =
            launchOnce(
                repository::operationWithCleanup,
            )
    }

    @Test
    fun independentFlowSourcesStartAsConcurrentSiblingJobs() =
        runTest {
            val root =
                VexExecutionRoot(
                    dispatcher =
                        StandardTestDispatcher(
                            testScheduler,
                        ),
                )
            val repository =
                ExampleRepository()
            val execution =
                ExampleFeatureExecution(
                    root = root,
                    repository = repository,
                )
            val primary =
                mutableListOf<Int>()
            val secondary =
                mutableListOf<Int>()

            val first =
                execution.observePrimary(
                    primary,
                )
            val second =
                execution.observeSecondary(
                    secondary,
                )

            /*
             * Both managed Jobs were admitted before the scheduler runs either one.
             * runCurrent() executes all work ready at this virtual instant, proving the second
             * collector is not sequenced behind the first never-ending collect.
             */
            runCurrent()

            assertEquals(
                listOf(1),
                primary,
            )
            assertEquals(
                listOf(2),
                secondary,
            )
            assertEquals(
                VexCoroutineStatus.RUNNING,
                first.state.value.status,
            )
            assertEquals(
                VexCoroutineStatus.RUNNING,
                second.state.value.status,
            )

            first.joinStop("test complete")
            second.joinStop("test complete")
            root.joinStopAll("test complete")
        }

    @Test
    fun restartDelayCanBeProvedWithoutWaitingForWallClockTime() =
        runTest {
            val root =
                VexExecutionRoot(
                    dispatcher =
                        StandardTestDispatcher(
                            testScheduler,
                        ),
                    maxCollectRestarts = 1,
                    collectRestartDelayMillis =
                        1_000L,
                )
            val repository =
                ExampleRepository()
            val execution =
                ExampleFeatureExecution(
                    root = root,
                    repository = repository,
                )

            val handle =
                execution.observeRestartable()

            runCurrent()

            assertEquals(
                1,
                repository.restartAttempts.get(),
            )
            assertEquals(
                VexCoroutineStatus.RESTARTING,
                handle.state.value.status,
            )

            advanceTimeBy(999L)
            runCurrent()

            assertEquals(
                1,
                repository.restartAttempts.get(),
            )

            advanceTimeBy(1L)
            runCurrent()

            assertEquals(
                2,
                repository.restartAttempts.get(),
            )
            assertEquals(
                VexCoroutineStatus.FAILED,
                handle.state.value.status,
            )

            root.joinStopAll("test complete")
        }

    @Test
    fun joinStopWaitsUntilStructuredCleanupActuallyFinishes() =
        runTest {
            val root =
                VexExecutionRoot(
                    dispatcher =
                        StandardTestDispatcher(
                            testScheduler,
                        ),
                )
            val execution =
                ExampleFeatureExecution(
                    root = root,
                    repository =
                        ExampleRepository(),
                )

            val handle =
                execution.runCleanupOperation()

            runCurrent()

            val stop =
                async {
                    handle.joinStop(
                        "replace operation",
                    )
                }

            runCurrent()

            assertFalse(
                stop.isCompleted,
            )

            advanceTimeBy(999L)
            runCurrent()

            assertFalse(
                stop.isCompleted,
            )

            advanceTimeBy(1L)
            runCurrent()

            assertTrue(
                stop.await(),
            )
            assertEquals(
                VexCoroutineStatus.CANCELLED,
                handle.state.value.status,
            )

            root.joinStopAll("test complete")
        }
}

// [VXG RealForever]
